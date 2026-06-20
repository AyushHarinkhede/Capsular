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
import com.example.capsulebar.data.CapsuleEvent
import com.example.capsulebar.data.CapsuleSettings
import com.example.capsulebar.data.CapsuleStateManager

class CapsuleNotificationListener : NotificationListenerService() {

    private lateinit var settings: CapsuleSettings
    private val activeMediaControllers = mutableMapOf<String, MediaController>()
    private val controllerCallbacks = mutableMapOf<String, MediaController.Callback>()
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
                template.contains("CallStyle")

        if (isCallNotification) {
            val title = if (titleText.isNotEmpty()) titleText else "Active Call"
            val text = if (textText.isNotEmpty()) textText else "Call in progress"
            
            // Check if it's an incoming call (by looking for Answer/Accept action button)
            var isIncomingCall = false
            notification.actions?.forEach { action ->
                val titleStr = action.title.toString().lowercase()
                if (titleStr.contains("answer") || titleStr.contains("accept")) {
                    isIncomingCall = true
                }
            }

            val eventId = "call_${packageName}"
            activeNotificationsMap[eventId] = sbn
            CapsuleStateManager.postEvent(
                CapsuleEvent.Call(
                    id = eventId,
                    contactName = title,
                    durationText = text,
                    isIncoming = isIncomingCall,
                    priority = 88,
                    durationMs = 0 // Persistent until notification cleared
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

        // 5. Detect Timer / Stopwatch from Google Clock and AOSP deskclock
        val isClockApp = packageName == "com.google.android.deskclock" ||
                         packageName == "com.android.deskclock" ||
                         packageName.contains("deskclock") ||
                         packageName.contains("clock")

        if (isClockApp) {
            val rawTitle = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val rawText  = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()  ?: ""
            val combined = "${rawTitle.lowercase()} ${rawText.lowercase()}"

            when {
                // Timer — title contains a time pattern like "0:30" or "Timer" with countdown text
                combined.contains("timer") || combined.matches(Regex(".*\\d+:\\d+.*")) -> {
                    // Try to parse remaining seconds from title (e.g. "0:30", "1:05:00")
                    val timePattern = Regex("(\\d+):(\\d+)(?::(\\d+))?")
                    val matchResult = timePattern.find(rawTitle) ?: timePattern.find(rawText)
                    val remainingSecs: Long = if (matchResult != null) {
                        val groups = matchResult.groupValues
                        val a = groups[1].toLongOrNull() ?: 0L
                        val b = groups[2].toLongOrNull() ?: 0L
                        val c = groups.getOrNull(3)?.toLongOrNull()
                        if (c != null) a * 3600 + b * 60 + c else a * 60 + b
                    } else 0L

                    val eventId = "timer_${sbn.id}"
                    activeNotificationsMap[eventId] = sbn
                    CapsuleStateManager.postEvent(
                        CapsuleEvent.Timer(
                            id = eventId,
                            label = if (rawTitle.lowercase().contains("timer")) rawTitle else "Timer",
                            remainingSeconds = remainingSecs,
                            isRunning = true
                        )
                    )
                }
                // Stopwatch — "stopwatch" keyword or "lap" keyword
                combined.contains("stopwatch") || combined.contains("lap") -> {
                    val timePattern = Regex("(\\d+):(\\d+)(?::(\\d+))?")
                    val matchResult = timePattern.find(rawTitle) ?: timePattern.find(rawText)
                    val elapsedSecs: Long = if (matchResult != null) {
                        val groups = matchResult.groupValues
                        val a = groups[1].toLongOrNull() ?: 0L
                        val b = groups[2].toLongOrNull() ?: 0L
                        val c = groups.getOrNull(3)?.toLongOrNull()
                        if (c != null) a * 3600 + b * 60 + c else a * 60 + b
                    } else 0L

                    val eventId = "stopwatch_${sbn.id}"
                    activeNotificationsMap[eventId] = sbn
                    CapsuleStateManager.postEvent(
                        CapsuleEvent.Stopwatch(
                            id = eventId,
                            elapsedSeconds = elapsedSecs,
                            isRunning = true,
                            label = "Stopwatch"
                        )
                    )
                }
            }
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
        val isMaps = packageName == "com.google.android.apps.maps" && settings.isNavigationEnabled

        if (isMedia) {
            // Already handled via MediaSessionManager but kept as fallback
            setupMediaController(packageName, token!!)
        } else if (isMaps) {
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            if (title.isNotEmpty() || text.isNotEmpty()) {
                val eventId = "navigation"
                activeNotificationsMap[eventId] = sbn
                CapsuleStateManager.postEvent(
                    CapsuleEvent.Navigation(
                        instruction = title,
                        distance = text
                    )
                )
            }
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
            if (!isOngoing) {
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                if (title.isNotEmpty() || text.isNotEmpty()) {
                    var appLabel = packageName
                    var appIconBitmap: Bitmap? = null
                    try {
                        val pm = packageManager
                        val appInfo = pm.getApplicationInfo(packageName, 0)
                        appLabel = pm.getApplicationLabel(appInfo).toString()
                        
                        val largeIcon = notification.largeIcon
                        if (largeIcon != null) {
                            appIconBitmap = largeIcon
                        } else {
                            val drawable = pm.getApplicationIcon(appInfo)
                            if (drawable != null) {
                                appIconBitmap = drawableToBitmap(drawable)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val eventId = "notification_${packageName}_${sbn.id}"
                    activeNotificationsMap[eventId] = sbn
                    CapsuleStateManager.postEvent(
                        CapsuleEvent.Notification(
                            id = eventId,
                            packageName = packageName,
                            appName = appLabel,
                            title = title,
                            text = text,
                            appIcon = appIconBitmap
                        )
                    )
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

        if (packageName == "com.google.android.apps.maps") {
            CapsuleStateManager.removeEvent("navigation")
        }

        // Clean up Clock app events (Timer / Stopwatch)
        val isClockApp = packageName == "com.google.android.deskclock" ||
                         packageName == "com.android.deskclock" ||
                         packageName.contains("deskclock") ||
                         packageName.contains("clock")
        if (isClockApp) {
            CapsuleStateManager.removeEvent("timer_${sbn.id}")
            CapsuleStateManager.removeEvent("stopwatch_${sbn.id}")
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
        activeNotificationsMap.remove("progress_${packageName}")
        activeNotificationsMap.remove("notification_${packageName}_${sbn.id}")
    }

    private fun setupMediaController(packageName: String, token: MediaSession.Token) {
        if (activeMediaControllers.containsKey(packageName)) return

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

    private fun updateMusicState(controller: MediaController, packageName: String) {
        if (!settings.isMusicEnabled) return

        val metadata = controller.metadata
        val state = controller.playbackState

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Track"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
        val isPlaying = state != null && state.state == PlaybackState.STATE_PLAYING
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = state?.position ?: 0L
        val albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        CapsuleStateManager.postEvent(
            CapsuleEvent.Music(
                title = title,
                artist = artist,
                isPlaying = isPlaying,
                duration = duration,
                position = position,
                albumArt = albumArt,
                packageName = packageName
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

    private fun handleMediaAction(action: String) {
        val controller = activeMediaControllers.values.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: activeMediaControllers.values.firstOrNull() ?: return
        
        try {
            when (action) {
                "play" -> controller.transportControls.play()
                "pause" -> controller.transportControls.pause()
                "next" -> controller.transportControls.skipToNext()
                "previous" -> controller.transportControls.skipToPrevious()
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

        fun dismissNotification(eventId: String): Boolean {
            val sbn = activeNotificationsMap[eventId] ?: return false
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
            val sbn = activeNotificationsMap[eventId] ?: return false
            val actions = sbn.notification.actions ?: return false
            for (action in actions) {
                if (action.title.toString().lowercase().contains(actionTitle.lowercase())) {
                    try {
                        action.actionIntent.send()
                        return true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            return false
        }
    }
}

