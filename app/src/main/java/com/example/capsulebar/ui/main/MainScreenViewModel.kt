package com.example.capsulebar.ui.main

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.capsulebar.data.CapsuleEvent
import com.example.capsulebar.data.CapsuleSettings
import com.example.capsulebar.data.CapsuleStateManager
import com.example.capsulebar.service.CapsuleBarService
import com.example.capsulebar.util.FlashlightController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainScreenViewModel(context: Context) : ViewModel() {

    private val appContext = context.applicationContext
    private val settings = CapsuleSettings(appContext)

    private val _isOverlayPermissionGranted = MutableStateFlow(false)
    val isOverlayPermissionGranted: StateFlow<Boolean> = _isOverlayPermissionGranted.asStateFlow()

    private val _isNotificationPermissionGranted = MutableStateFlow(false)
    val isNotificationPermissionGranted: StateFlow<Boolean> = _isNotificationPermissionGranted.asStateFlow()

    private val _isRecordAudioPermissionGranted = MutableStateFlow(false)
    val isRecordAudioPermissionGranted: StateFlow<Boolean> = _isRecordAudioPermissionGranted.asStateFlow()

    private val _isAccessibilityPermissionGranted = MutableStateFlow(false)
    val isAccessibilityPermissionGranted: StateFlow<Boolean> = _isAccessibilityPermissionGranted.asStateFlow()

    private val _isCalendarPermissionGranted = MutableStateFlow(false)
    val isCalendarPermissionGranted: StateFlow<Boolean> = _isCalendarPermissionGranted.asStateFlow()

    private val _isLocationPermissionGranted = MutableStateFlow(false)
    val isLocationPermissionGranted: StateFlow<Boolean> = _isLocationPermissionGranted.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _xOffset = MutableStateFlow(settings.xOffset)
    val xOffset: StateFlow<Int> = _xOffset.asStateFlow()

    private val _yOffset = MutableStateFlow(settings.yOffset)
    val yOffset: StateFlow<Int> = _yOffset.asStateFlow()

    private val _widthDp = MutableStateFlow(settings.widthDp)
    val widthDp: StateFlow<Int> = _widthDp.asStateFlow()

    private val _heightDp = MutableStateFlow(settings.heightDp)
    val heightDp: StateFlow<Int> = _heightDp.asStateFlow()

    private val _cornerRadiusDp = MutableStateFlow(settings.cornerRadiusDp)
    val cornerRadiusDp: StateFlow<Int> = _cornerRadiusDp.asStateFlow()

    private val _isBatteryEnabled = MutableStateFlow(settings.isBatteryEnabled)
    val isBatteryEnabled = _isBatteryEnabled.asStateFlow()

    private val _isMusicEnabled = MutableStateFlow(settings.isMusicEnabled)
    val isMusicEnabled = _isMusicEnabled.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(settings.isBluetoothEnabled)
    val isBluetoothEnabled = _isBluetoothEnabled.asStateFlow()

    private val _isTimerEnabled = MutableStateFlow(settings.isTimerEnabled)
    val isTimerEnabled = _isTimerEnabled.asStateFlow()

    private val _isNetworkEnabled = MutableStateFlow(settings.isNetworkEnabled)
    val isNetworkEnabled = _isNetworkEnabled.asStateFlow()

    private val _isNotificationsEnabled = MutableStateFlow(settings.isNotificationsEnabled)
    val isNotificationsEnabled = _isNotificationsEnabled.asStateFlow()

    private val _isCalibrationMode = MutableStateFlow(settings.isCalibrationMode)
    val isCalibrationMode = _isCalibrationMode.asStateFlow()

    private val _bluetoothImagePath = MutableStateFlow(settings.bluetoothImagePath)
    val bluetoothImagePath = _bluetoothImagePath.asStateFlow()

    private val _cameraPosition = MutableStateFlow(settings.cameraPosition)
    val cameraPosition: StateFlow<String> = _cameraPosition.asStateFlow()

    private val _cameraWidthDp = MutableStateFlow(settings.cameraWidthDp)
    val cameraWidthDp: StateFlow<Int> = _cameraWidthDp.asStateFlow()

    private val _compactDurationSec = MutableStateFlow(settings.compactDurationSec)
    val compactDurationSec: StateFlow<Int> = _compactDurationSec.asStateFlow()

    private val _expandedDurationSec = MutableStateFlow(settings.expandedDurationSec)
    val expandedDurationSec: StateFlow<Int> = _expandedDurationSec.asStateFlow()

    private val _dismissDelaySec = MutableStateFlow(settings.dismissDelaySec)
    val dismissDelaySec: StateFlow<Int> = _dismissDelaySec.asStateFlow()

    private val _maxPopupWidthPercent = MutableStateFlow(settings.maxPopupWidthPercent)
    val maxPopupWidthPercent: StateFlow<Int> = _maxPopupWidthPercent.asStateFlow()

    private val _edgeRoundingPercent = MutableStateFlow(settings.edgeRoundingPercent)
    val edgeRoundingPercent: StateFlow<Int> = _edgeRoundingPercent.asStateFlow()

    private val _showAsNotch = MutableStateFlow(settings.showAsNotch)
    val showAsNotch: StateFlow<Boolean> = _showAsNotch.asStateFlow()

    private val _addBackground = MutableStateFlow(settings.addBackground)
    val addBackground: StateFlow<Boolean> = _addBackground.asStateFlow()

    private val _showImages = MutableStateFlow(settings.showImages)
    val showImages: StateFlow<Boolean> = _showImages.asStateFlow()

    private val _quickAnimations = MutableStateFlow(settings.quickAnimations)
    val quickAnimations: StateFlow<Boolean> = _quickAnimations.asStateFlow()

    private val _premiumAnimations = MutableStateFlow(settings.premiumAnimations)
    val premiumAnimations: StateFlow<Boolean> = _premiumAnimations.asStateFlow()

    private val _reverseOrder = MutableStateFlow(settings.reverseOrder)
    val reverseOrder: StateFlow<Boolean> = _reverseOrder.asStateFlow()

    private val _maxTextLines = MutableStateFlow(settings.maxTextLines)
    val maxTextLines: StateFlow<Int> = _maxTextLines.asStateFlow()

    private val _defaultColor = MutableStateFlow(settings.defaultColor)
    val defaultColor: StateFlow<Int> = _defaultColor.asStateFlow()

    private val _autoColor = MutableStateFlow(settings.autoColor)
    val autoColor: StateFlow<Boolean> = _autoColor.asStateFlow()

    private val _useAppColors = MutableStateFlow(settings.useAppColors)
    val useAppColors: StateFlow<Boolean> = _useAppColors.asStateFlow()

    private val _showMusicVisualizer = MutableStateFlow(settings.showMusicVisualizer)
    val showMusicVisualizer: StateFlow<Boolean> = _showMusicVisualizer.asStateFlow()

    private val _useAndroidMusicControls = MutableStateFlow(settings.useAndroidMusicControls)
    val useAndroidMusicControls: StateFlow<Boolean> = _useAndroidMusicControls.asStateFlow()

    private val _iconOption = MutableStateFlow(settings.iconOption)
    val iconOption: StateFlow<Int> = _iconOption.asStateFlow()

    private val _allowTwoPopups = MutableStateFlow(settings.allowTwoPopups)
    val allowTwoPopups: StateFlow<Boolean> = _allowTwoPopups.asStateFlow()

    private val _autoExpand = MutableStateFlow(settings.autoExpand)
    val autoExpand: StateFlow<Boolean> = _autoExpand.asStateFlow()

    private val _sendReplies = MutableStateFlow(settings.sendReplies)
    val sendReplies: StateFlow<Boolean> = _sendReplies.asStateFlow()

    private val _hideInForeground = MutableStateFlow(settings.hideInForeground)
    val hideInForeground: StateFlow<Boolean> = _hideInForeground.asStateFlow()

    private val _showInLandscape = MutableStateFlow(settings.showInLandscape)
    val showInLandscape: StateFlow<Boolean> = _showInLandscape.asStateFlow()

    private val _showAlways = MutableStateFlow(settings.showAlways)
    val showAlways: StateFlow<Boolean> = _showAlways.asStateFlow()

    private val _quickAccessApps = MutableStateFlow(settings.quickAccessApps)
    val quickAccessApps: StateFlow<Boolean> = _quickAccessApps.asStateFlow()

    private val _showOnLockscreen = MutableStateFlow(settings.showOnLockscreen)
    val showOnLockscreen: StateFlow<Boolean> = _showOnLockscreen.asStateFlow()

    private val _hideOnNotificationPanel = MutableStateFlow(settings.hideOnNotificationPanel)
    val hideOnNotificationPanel: StateFlow<Boolean> = _hideOnNotificationPanel.asStateFlow()

    private val _notificationCountOption = MutableStateFlow(settings.notificationCountOption)
    val notificationCountOption: StateFlow<Int> = _notificationCountOption.asStateFlow()

    private val _autoHideSmallPopupHours = MutableStateFlow(settings.autoHideSmallPopupHours)
    val autoHideSmallPopupHours: StateFlow<Int> = _autoHideSmallPopupHours.asStateFlow()

    private val _autoHideExpandedPopupSec = MutableStateFlow(settings.autoHideExpandedPopupSec)
    val autoHideExpandedPopupSec: StateFlow<Int> = _autoHideExpandedPopupSec.asStateFlow()

    private val _hideWhenTouchingOutside = MutableStateFlow(settings.hideWhenTouchingOutside)
    val hideWhenTouchingOutside: StateFlow<Boolean> = _hideWhenTouchingOutside.asStateFlow()

    private val _splitPosition = MutableStateFlow(settings.splitPosition)
    val splitPosition: StateFlow<String> = _splitPosition.asStateFlow()

    private val _nfcWristWatchTagId = MutableStateFlow(settings.nfcWristWatchTagId)
    val nfcWristWatchTagId: StateFlow<String> = _nfcWristWatchTagId.asStateFlow()

    private val _nfcChetakTagId = MutableStateFlow(settings.nfcChetakTagId)
    val nfcChetakTagId: StateFlow<String> = _nfcChetakTagId.asStateFlow()

    private val _activeRegistrationTask = MutableStateFlow<String?>(null)
    val activeRegistrationTask: StateFlow<String?> = _activeRegistrationTask.asStateFlow()

    fun startNfcRegistration(task: String) {
        _activeRegistrationTask.value = task
    }

    fun cancelNfcRegistration() {
        _activeRegistrationTask.value = null
    }

    fun bindNfcTag(tagId: String) {
        val task = _activeRegistrationTask.value ?: return
        if (task == "wrist_watch") {
            settings.nfcWristWatchTagId = tagId
            _nfcWristWatchTagId.value = tagId
        } else if (task == "chetak") {
            settings.nfcChetakTagId = tagId
            _nfcChetakTagId.value = tagId
        }
        _activeRegistrationTask.value = null
    }

    fun clearNfcTag(task: String) {
        if (task == "wrist_watch") {
            settings.nfcWristWatchTagId = ""
            _nfcWristWatchTagId.value = ""
        } else if (task == "chetak") {
            settings.nfcChetakTagId = ""
            _nfcChetakTagId.value = ""
        }
    }

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "x_offset" -> _xOffset.value = settings.xOffset
            "y_offset" -> _yOffset.value = settings.yOffset
            "width_dp" -> _widthDp.value = settings.widthDp
            "height_dp" -> _heightDp.value = settings.heightDp
            "corner_radius_dp" -> _cornerRadiusDp.value = settings.cornerRadiusDp
            "is_calibration_mode" -> _isCalibrationMode.value = settings.isCalibrationMode
            "bluetooth_image_path" -> _bluetoothImagePath.value = settings.bluetoothImagePath
            "camera_position" -> _cameraPosition.value = settings.cameraPosition
            "camera_width_dp" -> _cameraWidthDp.value = settings.cameraWidthDp
            "compact_duration_sec" -> _compactDurationSec.value = settings.compactDurationSec
            "expanded_duration_sec" -> _expandedDurationSec.value = settings.expandedDurationSec
            "dismiss_delay_sec" -> _dismissDelaySec.value = settings.dismissDelaySec
            "max_popup_width_percent" -> _maxPopupWidthPercent.value = settings.maxPopupWidthPercent
            "edge_rounding_percent" -> _edgeRoundingPercent.value = settings.edgeRoundingPercent
            "show_as_notch" -> _showAsNotch.value = settings.showAsNotch
            "add_background" -> _addBackground.value = settings.addBackground
            "show_images" -> _showImages.value = settings.showImages
            "quick_animations" -> _quickAnimations.value = settings.quickAnimations
            "premium_animations" -> _premiumAnimations.value = settings.premiumAnimations
            "reverse_order" -> _reverseOrder.value = settings.reverseOrder
            "max_text_lines" -> _maxTextLines.value = settings.maxTextLines
            "default_color" -> _defaultColor.value = settings.defaultColor
            "auto_color" -> _autoColor.value = settings.autoColor
            "use_app_colors" -> _useAppColors.value = settings.useAppColors
            "show_music_visualizer" -> _showMusicVisualizer.value = settings.showMusicVisualizer
            "use_android_music_controls" -> _useAndroidMusicControls.value = settings.useAndroidMusicControls
            "icon_option" -> _iconOption.value = settings.iconOption
            "allow_two_popups" -> _allowTwoPopups.value = settings.allowTwoPopups
            "auto_expand" -> _autoExpand.value = settings.autoExpand
            "send_replies" -> _sendReplies.value = settings.sendReplies
            "hide_in_foreground" -> _hideInForeground.value = settings.hideInForeground
            "show_in_landscape" -> _showInLandscape.value = settings.showInLandscape
            "show_always" -> _showAlways.value = settings.showAlways
            "quick_access_apps" -> _quickAccessApps.value = settings.quickAccessApps
            "show_on_lockscreen" -> _showOnLockscreen.value = settings.showOnLockscreen
            "hide_on_notification_panel" -> _hideOnNotificationPanel.value = settings.hideOnNotificationPanel
            "notification_count_option" -> _notificationCountOption.value = settings.notificationCountOption
            "auto_hide_small_popup_hours" -> _autoHideSmallPopupHours.value = settings.autoHideSmallPopupHours
            "auto_hide_expanded_popup_sec" -> _autoHideExpandedPopupSec.value = settings.autoHideExpandedPopupSec
            "hide_when_touching_outside" -> _hideWhenTouchingOutside.value = settings.hideWhenTouchingOutside
            "split_position" -> _splitPosition.value = settings.splitPosition
            "nfc_wrist_watch_tag_id" -> _nfcWristWatchTagId.value = settings.nfcWristWatchTagId
            "nfc_chetak_tag_id" -> _nfcChetakTagId.value = settings.nfcChetakTagId
        }
    }

    init {
        checkPermissions()
        checkServiceStatus()
        val prefs = appContext.getSharedPreferences("capsule_settings", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onCleared() {
        val prefs = appContext.getSharedPreferences("capsule_settings", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        super.onCleared()
    }

    fun checkPermissions() {
        _isOverlayPermissionGranted.value = Settings.canDrawOverlays(appContext)
        
        val enabledListeners = Settings.Secure.getString(
            appContext.contentResolver,
            "enabled_notification_listeners"
        ) ?: ""
        _isNotificationPermissionGranted.value = enabledListeners.contains(appContext.packageName)

        _isRecordAudioPermissionGranted.value = androidx.core.content.ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        // Accessibility Service Check
        val accessibilityService = "${appContext.packageName}/com.example.capsulebar.service.CapsuleAccessibilityService"
        var accessibilityEnabled = false
        try {
            val enabledServices = Settings.Secure.getString(
                appContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            accessibilityEnabled = enabledServices.contains(accessibilityService)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _isAccessibilityPermissionGranted.value = accessibilityEnabled

        // Calendar Read Permission Check
        _isCalendarPermissionGranted.value = androidx.core.content.ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.READ_CALENDAR
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        // Location Check (for weather)
        _isLocationPermissionGranted.value = androidx.core.content.ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun checkServiceStatus() {
        val manager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        var running = false
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (CapsuleBarService::class.java.name == service.service.className) {
                running = true
                break
            }
        }
        _isServiceRunning.value = running
    }

    fun toggleService() {
        if (_isServiceRunning.value) {
            val intent = Intent(appContext, CapsuleBarService::class.java)
            appContext.stopService(intent)
            _isServiceRunning.value = false
            settings.isServiceEnabled = false
        } else {
            if (Settings.canDrawOverlays(appContext)) {
                val intent = Intent(appContext, CapsuleBarService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(intent)
                } else {
                    appContext.startService(intent)
                }
                settings.isServiceEnabled = true
                viewModelScope.launch {
                    kotlinx.coroutines.delay(200)
                    checkServiceStatus()
                }
            }
        }
    }

    fun updateXOffset(value: Int) {
        settings.xOffset = value
        _xOffset.value = value
    }

    fun updateYOffset(value: Int) {
        settings.yOffset = value
        _yOffset.value = value
    }

    fun updateWidthDp(value: Int) {
        settings.widthDp = value
        _widthDp.value = value
    }

    fun updateHeightDp(value: Int) {
        settings.heightDp = value
        _heightDp.value = value
    }

    fun updateCornerRadiusDp(value: Int) {
        settings.cornerRadiusDp = value
        _cornerRadiusDp.value = value
    }

    fun toggleBattery(value: Boolean) {
        settings.isBatteryEnabled = value
        _isBatteryEnabled.value = value
    }

    fun toggleMusic(value: Boolean) {
        settings.isMusicEnabled = value
        _isMusicEnabled.value = value
    }

    fun toggleBluetooth(value: Boolean) {
        settings.isBluetoothEnabled = value
        _isBluetoothEnabled.value = value
    }

    fun toggleTimer(value: Boolean) {
        settings.isTimerEnabled = value
        _isTimerEnabled.value = value
    }

    fun toggleNetwork(value: Boolean) {
        settings.isNetworkEnabled = value
        _isNetworkEnabled.value = value
    }

    fun toggleNotifications(value: Boolean) {
        settings.isNotificationsEnabled = value
        _isNotificationsEnabled.value = value
    }

    fun toggleCalibrationMode(value: Boolean) {
        settings.isCalibrationMode = value
        _isCalibrationMode.value = value
    }

    fun updateBluetoothImage(path: String?) {
        settings.bluetoothImagePath = path
        _bluetoothImagePath.value = path
    }

    fun updateCameraPosition(value: String) {
        settings.cameraPosition = value
        _cameraPosition.value = value
    }

    fun updateCameraWidthDp(value: Int) {
        settings.cameraWidthDp = value
        _cameraWidthDp.value = value
    }

    fun updateCompactDurationSec(value: Int) {
        settings.compactDurationSec = value
        _compactDurationSec.value = value
    }

    fun updateExpandedDurationSec(value: Int) {
        settings.expandedDurationSec = value
        _expandedDurationSec.value = value
    }

    fun updateDismissDelaySec(value: Int) {
        settings.dismissDelaySec = value
        _dismissDelaySec.value = value
    }

    fun triggerMockBatteryCharging() {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Battery(
                level = 88,
                isCharging = true,
                isLow = false
            )
        )
    }

    fun triggerMockLowBattery() {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Battery(
                level = 15,
                isCharging = false,
                isLow = true,
                priority = 92
            )
        )
    }

    fun triggerMockFaceID(success: Boolean) {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Authentication(
                label = "Biometric Unlock",
                success = success
            )
        )
    }

    fun triggerMockMobilePay() {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Authentication(
                label = "Secure Mobile Pay",
                success = true
            )
        )
    }

    fun triggerMockQuickShare() {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Progress(
                id = "quickshare",
                title = "File Transfer via Quick Share",
                progress = 65,
                max = 100
            )
        )
    }

    fun triggerMockBluetoothHeadset(name: String, battery: Int) {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Bluetooth(
                deviceName = name,
                isConnected = true,
                batteryLevel = battery
            )
        )
    }

    fun triggerMockFocusMode(name: String) {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Network(
                id = "focus_mode",
                type = "focus",
                statusText = "$name Focus Mode",
                priority = 48,
                durationMs = 3000
            )
        )
    }

    fun triggerMockOngoingCall(contact: String, incoming: Boolean) {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Call(
                contactName = contact,
                durationText = "05:14",
                isIncoming = incoming
            )
        )
    }

    fun triggerMockScreenRecording() {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Recording(
                type = "screen",
                durationText = "02:30"
            )
        )
    }

    fun triggerMockVoiceMemo() {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Recording(
                type = "voice",
                durationText = "00:45"
            )
        )
    }

    fun triggerMockHotspot(conn: Int) {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Hotspot(connections = conn)
        )
    }

    fun triggerMockRideSharing(appName: String, minsText: String, progress: Float) {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Delivery(
                id = "ride",
                appName = appName,
                statusText = minsText,
                progress = progress
            )
        )
    }

    fun triggerMockFoodDelivery(appName: String, status: String, progress: Float) {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Delivery(
                id = "food",
                appName = appName,
                statusText = status,
                progress = progress
            )
        )
    }

    fun triggerMockSystemToggle(name: String, enabled: Boolean) {
        val id = "toggle_${name.lowercase().replace(" ", "_")}"
        val isCurrentlyActive = CapsuleStateManager.isEventActive(id)
        if (isCurrentlyActive) {
            CapsuleStateManager.removeEvent(id)
            if (name.equals("Flashlight", ignoreCase = true)) {
                FlashlightController.turnOff(appContext)
            }
        } else {
            val priorityValue = if (name.equals("Flashlight", ignoreCase = true)) 750 else 400
            val duration = if (name.equals("Flashlight", ignoreCase = true)) 0L else 2500L
            CapsuleStateManager.postEvent(
                CapsuleEvent.SystemToggle(
                    id = id,
                    name = name,
                    isEnabled = true,
                    priority = priorityValue,
                    durationMs = duration
                )
            )
            if (name.equals("Flashlight", ignoreCase = true)) {
                FlashlightController.turnOn(appContext, FlashlightController.intensityFlow.value)
            }
        }
    }

    fun triggerMockMusicPlayback() {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Music(
                title = "Acoustic Melody",
                artist = "Talented Musician",
                isPlaying = true,
                duration = 200000L,
                position = 45000L,
                packageName = "com.generic.musicplayer"
            )
        )
    }

    fun triggerMockTimer() {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Timer(
                label = "Timer",
                remainingSeconds = 295,
                isRunning = true
            )
        )
    }

    fun triggerMockStopwatch() {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Stopwatch(
                id = "stopwatch_mock",
                label = "Stopwatch",
                elapsedSeconds = 0,
                isRunning = true
            )
        )
    }

    fun triggerMockNavigation() {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Navigation(
                instruction = "In 200m turn left on Outer Ring Road",
                distance = "200 meters"
            )
        )
    }

    fun triggerMockProgress() {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Progress(
                id = "progress_mock",
                title = "Downloading System Update...",
                progress = 42,
                max = 100
            )
        )
    }

    fun triggerMockNotification() {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Notification(
                id = "notification_mock_chat",
                packageName = "com.generic.chat",
                appName = "Chat App",
                title = "Friend",
                text = "Hey, check out this new Capsular animation! It looks awesome! 🔥",
                appIcon = null
            )
        )
    }

    fun triggerMockHourlyTrackerSteps() {
        CapsuleStateManager.postEvent(
            CapsuleEvent.HourlyTracker(
                id = "tracker_steps",
                trackerName = "Hourly Steps",
                countText = "4,500 / 6,000 Steps",
                progress = 0.75f
            )
        )
    }

    fun triggerMockHourlyTrackerWater() {
        CapsuleStateManager.postEvent(
            CapsuleEvent.HourlyTracker(
                id = "tracker_water",
                trackerName = "Hourly Water Intake",
                countText = "3 / 8 Glasses (600ml)",
                progress = 0.375f
            )
        )
    }

    fun triggerMockCalendarEvent() {
        CapsuleStateManager.postEvent(
            CapsuleEvent.CalendarEvent(
                title = "Project Sync Meeting",
                timeText = "11:30 AM - 12:00 PM",
                location = "Conference Room A"
            )
        )
    }

    fun triggerMockWeather() {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Weather(
                tempText = "24°C",
                condition = "Partly Cloudy"
            )
        )
    }

    fun triggerMockAlarm() {
        CapsuleStateManager.postEvent(
            CapsuleEvent.Alarm(
                id = "alarm_mock",
                timeText = "07:30 AM",
                label = "Morning Alarm",
                isFiring = true
            )
        )
    }


    fun clearEvents() {
        // Also turn off flashlight when clearing all events
        FlashlightController.turnOff(appContext)
        CapsuleStateManager.clearAllEvents()
    }

    fun updateMaxPopupWidthPercent(value: Int) {
        settings.maxPopupWidthPercent = value
        _maxPopupWidthPercent.value = value
    }

    fun updateEdgeRoundingPercent(value: Int) {
        settings.edgeRoundingPercent = value
        _edgeRoundingPercent.value = value
    }

    fun toggleShowAsNotch(value: Boolean) {
        settings.showAsNotch = value
        _showAsNotch.value = value
    }

    fun toggleAddBackground(value: Boolean) {
        settings.addBackground = value
        _addBackground.value = value
    }

    fun toggleShowImages(value: Boolean) {
        settings.showImages = value
        _showImages.value = value
    }

    fun toggleQuickAnimations(value: Boolean) {
        settings.quickAnimations = value
        _quickAnimations.value = value
    }

    fun togglePremiumAnimations(value: Boolean) {
        settings.premiumAnimations = value
        _premiumAnimations.value = value
    }

    fun toggleReverseOrder(value: Boolean) {
        settings.reverseOrder = value
        _reverseOrder.value = value
    }

    fun updateMaxTextLines(value: Int) {
        settings.maxTextLines = value
        _maxTextLines.value = value
    }

    fun updateDefaultColor(value: Int) {
        settings.defaultColor = value
        _defaultColor.value = value
    }

    fun toggleAutoColor(value: Boolean) {
        settings.autoColor = value
        _autoColor.value = value
    }

    fun toggleUseAppColors(value: Boolean) {
        settings.useAppColors = value
        _useAppColors.value = value
    }

    fun toggleShowMusicVisualizer(value: Boolean) {
        settings.showMusicVisualizer = value
        _showMusicVisualizer.value = value
    }

    fun toggleUseAndroidMusicControls(value: Boolean) {
        settings.useAndroidMusicControls = value
        _useAndroidMusicControls.value = value
    }

    fun updateIconOption(value: Int) {
        settings.iconOption = value
        _iconOption.value = value
    }

    fun toggleAllowTwoPopups(value: Boolean) {
        settings.allowTwoPopups = value
        _allowTwoPopups.value = value
    }

    fun toggleAutoExpand(value: Boolean) {
        settings.autoExpand = value
        _autoExpand.value = value
    }

    fun toggleSendReplies(value: Boolean) {
        settings.sendReplies = value
        _sendReplies.value = value
    }

    fun toggleHideInForeground(value: Boolean) {
        settings.hideInForeground = value
        _hideInForeground.value = value
    }

    fun toggleShowInLandscape(value: Boolean) {
        settings.showInLandscape = value
        _showInLandscape.value = value
    }

    fun toggleShowAlways(value: Boolean) {
        settings.showAlways = value
        _showAlways.value = value
    }

    fun toggleQuickAccessApps(value: Boolean) {
        settings.quickAccessApps = value
        _quickAccessApps.value = value
    }

    fun toggleShowOnLockscreen(value: Boolean) {
        settings.showOnLockscreen = value
        _showOnLockscreen.value = value
    }

    fun toggleHideOnNotificationPanel(value: Boolean) {
        settings.hideOnNotificationPanel = value
        _hideOnNotificationPanel.value = value
    }

    fun updateNotificationCountOption(value: Int) {
        settings.notificationCountOption = value
        _notificationCountOption.value = value
    }

    fun updateAutoHideSmallPopupHours(value: Int) {
        settings.autoHideSmallPopupHours = value
        _autoHideSmallPopupHours.value = value
    }

    fun updateAutoHideExpandedPopupSec(value: Int) {
        settings.autoHideExpandedPopupSec = value
        _autoHideExpandedPopupSec.value = value
    }

    fun toggleHideWhenTouchingOutside(value: Boolean) {
        settings.hideWhenTouchingOutside = value
        _hideWhenTouchingOutside.value = value
    }

    fun updateSplitPosition(value: String) {
        settings.splitPosition = value
        _splitPosition.value = value
    }
}

