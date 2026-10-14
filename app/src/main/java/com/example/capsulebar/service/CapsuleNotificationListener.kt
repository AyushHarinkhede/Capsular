package com.example.capsulebar.service

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.example.capsulebar.data.CapsuleEvent
import com.example.capsulebar.data.CapsuleSettings
import com.example.capsulebar.data.CapsuleStateManager

class CapsuleNotificationListener : NotificationListenerService() {

    private lateinit var settings: CapsuleSettings
    private val activeMediaControllers = mutableMapOf<String, MediaController>()
    private val controllerCallbacks = mutableMapOf<String, MediaController.Callback>()
    private val likedOverrides = mutableMapOf<String, Boolean>()
    private val dislikedOverrides = mutableMapOf<String, Boolean>()
    private val shuffleOverrides = mutableMapOf<String, Boolean>()
    private val repeatOverrides = mutableMapOf<String, Int>()
    private val speedOverrides = mutableMapOf<String, Float>()
    private val positionOverrides = mutableMapOf<String, Pair<Long, Long>>()
    private lateinit var mediaSessionManager: MediaSessionManager
    private lateinit var listenerComponent: ComponentName

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = CapsuleSettings(this)
        CapsuleStateManager.onMediaActionListener = { action ->
            handleMediaAction(action)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        listenerComponent = ComponentName(this, CapsuleNotificationListener::class.java)

        // Query active media sessions on startup
        try {
            val controllers = mediaSessionManager.getActiveSessions(listenerComponent)
            updateControllersList(controllers)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Listen for updates to active media sessions
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(
                { controllers ->
                    updateControllersList(controllers)
                },
                listenerComponent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sync any already-active notifications (including running System Clock Timers & Stopwatches)
        try {
            activeNotifications?.forEach { sbn ->
                onNotificationPosted(sbn)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        
        // 1. Ignore notifications from our own app to prevent feedback loops
        if (packageName == this.packageName) return

        val notification = sbn.notification
        val extras = notification.extras
        val titleText = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val textText = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val combinedText = "${titleText.lowercase()} ${textText.lowercase()}"

        // 2. Intercept system/app Calls (incoming or ongoing)
        val template = extras?.getString(Notification.EXTRA_TEMPLATE) ?: ""
        val isCallNotification = notification.category == Notification.CATEGORY_CALL ||
                template.contains("CallStyle") ||
                packageName.contains("dialer") ||
                packageName.contains("telecom") ||
                packageName.contains("incall") ||
                packageName.contains("phone") && (combinedText.contains("incoming") || combinedText.contains("call") || combinedText.contains("ringing"))

        if (isCallNotification) {
            val title = if (titleText.isNotEmpty()) titleText else "Active Call"
            var text = if (textText.isNotEmpty()) textText else "Call in progress"
            
            // Check if it's an incoming call (by looking for Answer/Accept action button)
            var isIncomingCall = false
            notification.actions?.forEach { action ->
                val titleStr = action.title.toString().lowercase()
                if (titleStr.contains("answer") || titleStr.contains("accept") || titleStr.contains("receive")) {
                    isIncomingCall = true
                }
            }

            // If ongoing, check if chronometer duration is available
            if (!isIncomingCall && extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) == true) {
                val base = extras.getLong("android.chronometerBase", notification.`when`)
                if (base > 0L) {
                    val elapsed = ((android.os.SystemClock.elapsedRealtime() - base) / 1000).coerceAtLeast(0L)
                    val m = elapsed / 60
                    val s = elapsed % 60
                    text = "%02d:%02d".format(m, s)
                }
            }

            val eventId = "call_${packageName}"
            activeNotificationsMap[eventId] = sbn
            CapsuleStateManager.postEvent(
                CapsuleEvent.Call(
                    id = eventId,
                    contactName = title,
                    durationText = if (text.isNotEmpty()) text else (if (isIncomingCall) "Incoming Call" else "00:00"),
                    isIncoming = isIncomingCall,
                    priority = 1000, // HIGHEST — always takes precedence
                    durationMs = 0   // Persistent until notification cleared
                )
            )
            return // Skip generic notification display
        }

        // 2b. Intercept system/app Alarms (firing or snooze/dismiss active)
        val isAlarm = notification.category == Notification.CATEGORY_ALARM ||
                packageName.contains("clock") && (combinedText.contains("alarm") || combinedText.contains("snooze") || combinedText.contains("dismiss"))
        if (isAlarm) {
            var isFiring = false
            notification.actions?.forEach { action ->
                val actionTitle = action.title.toString().lowercase()
                if (actionTitle.contains("snooze") || actionTitle.contains("dismiss")) {
                    isFiring = true
                }
            }
            
            if (isFiring) {
                val eventId = "alarm_${packageName}"
                activeNotificationsMap[eventId] = sbn
                CapsuleStateManager.postEvent(
                    CapsuleEvent.Alarm(
                        id = eventId,
                        timeText = if (titleText.isNotEmpty()) titleText else "Alarm",
                        label = if (textText.isNotEmpty()) textText else "Wake up!",
                        isFiring = true
                    )
                )
                return // Skip generic notification display
            }
        }

        // 4. Detect Screen Recording notification (system media projection service)
        val isOngoingService = (notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0 ||
                               (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0

        // Screen recording — detected via system packages or title keywords
        val isScreenRecordPkg = packageName.contains("screenrecord") ||
                                packageName == "com.android.systemui" && combinedText.contains("screen record")
        val isScreenRecordTitle = combinedText.contains("screen record")
        if (isOngoingService && (isScreenRecordPkg || isScreenRecordTitle)) {
            val eventId = "recording_screen"
            activeNotificationsMap[eventId] = sbn
            CapsuleStateManager.postEvent(
                CapsuleEvent.Recording(
                    id = eventId,
                    type = "screen",
                    durationText = "Recording"
                )
            )
            return
        }

        // Voice / microphone recording — recorder apps posting ongoing notifications
        val isRecorderPkg = packageName.contains("recorder") || packageName.contains("voicerecord") ||
                            packageName.contains("soundrecord") || packageName.contains("audiocapture")
        val isRecordTitle = combinedText.contains("recording") && (combinedText.contains("voice") ||
                            combinedText.contains("audio") || combinedText.contains("sound") ||
                            combinedText.contains("mic"))
        if (isOngoingService && (isRecorderPkg || isRecordTitle)) {
            val eventId = "recording_voice"
            activeNotificationsMap[eventId] = sbn
            CapsuleStateManager.postEvent(
                CapsuleEvent.Recording(
                    id = eventId,
                    type = "voice",
                    durationText = "Recording"
                )
            )
            return
        }

        // 5. Detect & Sync Timer / Stopwatch from Google Clock, Samsung Clock, OnePlus/ColorOS, MIUI, AOSP, and all OEM clock apps
        val isClockApp = isSystemClockPackage(packageName)

        val clockData = extractClockNotificationData(notification, extras)
        val combined = clockData.allTextLower
        val actionTitlesLower = notification.actions?.mapNotNull { it.title?.toString()?.lowercase()?.trim() } ?: emptyList()
        val actionTitlesDisplay = notification.actions?.mapNotNull { it.title?.toString()?.trim()?.takeIf { t -> t.isNotEmpty() } } ?: emptyList()

        val hasLapAction = actionTitlesLower.any { it.contains("lap") || it.contains("split") }
        val hasAddMinuteAction = actionTitlesLower.any { it.contains("+1") || it.contains("add 1") || it.contains("1 min") || it.contains("+ 1") }

        val isStopwatchCategory = notification.category == Notification.CATEGORY_STOPWATCH ||
                hasLapAction ||
                (clockData.hasChronometer && !clockData.isCountDown && (isClockApp || combined.contains("stopwatch") || combined.contains("lap"))) ||
                (isClockApp && (combined.contains("stopwatch") || combined.contains("lap")))

        val isTimerCategory = (clockData.hasChronometer && clockData.isCountDown) ||
                hasAddMinuteAction ||
                (isClockApp && (combined.contains("timer") || combined.contains("countdown") || combined.contains("time's up") || combined.contains("times up")))

        if (isStopwatchCategory) {
            var isRunning = true
            actionTitlesLower.forEach { at ->
                if (at.contains("start") || at.contains("resume") || at.contains("continue")) isRunning = false
                if (at.contains("pause") || at.contains("lap") || at.contains("stop")) isRunning = true
            }
            if (combined.contains("paused")) {
                isRunning = false
            }

            val elapsedSecs: Long = resolveStopwatchElapsedSeconds(clockData, notification.`when`, isRunning)
            val parsedLaps = clockData.textLines.filter { line ->
                val l = line.lowercase()
                l.startsWith("lap") || l.startsWith("#")
            }.take(5)

            activeNotificationsMap["stopwatch"] = sbn
            activeNotificationsMap["stopwatch_${sbn.id}"] = sbn
            CapsuleStateManager.postEvent(
                CapsuleEvent.Stopwatch(
                    id = "stopwatch",
                    elapsedSeconds = elapsedSecs,
                    isRunning = isRunning,
                    label = resolveAppName(packageName).ifBlank { "Stopwatch" },
                    laps = parsedLaps,
                    packageName = packageName,
                    appName = resolveAppName(packageName).ifBlank { "Clock" },
                    isSystemSynced = true,
                    systemActions = actionTitlesDisplay
                )
            )
            return
        }

        if (isTimerCategory || (isClockApp && clockData.parsedSecondsFromText != null)) {
            var isRunning = true
            var isFiring = combined.contains("time's up") ||
                    combined.contains("times up") ||
                    combined.contains("expired") ||
                    combined.contains("timer done") ||
                    clockData.hasNegativeTime

            actionTitlesLower.forEach { at ->
                if (at.contains("start") || at.contains("resume") || at.contains("continue")) isRunning = false
                if (at.contains("pause")) isRunning = true
            }
            if (combined.contains("paused")) {
                isRunning = false
            }

            val remainingSecs: Long = if (isFiring) {
                0L
            } else {
                resolveTimerRemainingSeconds(clockData, notification.`when`, isRunning)
            }

            if (remainingSecs == 0L && hasAddMinuteAction && !actionTitlesLower.any { it.contains("pause") || it.contains("resume") || it.contains("start") }) {
                isFiring = true
                isRunning = false
            }

            val cleanLabel = clockData.title.takeIf {
                it.isNotBlank() && !it.matches(Regex("^-?\\d+:\\d+(?::\\d+)?$"))
            } ?: "Timer"

            activeNotificationsMap["timer"] = sbn
            activeNotificationsMap["timer_${sbn.id}"] = sbn
            CapsuleStateManager.postEvent(
                CapsuleEvent.Timer(
                    id = "timer",
                    label = cleanLabel,
                    remainingSeconds = remainingSecs,
                    isRunning = isRunning && !isFiring,
                    totalSeconds = remainingSecs.coerceAtLeast(1L),
                    isFiring = isFiring,
                    packageName = packageName,
                    appName = resolveAppName(packageName).ifBlank { "Clock" },
                    isSystemSynced = true,
                    systemActions = actionTitlesDisplay
                )
            )
            return
        }

        // 3. Process normal notifications, progress tracking, and navigation
        val token = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(Notification.EXTRA_MEDIA_SESSION)
        }
        val isProgress = settings.isProgressEnabled && extras.containsKey(Notification.EXTRA_PROGRESS) && extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0
        val isMedia = token != null && settings.isMusicEnabled
        val isMaps = (packageName == "com.google.android.apps.maps" || packageName.contains("waze") || notification.category == Notification.CATEGORY_NAVIGATION) && settings.isNavigationEnabled
        val isDeliveryPkg = packageName.contains("swiggy") || packageName.contains("zomato") ||
                            packageName.contains("ubercab") || packageName.contains("olacabs") ||
                            packageName.contains("rapido") || packageName.contains("blinkit") ||
                            packageName.contains("zepto") || packageName.contains("doordash") ||
                            packageName.contains("ubereats")

        if (isMedia) {
            activeNotificationsMap["music"] = sbn
            activeNotificationsMap["music_$packageName"] = sbn
            setupMediaController(packageName, token!!)
        } else if (isMaps) {
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            if (title.isNotEmpty() || text.isNotEmpty()) {
                val eventId = "navigation"
                activeNotificationsMap[eventId] = sbn
                CapsuleStateManager.postEvent(
                    CapsuleEvent.Navigation(
                        instruction = title.ifEmpty { text },
                        distance = if (title.isNotEmpty() && text.isNotEmpty()) text else "Active Route"
                    )
                )
            }
        } else if (isDeliveryPkg && (titleText.isNotEmpty() || textText.isNotEmpty())) {
            val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 100)
            val current = extras.getInt(Notification.EXTRA_PROGRESS, 0)
            val progressVal = if (max > 0 && current > 0) {
                (current.toFloat() / max).coerceIn(0.1f, 1.0f)
            } else {
                when {
                    combinedText.contains("arriving") || combinedText.contains("nearby") || combinedText.contains("doorstep") -> 0.88f
                    combinedText.contains("out for delivery") || combinedText.contains("on the way") || combinedText.contains("picked up") -> 0.65f
                    combinedText.contains("preparing") || combinedText.contains("assign") || combinedText.contains("confirm") -> 0.35f
                    else -> 0.50f
                }
            }
            var appLabel = "Delivery"
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                appLabel = packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {}
            val eventId = "delivery_${packageName}"
            activeNotificationsMap[eventId] = sbn
            CapsuleStateManager.postEvent(
                CapsuleEvent.Delivery(
                    id = eventId,
                    appName = appLabel,
                    statusText = titleText.ifEmpty { textText },
                    progress = progressVal
                )
            )
        } else if (isProgress) {
            val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
            val current = extras.getInt(Notification.EXTRA_PROGRESS, 0)
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Downloading"
            if (max > 0) {
                val eventId = "progress_${packageName}"
                activeNotificationsMap[eventId] = sbn
                CapsuleStateManager.postEvent(
                    CapsuleEvent.Progress(
                        id = eventId,
                        title = title,
                        progress = current,
                        max = max
                    )
                )
            }
        } else if (settings.isNotificationsEnabled) {
            val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 || 
                            (notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0
            val isMuted = settings.mutedPackages.contains(packageName)
            if (!isOngoing && !isMuted) {
                val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
                val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
                val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
                val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
                
                val title = if (rawTitle.isNotEmpty()) rawTitle else subText
                val text = if (bigText.isNotEmpty()) bigText else rawText

                if (title.isNotEmpty() || text.isNotEmpty()) {
                    var appLabel = packageName
                    var appIconBitmap: Bitmap? = null
                    var largeIconBitmap: Bitmap? = notification.largeIcon
                    try {
                        val pm = packageManager
                        val appInfo = pm.getApplicationInfo(packageName, 0)
                        appLabel = pm.getApplicationLabel(appInfo).toString()
                        val drawable = pm.getApplicationIcon(appInfo)
                        if (drawable != null) {
                            appIconBitmap = drawableToBitmap(drawable)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Notification count option: 0 = Show latest notification only
                    if (settings.notificationCountOption == 0) {
                        val keysToRemove = activeNotificationsMap.keys.filter { it.startsWith("notification_") }
                        keysToRemove.forEach { key ->
                            activeNotificationsMap.remove(key)
                            CapsuleStateManager.removeEvent(key)
                        }
                    }

                    val eventId = "notification_${packageName}_${sbn.id}"
                    activeNotificationsMap[eventId] = sbn
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && sbn.key.isNotEmpty()) {
                        activeNotificationsMap[sbn.key] = sbn
                    }

                    // Trigger sound / haptic on new notification appear
                    try {
                        HapticSoundManager.playCapsuleAppear()
                    } catch (_: Exception) {}

                    CapsuleStateManager.postEvent(
                        CapsuleEvent.Notification(
                            id = eventId,
                            packageName = packageName,
                            appName = appLabel,
                            title = title,
                            text = text,
                            appIcon = appIconBitmap ?: largeIconBitmap,
                            largeImage = largeIconBitmap,
                            smallIcon = appIconBitmap
                        )
                    )

                    // Auto expand notifications
                    if (settings.autoExpand) {
                        CapsuleStateManager.setDisplayMode(com.example.capsulebar.data.DisplayMode.EXPANDED)
                    }
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notification = sbn.notification
        
        val template = notification.extras?.getString(Notification.EXTRA_TEMPLATE) ?: ""
        val isCallNotification = notification.category == Notification.CATEGORY_CALL ||
                template.contains("CallStyle")

        if (isCallNotification) {
            CapsuleStateManager.removeEvent("call_${packageName}")
            CapsuleStateManager.removeEvent("call") // Fallback
        }

        if (packageName.contains("clock") || notification.category == Notification.CATEGORY_ALARM) {
            CapsuleStateManager.removeEvent("alarm_${packageName}")
            CapsuleStateManager.removeEvent("alarm") // Fallback
        }

        if (activeMediaControllers.containsKey(packageName)) {
            val controller = activeMediaControllers.remove(packageName)
            val callback = controllerCallbacks.remove(packageName)
            if (controller != null && callback != null) {
                try {
                    controller.unregisterCallback(callback)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            CapsuleStateManager.removeEvent("music")
        }

        if (packageName == "com.google.android.apps.maps" || packageName.contains("waze")) {
            CapsuleStateManager.removeEvent("navigation")
        }

        // Clean up Clock app events (Timer / Stopwatch)
        if (isSystemClockPackage(packageName)) {
            if (activeNotificationsMap["timer"]?.id == sbn.id || activeNotificationsMap.containsKey("timer_${sbn.id}")) {
                CapsuleStateManager.removeEvent("timer")
                CapsuleStateManager.removeEvent("timer_${sbn.id}")
                activeNotificationsMap.remove("timer")
            }
            if (activeNotificationsMap["stopwatch"]?.id == sbn.id || activeNotificationsMap.containsKey("stopwatch_${sbn.id}")) {
                CapsuleStateManager.removeEvent("stopwatch")
                CapsuleStateManager.removeEvent("stopwatch_${sbn.id}")
                activeNotificationsMap.remove("stopwatch")
            }
        }

        // Clean up Recording events
        val isRecorderPkg = packageName.contains("recorder") || packageName.contains("voicerecord") ||
                            packageName.contains("soundrecord") || packageName.contains("audiocapture") ||
                            packageName.contains("screenrecord")
        if (isRecorderPkg) {
            CapsuleStateManager.removeEvent("recording_screen")
            CapsuleStateManager.removeEvent("recording_voice")
        }
        if (packageName == "com.android.systemui") {
            // Screen recorder from system UI dismissed
            CapsuleStateManager.removeEvent("recording_screen")
        }

        CapsuleStateManager.removeEvent("delivery_${packageName}")
        CapsuleStateManager.removeEvent("progress_${packageName}")
        CapsuleStateManager.removeEvent("notification_${packageName}_${sbn.id}")

        // Clean from active map
        activeNotificationsMap.remove("call_${packageName}")
        activeNotificationsMap.remove("alarm_${packageName}")
        activeNotificationsMap.remove("recording_screen")
        activeNotificationsMap.remove("recording_voice")
        activeNotificationsMap.remove("timer_${sbn.id}")
        activeNotificationsMap.remove("stopwatch_${sbn.id}")
        activeNotificationsMap.remove("navigation")
        activeNotificationsMap.remove("delivery_${packageName}")
        activeNotificationsMap.remove("progress_${packageName}")
        activeNotificationsMap.remove("notification_${packageName}_${sbn.id}")
    }

    private data class ClockNotificationData(
        val title: String,
        val textLines: List<String>,
        val allTextLower: String,
        val hasChronometer: Boolean,
        val isCountDown: Boolean,
        val remoteViewsChronometerBaseElapsedMs: Long,
        val extrasChronometerBaseMs: Long,
        val parsedSecondsFromText: Long?,
        val hasNegativeTime: Boolean
    )

    private fun isSystemClockPackage(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return pkg == "com.google.android.deskclock" ||
               pkg == "com.android.deskclock" ||
               pkg == "com.sec.android.app.clockpackage" ||
               pkg == "com.oneplus.deskclock" ||
               pkg == "com.coloros.alarmclock" ||
               pkg == "com.oplus.alarmclock" ||
               pkg == "com.motorola.blur.alarmclock" ||
               pkg == "com.miui.deskclock" ||
               pkg == "com.vivo.dreamclock" ||
               pkg == "com.bbk.alarmclock" ||
               pkg == "com.asus.deskclock" ||
               pkg.contains("deskclock") ||
               pkg.contains("alarmclock") ||
               pkg.contains("clockpackage") ||
               pkg.contains("clock") ||
               pkg.contains("timer") ||
               pkg.contains("stopwatch")
    }

    private fun extractClockNotificationData(notification: Notification, extras: Bundle?): ClockNotificationData {
        val lines = mutableListOf<String>()
        val rawTitle = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val rawText = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val infoText = extras?.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()?.trim().orEmpty()
        val summaryText = extras?.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.trim().orEmpty()
        val ticker = notification.tickerText?.toString()?.trim().orEmpty()

        listOf(rawTitle, rawText, subText, bigText, infoText, summaryText, ticker).forEach { s ->
            if (s.isNotEmpty() && !lines.contains(s)) lines.add(s)
        }

        var rvChronoBase = 0L
        var rvCountDown: Boolean? = null

        // Inspect RemoteViews (contentView & bigContentView) for custom Google/Samsung/OEM Clock notifications
        @Suppress("DEPRECATION")
        val remoteViewsList = listOfNotNull(notification.contentView, notification.bigContentView, notification.headsUpContentView)
        for (rv in remoteViewsList) {
            try {
                var clazz: Class<*>? = rv.javaClass
                var actionsField: java.lang.reflect.Field? = null
                while (clazz != null && actionsField == null) {
                    actionsField = try { clazz.getDeclaredField("mActions") } catch (_: Exception) { null }
                    clazz = clazz.superclass
                }
                actionsField?.isAccessible = true
                val actions = actionsField?.get(rv) as? Iterable<*>
                actions?.forEach { actionObj ->
                    if (actionObj != null) {
                        val aClass = actionObj.javaClass
                        val methodName = try {
                            val mf = aClass.getDeclaredField("methodName").apply { isAccessible = true }
                            mf.get(actionObj) as? String
                        } catch (_: Exception) { null }

                        val valueObj = try {
                            val vf = aClass.getDeclaredField("value").apply { isAccessible = true }
                            vf.get(actionObj)
                        } catch (_: Exception) { null }

                        when (methodName) {
                            "setText" -> {
                                val str = valueObj?.toString()?.trim().orEmpty()
                                if (str.isNotEmpty() && !lines.contains(str)) {
                                    lines.add(str)
                                }
                            }
                            "setBase" -> {
                                val baseVal = (valueObj as? Long) ?: 0L
                                if (baseVal > 0L) rvChronoBase = baseVal
                            }
                            "setCountDown" -> {
                                val cd = valueObj as? Boolean
                                if (cd != null) rvCountDown = cd
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        val hasChronometer = (extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) == true) || rvChronoBase > 0L
        val isCountDown = rvCountDown ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            extras?.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN, false) == true
        } else false
        val extrasChronoBase = extras?.getLong("android.chronometerBase", 0L) ?: 0L

        var parsedSecs: Long? = null
        var hasNegative = false
        for (line in lines) {
            if (Regex("-\\d+:\\d+").containsMatchIn(line)) {
                hasNegative = true
            }
            if (parsedSecs == null) {
                parsedSecs = parseDurationSecondsFromText(line)
            }
        }

        val resolvedTitle = rawTitle.ifEmpty {
            lines.firstOrNull { !it.matches(Regex("^-?\\d+:\\d+(?::\\d+)?(?:\\.\\d+)?$")) }.orEmpty()
        }

        return ClockNotificationData(
            title = resolvedTitle,
            textLines = lines,
            allTextLower = lines.joinToString(" ").lowercase(),
            hasChronometer = hasChronometer,
            isCountDown = isCountDown,
            remoteViewsChronometerBaseElapsedMs = rvChronoBase,
            extrasChronometerBaseMs = extrasChronoBase,
            parsedSecondsFromText = parsedSecs,
            hasNegativeTime = hasNegative
        )
    }

    private fun parseDurationSecondsFromText(text: String): Long? {
        // 1. Check HH:MM:SS or MM:SS (with optional centiseconds .cc)
        val colonMatch = Regex("(?:^|\\s)(-?)(\\d{1,2}):(\\d{2})(?::(\\d{2}))?(?:\\.\\d{1,2})?(?:$|\\s)").find(text)
        if (colonMatch != null) {
            val isNeg = colonMatch.groupValues[1] == "-"
            if (isNeg) return 0L
            val a = colonMatch.groupValues[2].toLongOrNull() ?: 0L
            val b = colonMatch.groupValues[3].toLongOrNull() ?: 0L
            val c = colonMatch.groupValues.getOrNull(4)?.takeIf { it.isNotEmpty() }?.toLongOrNull()
            return if (c != null) (a * 3600L + b * 60L + c) else (a * 60L + b)
        }

        // 2. Check verbal format like "4m 30s", "1h 15m", "45s", "12 min 10 sec"
        val hMatch = Regex("(\\d+)\\s*(?:h|hr|hrs|hour|hours)\\b", RegexOption.IGNORE_CASE).find(text)
        val mMatch = Regex("(\\d+)\\s*(?:m|min|mins|minute|minutes)\\b", RegexOption.IGNORE_CASE).find(text)
        val sMatch = Regex("(\\d+)\\s*(?:s|sec|secs|second|seconds)\\b", RegexOption.IGNORE_CASE).find(text)
        if (hMatch != null || mMatch != null || sMatch != null) {
            val h = hMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            val m = mMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            val s = sMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            val total = h * 3600L + m * 60L + s
            if (total > 0L) return total
        }
        return null
    }

    private fun resolveTimerRemainingSeconds(
        clockData: ClockNotificationData,
        notificationWhen: Long,
        isRunning: Boolean
    ): Long {
        val nowElapsed = android.os.SystemClock.elapsedRealtime()
        val nowEpoch = System.currentTimeMillis()

        // If paused and text explicitly contains the remaining duration, prefer text first
        if (!isRunning && clockData.parsedSecondsFromText != null) {
            return clockData.parsedSecondsFromText
        }

        // 1. RemoteViews Chronometer base (in SystemClock.elapsedRealtime() domain)
        if (clockData.remoteViewsChronometerBaseElapsedMs > 0L) {
            val diffSec = ((clockData.remoteViewsChronometerBaseElapsedMs - nowElapsed + 499L) / 1000L)
            if (diffSec in 0L..864_000L) return diffSec
        }

        // 2. Extras chronometerBase
        if (clockData.extrasChronometerBaseMs > 0L) {
            val base = clockData.extrasChronometerBaseMs
            val diffSec = if (base > 1_000_000_000_000L) {
                (base - nowEpoch + 499L) / 1000L
            } else {
                (base - nowElapsed + 499L) / 1000L
            }
            if (diffSec in 0L..864_000L) return diffSec
        }

        // 3. notification.when (epoch ms if > 1e12, elapsedRealtime if smaller)
        if (notificationWhen > 0L && (clockData.hasChronometer || notificationWhen > nowEpoch)) {
            val diffSec = if (notificationWhen > 1_000_000_000_000L) {
                (notificationWhen - nowEpoch + 499L) / 1000L
            } else {
                (notificationWhen - nowElapsed + 499L) / 1000L
            }
            if (diffSec in 0L..864_000L) return diffSec
        }

        // 4. Fallback to parsed duration text
        return clockData.parsedSecondsFromText ?: 0L
    }

    private fun resolveStopwatchElapsedSeconds(
        clockData: ClockNotificationData,
        notificationWhen: Long,
        isRunning: Boolean
    ): Long {
        val nowElapsed = android.os.SystemClock.elapsedRealtime()
        val nowEpoch = System.currentTimeMillis()

        if (!isRunning && clockData.parsedSecondsFromText != null) {
            return clockData.parsedSecondsFromText
        }

        // 1. RemoteViews Chronometer base (in SystemClock.elapsedRealtime() domain)
        if (clockData.remoteViewsChronometerBaseElapsedMs > 0L) {
            val diffSec = (nowElapsed - clockData.remoteViewsChronometerBaseElapsedMs) / 1000L
            if (diffSec in 0L..864_000L) return diffSec
        }

        // 2. Extras chronometerBase
        if (clockData.extrasChronometerBaseMs > 0L) {
            val base = clockData.extrasChronometerBaseMs
            val diffSec = if (base > 1_000_000_000_000L) {
                (nowEpoch - base) / 1000L
            } else {
                (nowElapsed - base) / 1000L
            }
            if (diffSec in 0L..864_000L) return diffSec
        }

        // 3. notification.when (only if hasChronometer is true so we don't mistake post timestamp on paused notification)
        if (notificationWhen > 0L && clockData.hasChronometer && isRunning) {
            val diffSec = if (notificationWhen > 1_000_000_000_000L) {
                (nowEpoch - notificationWhen) / 1000L
            } else {
                (nowElapsed - notificationWhen) / 1000L
            }
            if (diffSec in 0L..864_000L) return diffSec
        }

        return clockData.parsedSecondsFromText ?: 0L
    }

    private fun setupMediaController(packageName: String, token: MediaSession.Token) {
        val existing = activeMediaControllers[packageName]
        if (existing != null) {
            updateMusicState(existing, packageName)
            return
        }

        try {
            val controller = MediaController(this, token)
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    updateMusicState(controller, packageName)
                }

                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    updateMusicState(controller, packageName)
                }
            }

            controller.registerCallback(callback)
            activeMediaControllers[packageName] = controller
            controllerCallbacks[packageName] = callback

            updateMusicState(controller, packageName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateControllersList(controllers: List<MediaController>?) {
        if (controllers == null) return

        // Clean up callbacks of previous active controllers
        for ((pkg, controller) in activeMediaControllers) {
            val callback = controllerCallbacks[pkg]
            if (callback != null) {
                try {
                    controller.unregisterCallback(callback)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        activeMediaControllers.clear()
        controllerCallbacks.clear()

        // Register new callbacks
        for (controller in controllers) {
            val packageName = controller.packageName
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    updateMusicState(controller, packageName)
                }

                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    updateMusicState(controller, packageName)
                }
            }
            try {
                controller.registerCallback(callback)
                activeMediaControllers[packageName] = controller
                controllerCallbacks[packageName] = callback
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Show controller that is currently playing, or fallback to first active session
        val playingController = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers.firstOrNull()

        if (playingController != null) {
            updateMusicState(playingController, playingController.packageName)
        } else {
            CapsuleStateManager.removeEvent("music")
        }
    }

    private fun resolveAppName(packageName: String): String {
        if (packageName.isBlank()) return "Media Player"
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            when {
                packageName.contains("spotify", true) -> "Spotify"
                packageName.contains("youtube.music", true) || packageName.contains("youtubemusic", true) -> "YT Music"
                packageName.contains("youtube", true) -> "YouTube"
                packageName.contains("apple", true) -> "Apple Music"
                packageName.contains("saavn", true) -> "JioSaavn"
                packageName.contains("gaana", true) -> "Gaana"
                packageName.contains("wynk", true) -> "Wynk Music"
                packageName.contains("soundcloud", true) -> "SoundCloud"
                packageName.contains("pocketfm", true) -> "Pocket FM"
                packageName.contains("audible", true) -> "Audible"
                packageName.contains("twitch", true) -> "Twitch"
                packageName.contains("vlc", true) -> "VLC"
                else -> "Music"
            }
        }
    }

    private fun detectAudioOutputName(): String {
        return try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
            val btDevice = devices.firstOrNull {
                it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
                    it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == android.media.AudioDeviceInfo.TYPE_BLE_SPEAKER
                ))
            }
            if (btDevice != null) {
                val prod = btDevice.productName?.toString()?.takeIf { it.isNotBlank() }
                return prod ?: "Bluetooth"
            }
            val wired = devices.firstOrNull {
                it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET
            }
            if (wired != null) return "Headphones"
            "Speaker"
        } catch (_: Exception) {
            "Speaker"
        }
    }

    private fun isStandardTransportTitle(text: String): Boolean {
        val lower = text.trim().lowercase()
        return lower in setOf(
            "play", "pause", "next", "previous", "prev", "skip to next", "skip to previous",
            "stop", "skip", "back", "resume"
        )
    }

    private fun updateMusicState(controller: MediaController, packageName: String) {
        if (!settings.isMusicEnabled) return

        val metadata = controller.metadata
        val state = controller.playbackState
        val sbn = activeNotificationsMap["music_$packageName"] ?: activeNotificationsMap["music"]
        val notifExtras = sbn?.notification?.extras

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: notifExtras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: "Unknown Track"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: notifExtras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: "Unknown Artist"
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val appName = resolveAppName(packageName)

        val isPlaying = state != null && state.state == PlaybackState.STATE_PLAYING
        val isBuffering = state != null && (
            state.state == PlaybackState.STATE_BUFFERING ||
            state.state == PlaybackState.STATE_CONNECTING
        )
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val isLiveStream = duration <= 0L && (
            title.contains("live", ignoreCase = true) ||
            packageName.contains("twitch", ignoreCase = true) ||
            packageName.contains("radio", ignoreCase = true) ||
            (packageName.contains("youtube", ignoreCase = true) && duration <= 0L && isPlaying)
        )

        val recentSeek = positionOverrides[packageName]?.takeIf { System.currentTimeMillis() - it.second < 1800L }?.first
        val position = recentSeek ?: (state?.position ?: 0L).coerceAtLeast(0L)
        val playbackSpeed = speedOverrides[packageName] ?: (state?.playbackSpeed?.takeIf { it > 0f } ?: 1.0f)

        val rawAlbumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            ?: sbn?.notification?.largeIcon

        val albumArt = if (rawAlbumArt != null && !rawAlbumArt.isRecycled) {
            try {
                val maxDim = 512
                val srcW = rawAlbumArt.width
                val srcH = rawAlbumArt.height
                val safeConfigBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && rawAlbumArt.config == Bitmap.Config.HARDWARE) {
                    rawAlbumArt.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    rawAlbumArt
                }
                if (safeConfigBitmap != null && (srcW > maxDim || srcH > maxDim)) {
                    val ratio = maxDim.toFloat() / maxOf(srcW, srcH)
                    val targetW = (srcW * ratio).toInt().coerceAtLeast(1)
                    val targetH = (srcH * ratio).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(safeConfigBitmap, targetW, targetH, true)
                } else {
                    safeConfigBitmap
                }
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

        // Extract custom actions from both MediaSession PlaybackState and Media Notification
        val customActionsList = mutableListOf<CapsuleEvent.MediaCustomActionUi>()
        var actionIndicatesLiked = false
        state?.customActions?.forEach { ca ->
            val label = ca.name?.toString()?.trim().orEmpty()
            val id = ca.action.orEmpty()
            val combined = "$label $id".lowercase()
            if (combined.contains("unlike") || combined.contains("remove from liked") || combined.contains("remove_from_collection") || combined.contains("unheart")) {
                actionIndicatesLiked = true
            }
            if (label.isNotEmpty() && !isStandardTransportTitle(label)) {
                customActionsList.add(
                    CapsuleEvent.MediaCustomActionUi(
                        id = id.ifEmpty { label },
                        label = label,
                        isNotificationAction = false
                    )
                )
            }
        }

        sbn?.notification?.actions?.forEach { na ->
            val label = na.title?.toString()?.trim().orEmpty()
            val lower = label.lowercase()
            if (lower.contains("unlike") || lower.contains("remove from liked") || lower == "liked" || lower == "saved") {
                actionIndicatesLiked = true
            }
            if (label.isNotEmpty() && !isStandardTransportTitle(label) && customActionsList.none { it.label.equals(label, ignoreCase = true) }) {
                customActionsList.add(
                    CapsuleEvent.MediaCustomActionUi(
                        id = label,
                        label = label,
                        isNotificationAction = true
                    )
                )
            }
        }

        val trackKey = "$packageName:$title"
        val rating = try { metadata?.getRating(MediaMetadata.METADATA_KEY_USER_RATING) } catch (_: Exception) { null }
        val ratedLiked = rating != null && rating.isRated && (
            (try { rating.hasHeart() } catch (_: Exception) { false }) ||
            (try { rating.isThumbUp } catch (_: Exception) { false })
        )
        val isLiked = likedOverrides[trackKey] ?: (ratedLiked || actionIndicatesLiked)
        val isDisliked = dislikedOverrides[trackKey] ?: false
        val isShuffle = shuffleOverrides[packageName] ?: false
        val repeatModeVal = repeatOverrides[packageName] ?: (
            when (controller.playbackState?.actions) {
                else -> 0
            }
        )
        val isRepeat = repeatModeVal > 0

        CapsuleStateManager.postEvent(
            CapsuleEvent.Music(
                title = title,
                artist = artist,
                isPlaying = isPlaying,
                duration = duration,
                position = position,
                albumArt = albumArt,
                packageName = packageName,
                appName = appName,
                album = album,
                isLiked = isLiked,
                isDisliked = isDisliked,
                isLiveStream = isLiveStream,
                isBuffering = isBuffering,
                playbackSpeed = playbackSpeed,
                isShuffleEnabled = isShuffle,
                isRepeatEnabled = isRepeat,
                repeatMode = repeatModeVal,
                customActions = customActionsList.take(6),
                audioOutputName = detectAudioOutputName()
            )
        )
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }

        val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun dispatchSystemMediaKey(keyCode: Int) {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val now = android.os.SystemClock.uptimeMillis()
            val downEvent = android.view.KeyEvent(now, now, android.view.KeyEvent.ACTION_DOWN, keyCode, 0)
            val upEvent = android.view.KeyEvent(now, now, android.view.KeyEvent.ACTION_UP, keyCode, 0)
            audioManager.dispatchMediaKeyEvent(downEvent)
            audioManager.dispatchMediaKeyEvent(upEvent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerMatchingCustomOrNotificationAction(
        controller: MediaController?,
        packageName: String,
        keywords: List<String>
    ): Boolean {
        var triggered = false
        controller?.playbackState?.customActions?.forEach { ca ->
            val combined = "${ca.name} ${ca.action}".lowercase()
            if (!triggered && keywords.any { combined.contains(it) }) {
                try {
                    controller.transportControls.sendCustomAction(ca, ca.extras)
                    triggered = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        if (!triggered) {
            val sbn = activeNotificationsMap["music_$packageName"] ?: activeNotificationsMap["music"]
            sbn?.notification?.actions?.forEach { na ->
                val title = na.title?.toString()?.lowercase().orEmpty()
                if (!triggered && keywords.any { title.contains(it) }) {
                    try {
                        na.actionIntent?.send()
                        triggered = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return triggered
    }

    private fun handleMediaAction(action: String) {
        val currentUiMusic = (CapsuleStateManager.uiState.value.mainEvent as? CapsuleEvent.Music)
            ?: (CapsuleStateManager.uiState.value.splitEvent as? CapsuleEvent.Music)

        val controller = activeMediaControllers.values.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: currentUiMusic?.packageName?.let { activeMediaControllers[it] }
          ?: activeMediaControllers.values.firstOrNull()

        val pkg = controller?.packageName ?: currentUiMusic?.packageName.orEmpty()
        val title = controller?.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: currentUiMusic?.title ?: "Unknown Track"
        val trackKey = "$pkg:$title"

        try {
            when {
                action == "play" -> {
                    if (controller != null) {
                        controller.transportControls.play()
                    } else if (!triggerMatchingCustomOrNotificationAction(null, pkg, listOf("play", "resume"))) {
                        dispatchSystemMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
                    }
                    if (currentUiMusic != null) {
                        CapsuleStateManager.postEvent(currentUiMusic.copy(isPlaying = true))
                    }
                }
                action == "pause" -> {
                    if (controller != null) {
                        controller.transportControls.pause()
                    } else if (!triggerMatchingCustomOrNotificationAction(null, pkg, listOf("pause"))) {
                        dispatchSystemMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
                    }
                    if (currentUiMusic != null) {
                        CapsuleStateManager.postEvent(currentUiMusic.copy(isPlaying = false))
                    }
                }
                action == "next" -> {
                    if (controller != null) {
                        controller.transportControls.skipToNext()
                    } else if (!triggerMatchingCustomOrNotificationAction(null, pkg, listOf("next", "skip"))) {
                        dispatchSystemMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
                    }
                }
                action == "previous" -> {
                    if (controller != null) {
                        controller.transportControls.skipToPrevious()
                    } else if (!triggerMatchingCustomOrNotificationAction(null, pkg, listOf("prev", "previous"))) {
                        dispatchSystemMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                    }
                }
                action == "rewind" || action == "reverse" -> {
                    val curPos = currentUiMusic?.position ?: controller?.playbackState?.position ?: 0L
                    val targetMs = (curPos - 10_000L).coerceAtLeast(0L)
                    positionOverrides[pkg] = targetMs to System.currentTimeMillis()
                    if (controller != null) {
                        val duration = controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
                        if (duration > 0L) {
                            controller.transportControls.seekTo(targetMs)
                        } else {
                            controller.transportControls.rewind()
                            triggerMatchingCustomOrNotificationAction(controller, pkg, listOf("rewind", "replay", "-10", "-15", "back"))
                        }
                        updateMusicState(controller, pkg)
                    } else {
                        dispatchSystemMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_REWIND)
                        if (currentUiMusic != null) {
                            CapsuleStateManager.postEvent(currentUiMusic.copy(position = targetMs))
                        }
                    }
                }
                action == "fast_forward" || action == "forward" -> {
                    val curPos = currentUiMusic?.position ?: controller?.playbackState?.position ?: 0L
                    val maxDur = currentUiMusic?.duration ?: controller?.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
                    val targetMs = if (maxDur > 0L) (curPos + 10_000L).coerceAtMost(maxDur) else curPos + 10_000L
                    positionOverrides[pkg] = targetMs to System.currentTimeMillis()
                    if (controller != null) {
                        if (maxDur > 0L) {
                            controller.transportControls.seekTo(targetMs)
                        } else {
                            controller.transportControls.fastForward()
                            triggerMatchingCustomOrNotificationAction(controller, pkg, listOf("forward", "fast", "+10", "+15", "+30"))
                        }
                        updateMusicState(controller, pkg)
                    } else {
                        dispatchSystemMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
                        if (currentUiMusic != null) {
                            CapsuleStateManager.postEvent(currentUiMusic.copy(position = targetMs))
                        }
                    }
                }
                action.startsWith("seek:") -> {
                    val targetMs = action.removePrefix("seek:").toLongOrNull()
                    if (targetMs != null) {
                        val clamped = targetMs.coerceAtLeast(0L)
                        positionOverrides[pkg] = clamped to System.currentTimeMillis()
                        if (controller != null) {
                            controller.transportControls.seekTo(clamped)
                            updateMusicState(controller, pkg)
                        } else if (currentUiMusic != null) {
                            CapsuleStateManager.postEvent(currentUiMusic.copy(position = clamped))
                        }
                    }
                }
                action == "toggle_like" || action == "like" || action == "unlike" -> {
                    val currentlyLiked = likedOverrides[trackKey] ?: currentUiMusic?.isLiked ?: false
                    val nextLiked = when (action) {
                        "like" -> true
                        "unlike" -> false
                        else -> !currentlyLiked
                    }
                    likedOverrides[trackKey] = nextLiked
                    if (nextLiked) dislikedOverrides[trackKey] = false

                    if (controller != null) {
                        try {
                            when (controller.ratingType) {
                                android.media.Rating.RATING_HEART -> {
                                    controller.transportControls.setRating(android.media.Rating.newHeartRating(nextLiked))
                                }
                                android.media.Rating.RATING_THUMB_UP_DOWN -> {
                                    val r = if (nextLiked) android.media.Rating.newThumbRating(true)
                                            else android.media.Rating.newUnratedRating(android.media.Rating.RATING_THUMB_UP_DOWN)
                                    controller.transportControls.setRating(r)
                                }
                                else -> {
                                    controller.transportControls.setRating(android.media.Rating.newHeartRating(nextLiked))
                                }
                            }
                        } catch (_: Exception) {}
                        triggerMatchingCustomOrNotificationAction(
                            controller,
                            pkg,
                            listOf("like", "heart", "fav", "thumb_up", "thumbs_up", "thumbs up", "save", "collection", "love", "add_to")
                        )
                        updateMusicState(controller, pkg)
                    } else if (currentUiMusic != null) {
                        triggerMatchingCustomOrNotificationAction(null, pkg, listOf("like", "heart", "fav", "thumb", "save"))
                        CapsuleStateManager.postEvent(
                            currentUiMusic.copy(isLiked = nextLiked, isDisliked = if (nextLiked) false else currentUiMusic.isDisliked)
                        )
                    }
                }
                action == "toggle_dislike" || action == "dislike" -> {
                    val currentlyDisliked = dislikedOverrides[trackKey] ?: currentUiMusic?.isDisliked ?: false
                    val nextDisliked = !currentlyDisliked
                    dislikedOverrides[trackKey] = nextDisliked
                    if (nextDisliked) likedOverrides[trackKey] = false

                    if (controller != null) {
                        try {
                            if (controller.ratingType == android.media.Rating.RATING_THUMB_UP_DOWN) {
                                val r = if (nextDisliked) android.media.Rating.newThumbRating(false)
                                        else android.media.Rating.newUnratedRating(android.media.Rating.RATING_THUMB_UP_DOWN)
                                controller.transportControls.setRating(r)
                            }
                        } catch (_: Exception) {}
                        triggerMatchingCustomOrNotificationAction(
                            controller,
                            pkg,
                            listOf("dislike", "thumb_down", "thumbs_down", "thumbs down", "ban", "don't play", "hide")
                        )
                        updateMusicState(controller, pkg)
                    } else if (currentUiMusic != null) {
                        CapsuleStateManager.postEvent(
                            currentUiMusic.copy(isDisliked = nextDisliked, isLiked = if (nextDisliked) false else currentUiMusic.isLiked)
                        )
                    }
                }
                action == "toggle_shuffle" || action == "shuffle" -> {
                    val nextShuffle = !(shuffleOverrides[pkg] ?: currentUiMusic?.isShuffleEnabled ?: false)
                    shuffleOverrides[pkg] = nextShuffle
                    triggerMatchingCustomOrNotificationAction(controller, pkg, listOf("shuffle", "random"))
                    if (controller != null) {
                        updateMusicState(controller, pkg)
                    } else if (currentUiMusic != null) {
                        CapsuleStateManager.postEvent(currentUiMusic.copy(isShuffleEnabled = nextShuffle))
                    }
                }
                action == "toggle_repeat" || action == "repeat" -> {
                    val curRepeat = repeatOverrides[pkg] ?: currentUiMusic?.repeatMode ?: 0
                    // Cycle: 0 (Off) -> 1 (All) -> 2 (One) -> 0 (Off)
                    val nextRepeat = (curRepeat + 1) % 3
                    repeatOverrides[pkg] = nextRepeat

                    val targetKeywords = when (nextRepeat) {
                        2 -> listOf("repeat_one", "repeat one", "repeat_single", "repeat single", "loop_one", "repeat_1")
                        1 -> listOf("repeat_all", "repeat all", "repeat", "loop")
                        else -> listOf("repeat_off", "repeat off", "no repeat", "repeat")
                    }
                    triggerMatchingCustomOrNotificationAction(controller, pkg, targetKeywords)
                    if (controller != null) {
                        updateMusicState(controller, pkg)
                    } else if (currentUiMusic != null) {
                        CapsuleStateManager.postEvent(
                            currentUiMusic.copy(
                                repeatMode = nextRepeat,
                                isRepeatEnabled = nextRepeat > 0
                            )
                        )
                    }
                }
                action == "cycle_speed" || action.startsWith("speed:") -> {
                    val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f, 0.75f)
                    val curSpeed = speedOverrides[pkg] ?: currentUiMusic?.playbackSpeed ?: 1.0f
                    val nextSpeed = if (action.startsWith("speed:")) {
                        action.removePrefix("speed:").toFloatOrNull() ?: 1.0f
                    } else {
                        val idx = speeds.indexOfFirst { kotlin.math.abs(it - curSpeed) < 0.05f }
                        speeds[(idx + 1) % speeds.size]
                    }
                    speedOverrides[pkg] = nextSpeed
                    if (controller != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            try {
                                controller.transportControls.setPlaybackSpeed(nextSpeed)
                            } catch (_: Exception) {}
                        }
                        triggerMatchingCustomOrNotificationAction(controller, pkg, listOf("speed", "rate"))
                        updateMusicState(controller, pkg)
                    } else if (currentUiMusic != null) {
                        CapsuleStateManager.postEvent(currentUiMusic.copy(playbackSpeed = nextSpeed))
                    }
                }
                action.startsWith("custom:") -> {
                    val targetId = action.removePrefix("custom:")
                    val ca = controller?.playbackState?.customActions?.firstOrNull {
                        it.action == targetId || it.name?.toString() == targetId
                    }
                    if (ca != null && controller != null) {
                        controller.transportControls.sendCustomAction(ca, ca.extras)
                        updateMusicState(controller, pkg)
                    }
                }
                action.startsWith("notif_action:") -> {
                    val targetTitle = action.removePrefix("notif_action:")
                    val sbn = activeNotificationsMap["music_$pkg"] ?: activeNotificationsMap["music"]
                    val matched = sbn?.notification?.actions?.firstOrNull {
                        it.title?.toString()?.equals(targetTitle, ignoreCase = true) == true
                    }
                    matched?.actionIntent?.send()
                    if (controller != null) {
                        updateMusicState(controller, pkg)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        CapsuleStateManager.onMediaActionListener = null
        for ((pkg, controller) in activeMediaControllers) {
            val callback = controllerCallbacks[pkg]
            if (callback != null) {
                try {
                    controller.unregisterCallback(callback)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        activeMediaControllers.clear()
        controllerCallbacks.clear()
        instance = null
        super.onDestroy()
    }

    companion object {
        private var instance: CapsuleNotificationListener? = null
        val activeNotificationsMap = mutableMapOf<String, StatusBarNotification>()

        fun openNotification(context: Context, eventId: String, fallbackPackageName: String = ""): Boolean {
            val sbn = activeNotificationsMap[eventId]
            val contentIntent = sbn?.notification?.contentIntent
            if (contentIntent != null) {
                try {
                    contentIntent.send()
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val pkg = fallbackPackageName.ifEmpty { sbn?.packageName ?: "" }
            if (pkg.isNotEmpty()) {
                try {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (eventId == "music" || pkg.isNotEmpty()) {
                val mediaIntents = listOf(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC),
                    Intent("android.intent.action.MUSIC_PLAYER")
                )
                for (intent in mediaIntents) {
                    try {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        return true
                    } catch (_: Exception) {}
                }
            }
            return false
        }

        fun getNotificationActions(eventId: String): List<String> {
            val sbn = activeNotificationsMap[eventId] ?: return emptyList()
            val actions = sbn.notification.actions ?: return emptyList()
            return actions.filter { it.remoteInputs.isNullOrEmpty() }
                .mapNotNull { it.title?.toString()?.takeIf { t -> t.isNotBlank() } }
                .take(3)
        }

        fun replyToNotification(eventId: String, text: String): Boolean {
            val sbn = activeNotificationsMap[eventId] ?: return false
            val notification = sbn.notification
            val actions = notification.actions ?: return false
            
            for (action in actions) {
                val remoteInputs = action.remoteInputs ?: continue
                for (remoteInput in remoteInputs) {
                    val bundle = Bundle().apply {
                        putCharSequence(remoteInput.resultKey, text)
                    }
                    val intent = Intent()
                    android.app.RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)
                    try {
                        action.actionIntent.send(instance, 0, intent)
                        return true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            return false
        }

        fun resyncActiveClockNotifications() {
            val svc = instance ?: return
            try {
                svc.activeNotifications?.forEach { sbn ->
                    if (svc.isSystemClockPackage(sbn.packageName)) {
                        svc.onNotificationPosted(sbn)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun startSystemClockTimer(
            context: Context,
            seconds: Int,
            label: String = "Timer",
            skipUi: Boolean = true
        ): Boolean {
            return try {
                val intent = Intent(android.provider.AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(android.provider.AlarmClock.EXTRA_LENGTH, seconds.coerceAtLeast(1))
                    putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, label)
                    putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, skipUi)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        fun openClockApp(
            context: Context,
            eventId: String,
            packageName: String = "",
            isTimer: Boolean = true
        ): Boolean {
            val fallbackKey = if (isTimer) "timer" else "stopwatch"
            val sbn = activeNotificationsMap[eventId] ?: activeNotificationsMap[fallbackKey]
            val contentIntent = sbn?.notification?.contentIntent
            if (contentIntent != null) {
                try {
                    contentIntent.send()
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (isTimer) {
                try {
                    val showTimersIntent = Intent(android.provider.AlarmClock.ACTION_SHOW_TIMERS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(showTimersIntent)
                    return true
                } catch (_: Exception) {}
            }

            val candidates = buildList {
                if (packageName.isNotBlank()) add(packageName)
                if (!sbn?.packageName.isNullOrBlank()) add(sbn!!.packageName)
                add("com.google.android.deskclock")
                add("com.sec.android.app.clockpackage")
                add("com.oneplus.deskclock")
                add("com.coloros.alarmclock")
                add("com.oplus.alarmclock")
                add("com.miui.deskclock")
                add("com.android.deskclock")
            }
            val pm = context.packageManager
            for (pkg in candidates.distinct()) {
                try {
                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return true
                    }
                } catch (_: Exception) {}
            }
            return false
        }

        fun dismissNotification(eventId: String): Boolean {
            val sbn = activeNotificationsMap[eventId]
                ?: (if (eventId.startsWith("timer")) activeNotificationsMap["timer"] else null)
                ?: (if (eventId.startsWith("stopwatch")) activeNotificationsMap["stopwatch"] else null)
                ?: return false
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    instance?.cancelNotification(sbn.key)
                } else {
                    @Suppress("DEPRECATION")
                    instance?.cancelNotification(sbn.packageName, sbn.tag, sbn.id)
                }
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return false
        }

        fun triggerNotificationAction(eventId: String, actionTitle: String): Boolean {
            val sbn = activeNotificationsMap[eventId]
                ?: (if (eventId.startsWith("timer")) activeNotificationsMap["timer"] else null)
                ?: (if (eventId.startsWith("stopwatch")) activeNotificationsMap["stopwatch"] else null)
                ?: return false
            val actions = sbn.notification.actions ?: return false
            val searchAliases = when (actionTitle.lowercase()) {
                "answer" -> listOf("answer", "accept", "receive", "pick up")
                "decline", "end" -> listOf("decline", "reject", "dismiss", "ignore", "hang up", "end")
                "pause" -> listOf("pause", "hold", "stop")
                "start", "resume" -> listOf("resume", "start", "continue", "play")
                "lap" -> listOf("lap", "split")
                "reset" -> listOf("reset", "restart", "clear", "zero")
                "+1", "add", "+1m" -> listOf("+1", "add 1", "1 min", "1m", "+ 1", "extend")
                "stop", "delete", "dismiss" -> listOf("delete", "cancel", "dismiss", "stop", "close")
                else -> listOf(actionTitle.lowercase())
            }
            for (alias in searchAliases) {
                for (action in actions) {
                    val title = action.title?.toString()?.lowercase()?.trim().orEmpty()
                    if (title.contains(alias)) {
                        try {
                            action.actionIntent.send()
                            return true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            return false
        }

        fun markNotificationAsRead(eventId: String): Boolean {
            val sbn = activeNotificationsMap[eventId]
                ?: (if (eventId.startsWith("timer")) activeNotificationsMap["timer"] else null)
                ?: (if (eventId.startsWith("stopwatch")) activeNotificationsMap["stopwatch"] else null)
                ?: return false
            val actions = sbn.notification.actions
            var handled = false
            if (actions != null) {
                val markReadKeywords = listOf("read", "mark as read", "seen", "padh liya", "dismiss")
                for (action in actions) {
                    val title = action.title?.toString()?.lowercase()?.trim().orEmpty()
                    val isSemanticRead = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        action.semanticAction == NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ
                    } else false

                    if (isSemanticRead || markReadKeywords.any { title.contains(it) }) {
                        try {
                            action.actionIntent.send()
                            handled = true
                            break
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            // Always dismiss the notification from shade as part of mark as read
            dismissNotification(eventId)
            CapsuleStateManager.removeEvent(eventId)
            return handled || true
        }

        fun muteNotification(context: Context, eventId: String, packageName: String = ""): Boolean {
            val sbn = activeNotificationsMap[eventId]
            val pkg = if (packageName.isNotEmpty()) packageName else sbn?.packageName.orEmpty()
            
            // 1. Snooze via NotificationListenerService if supported
            if (sbn != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    instance?.snoozeNotification(sbn.key, 3600_000L) // Snooze for 1 hour
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. Add package to muted list in CapsuleSettings
            if (pkg.isNotEmpty()) {
                val settings = CapsuleSettings(context)
                val currentMuted = settings.mutedPackages.toMutableSet()
                currentMuted.add(pkg)
                settings.mutedPackages = currentMuted
            }

            // 3. Dismiss current notification and remove capsule
            dismissNotification(eventId)
            CapsuleStateManager.removeEvent(eventId)
            return true
        }
    }
}

