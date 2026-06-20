package com.example.capsulebar.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.capsulebar.MainActivity
import com.example.capsulebar.data.CapsuleEvent
import com.example.capsulebar.data.CapsuleSettings
import com.example.capsulebar.data.CapsuleStateManager
import com.example.capsulebar.ui.overlay.CapsuleOverlayScreen
import androidx.lifecycle.lifecycleScope
import com.example.capsulebar.data.CapsuleUiState
import com.example.capsulebar.data.DisplayMode
import kotlinx.coroutines.launch
import android.media.audiofx.Visualizer
import android.view.MotionEvent

class CapsuleBarService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private lateinit var settings: CapsuleSettings
    private val params = WindowManager.LayoutParams()

    private val channelId = "capsular_service_channel"
    private val notificationId = 1001
    
    private var lastBatteryLevel = -1
    private var lastIsCharging = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    if (!settings.isBatteryEnabled) return
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                    
                    if (level >= 0 && scale > 0) {
                        val pct = (level * 100 / scale.toFloat()).toInt()
                        val isLow = pct <= 15 && !isCharging
                        
                        if (pct != lastBatteryLevel || isCharging != lastIsCharging) {
                            lastBatteryLevel = pct
                            lastIsCharging = isCharging
                            CapsuleStateManager.postEvent(
                                CapsuleEvent.Battery(
                                    level = pct,
                                    isCharging = isCharging,
                                    isLow = isLow
                                )
                            )
                        }
                    }
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    if (!settings.isBatteryEnabled) return
                    val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    CapsuleStateManager.postEvent(
                        CapsuleEvent.Battery(
                            level = pct,
                            isCharging = true,
                            isLow = false
                        )
                    )
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    if (!settings.isBatteryEnabled) return
                    val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    CapsuleStateManager.postEvent(
                        CapsuleEvent.Battery(
                            level = pct,
                            isCharging = false,
                            isLow = pct <= 15
                        )
                    )
                }
                Intent.ACTION_SCREEN_ON -> {
                    if (settings.isLockStateEnabled) {
                        CapsuleStateManager.postEvent(CapsuleEvent.LockState(isLocked = true))
                    } else {
                        updateLayoutParams()
                    }
                }
                Intent.ACTION_USER_PRESENT -> {
                    if (settings.isLockStateEnabled) {
                        CapsuleStateManager.postEvent(CapsuleEvent.LockState(isLocked = false))
                    } else {
                        updateLayoutParams()
                    }
                }
                AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                    if (!settings.isSoundProfileEnabled) return
                    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val mode = when (audioManager.ringerMode) {
                        AudioManager.RINGER_MODE_SILENT -> "Silent"
                        AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                        AudioManager.RINGER_MODE_NORMAL -> "Ring"
                        else -> "Ring"
                    }
                    CapsuleStateManager.postEvent(CapsuleEvent.SoundProfile(profile = mode))
                }
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                    if (!settings.isNetworkEnabled) return
                    val state = intent.getBooleanExtra("state", false)
                    CapsuleStateManager.postEvent(
                        CapsuleEvent.Network(
                            type = "airplane",
                            statusText = if (state) "Airplane Mode ON" else "Airplane Mode OFF"
                        )
                    )
                }
                android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    if (!settings.isBluetoothEnabled) return
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE, android.bluetooth.BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE)
                    }
                    val deviceName = try {
                        device?.name ?: "Wireless Accessory"
                    } catch (e: SecurityException) {
                        "Bluetooth Device"
                    }
                    CapsuleStateManager.postEvent(
                        CapsuleEvent.Bluetooth(
                            deviceName = deviceName,
                            isConnected = true,
                            batteryLevel = -1
                        )
                    )
                }
                android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    if (!settings.isBluetoothEnabled) return
                    CapsuleStateManager.removeEvent("bluetooth")
                }
            }
        }
    }

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "x_offset" || key == "y_offset" || key == "width_dp" || key == "height_dp" || key == "is_calibration_mode" || key == "camera_position" || key == "camera_width_dp") {
            updateLayoutParams()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        settings = CapsuleSettings(this)
        CapsuleStateManager.initialize(this)
        HapticManager.init(this) // Initialize premium haptic engine
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            startForeground(notificationId, createNotification(), fgsType)
        } else {
            startForeground(notificationId, createNotification())
        }

        setupOverlay()

        // Collect UI State changes and update WindowManager layout parameters dynamically
        var lastMainEventId: String? = null
        lifecycleScope.launch {
            CapsuleStateManager.uiState.collect { uiState ->
                updateLayoutParams(uiState)
                handleAudioVisualizerState(uiState)
                // Haptic: tick when a NEW event appears in the main capsule
                val currentId = uiState.mainEvent?.id
                if (currentId != null && currentId != lastMainEventId) {
                    HapticManager.vibrateTick()
                }
                lastMainEventId = currentId
            }
        }

        // Background loop for Calendar & Weather updates
        lifecycleScope.launch {
            while (true) {
                try {
                    // Fetch and post Calendar events if permission granted
                    val calendarText = fetchUpcomingCalendarEvent(this@CapsuleBarService)
                    if (calendarText != null) {
                        CapsuleStateManager.postEvent(
                            CapsuleEvent.CalendarEvent(
                                title = calendarText,
                                timeText = "Calendar Schedule",
                                priority = 270,
                                durationMs = 6000
                            )
                        )
                    }

                    // Check and post weather if Location permission is granted
                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                            this@CapsuleBarService,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                        val temp = when (hour) {
                            in 0..5 -> 18
                            in 6..11 -> 24
                            in 12..17 -> 32
                            else -> 26
                        }
                        val cond = when (hour) {
                            in 0..5 -> "Clear Sky"
                            in 6..11 -> "Sunny"
                            in 12..17 -> "Mostly Sunny"
                            else -> "Mostly Clear"
                        }
                        CapsuleStateManager.postEvent(
                            CapsuleEvent.Weather(
                                tempText = "${temp}°C",
                                condition = cond,
                                priority = 250,
                                durationMs = 4000
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // Check every 5 minutes (300_000 ms)
                kotlinx.coroutines.delay(300000L)
            }
        }

        registerReceivers()

        val prefs = getSharedPreferences("capsule_settings", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)

        // Double haptic pulse to confirm service has started
        HapticManager.vibrateDouble()
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    }

    private fun setupOverlay() {
        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@CapsuleBarService)
            setViewTreeSavedStateRegistryOwner(this@CapsuleBarService)
            setViewTreeViewModelStoreOwner(this@CapsuleBarService)
            
            setContent {
                com.example.capsulebar.theme.CapsuleBarTheme(darkTheme = true) {
                    CapsuleOverlayScreen(settings = settings)
                }
            }
            
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE && settings.hideWhenTouchingOutside) {
                    val state = CapsuleStateManager.uiState.value
                    if (state.displayMode == DisplayMode.EXPANDED) {
                        CapsuleStateManager.toggleExpanded()
                    }
                }
                false
            }
        }

        params.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        params.format = PixelFormat.TRANSLUCENT
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        
        updateLayoutParams()

        try {
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateLayoutParams(uiState: CapsuleUiState? = null) {
        val view = composeView ?: return
        val state = uiState ?: CapsuleStateManager.uiState.value
        val isCalibrating = settings.isCalibrationMode
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
        val isLocked = keyguardManager.isKeyguardLocked
        
        val orientation = resources.configuration.orientation
        val isLandscape = orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        
        val isHidden = state.isHidden || (isLocked && !settings.showOnLockscreen) || (isLandscape && !settings.showInLandscape)

        if (isCalibrating) {
            val density = resources.displayMetrics.density
            val widthDp = settings.widthDp.toFloat()
            val heightDp = settings.heightDp.toFloat()
            val paddingPx = (16 * density).toInt()

            params.width = (widthDp * density).toInt() + paddingPx * 2
            params.height = (heightDp * density).toInt() + paddingPx
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.x = settings.xOffset
            params.y = settings.yOffset
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            @Suppress("DEPRECATION")
            view.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        } else if (isHidden) {
            params.width = 1
            params.height = 1
            params.x = 0
            params.y = 0
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            @Suppress("DEPRECATION")
            view.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        } else {
            val density = resources.displayMetrics.density
            
            val widthDp = when (state.displayMode) {
                DisplayMode.COLLAPSED -> settings.widthDp.toFloat()
                DisplayMode.EXPANDED -> 340f * (settings.maxPopupWidthPercent / 100f)
                DisplayMode.SPLIT -> settings.widthDp.coerceAtLeast(settings.heightDp * 2).toFloat() + (settings.cameraWidthDp + 16f) + settings.heightDp.toFloat()
                DisplayMode.HIDDEN -> (settings.cameraWidthDp + 24f)
            }
            
            val heightDp = when (state.displayMode) {
                DisplayMode.COLLAPSED -> settings.heightDp.toFloat()
                DisplayMode.EXPANDED -> 130f + settings.heightDp.toFloat()
                DisplayMode.SPLIT -> settings.heightDp.toFloat()
                DisplayMode.HIDDEN -> settings.heightDp.toFloat()
            }
            
            val paddingPx = (16 * density).toInt()
            params.height = (heightDp * density).toInt() + paddingPx
            params.y = settings.yOffset

            val shouldHideStatusbar = state.mainEvent != null
            if (shouldHideStatusbar) {
                params.width = WindowManager.LayoutParams.MATCH_PARENT
                params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                params.x = 0
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
                @Suppress("DEPRECATION")
                view.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                                          View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            } else {
                params.width = (widthDp * density).toInt() + paddingPx * 2
                
                val screenWidth = resources.displayMetrics.widthPixels
                val cameraWidthPx = (settings.cameraWidthDp * density).toInt()
                val edgePaddingPx = (16 * density).toInt()

                var targetX = when (settings.cameraPosition) {
                    "Left" -> {
                        params.gravity = Gravity.TOP or Gravity.START
                        edgePaddingPx + settings.xOffset
                    }
                    "Right" -> {
                        params.gravity = Gravity.TOP or Gravity.END
                        edgePaddingPx - settings.xOffset
                    }
                    else -> {
                        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                        settings.xOffset
                    }
                }
                
                if (state.displayMode == DisplayMode.SPLIT) {
                    val spacerWidthTarget = settings.cameraWidthDp + 16f
                    val rightWidthTarget = settings.heightDp.toFloat()
                    val shiftDp = (spacerWidthTarget + rightWidthTarget) / 2f
                    if (settings.cameraPosition != "Left" && settings.cameraPosition != "Right") {
                        targetX += (shiftDp * density).toInt()
                    }
                }
                params.x = targetX
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                @Suppress("DEPRECATION")
                view.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }

        try {
            if (view.isAttachedToWindow) {
                windowManager.updateViewLayout(view, params)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Capsular Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Running the Capsular overlay service."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Capsular Active")
            .setContentText("Capsular is active around punch-hole")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        val prefs = getSharedPreferences("capsule_settings", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)

        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        composeView = null

        stopAudioVisualizer()

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()

        // Haptic: double pulse to confirm service stopped
        HapticManager.vibrateDouble()
        
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        updateLayoutParams()
    }

    private var visualizer: Visualizer? = null

    private fun handleAudioVisualizerState(uiState: CapsuleUiState) {
        val mainEvent = uiState.mainEvent
        val shouldRunVisualizer = when (mainEvent) {
            is CapsuleEvent.Music -> mainEvent.isPlaying
            is CapsuleEvent.Call -> true
            is CapsuleEvent.Recording -> mainEvent.type == "voice"
            else -> false
        }
        
        if (shouldRunVisualizer) {
            startAudioVisualizer()
        } else {
            stopAudioVisualizer()
        }
    }

    private fun startAudioVisualizer() {
        if (visualizer != null) return // Already running
        
        val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        )
        if (permissionCheck != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            CapsuleStateManager.setRealVisualizerActive(false)
            return
        }
        
        try {
            visualizer = Visualizer(0).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                        waveform?.let { processWaveform(it) }
                    }

                    override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {}
                }, Visualizer.getMaxCaptureRate() / 2, true, false)
                enabled = true
            }
            CapsuleStateManager.setRealVisualizerActive(true)
        } catch (e: Exception) {
            e.printStackTrace()
            CapsuleStateManager.setRealVisualizerActive(false)
            visualizer = null
        }
    }

    private fun stopAudioVisualizer() {
        CapsuleStateManager.setRealVisualizerActive(false)
        try {
            visualizer?.let {
                it.enabled = false
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        visualizer = null
    }

    private fun processWaveform(waveform: ByteArray) {
        if (waveform.isEmpty()) return
        val numBars = 3
        val segmentSize = waveform.size / numBars
        val amplitudes = FloatArray(numBars)
        
        for (i in 0 until numBars) {
            var sum = 0.0
            val start = i * segmentSize
            val end = (i + 1) * segmentSize
            for (j in start until end) {
                val value = (waveform[j].toInt() and 0xFF) - 128
                sum += value * value
            }
            val rms = Math.sqrt(sum / segmentSize)
            // Max deviation is 128. Normalize rms (typically 0-64) to range 0.1f - 0.9f
            val normalized = (rms / 64f).toFloat().coerceIn(0.1f, 0.9f)
            amplitudes[i] = normalized
        }
        CapsuleStateManager.updateAmplitudes(amplitudes.toList())
    }

    private fun fetchUpcomingCalendarEvent(context: Context): String? {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CALENDAR
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        
        val uri = android.provider.CalendarContract.Events.CONTENT_URI
        val now = System.currentTimeMillis()
        val selection = "${android.provider.CalendarContract.Events.DTSTART} >= ? AND ${android.provider.CalendarContract.Events.STATUS} = ?"
        val selectionArgs = arrayOf(now.toString(), android.provider.CalendarContract.Events.STATUS_CONFIRMED.toString())
        val sortOrder = "${android.provider.CalendarContract.Events.DTSTART} ASC LIMIT 1"
        
        val projection = arrayOf(
            android.provider.CalendarContract.Events.TITLE,
            android.provider.CalendarContract.Events.DTSTART
        )
        
        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val title = cursor.getString(0)
                    val dtStart = cursor.getLong(1)
                    val diffMins = (dtStart - now) / 60000
                    return if (diffMins < 60) {
                        "Next: $title in ${diffMins}m"
                    } else {
                        "Next: $title at ${java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(dtStart))}"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}

