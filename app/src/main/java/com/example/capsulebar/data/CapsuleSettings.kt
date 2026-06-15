package com.example.capsulebar.data

import android.content.Context
import android.content.SharedPreferences

class CapsuleSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("capsule_settings", Context.MODE_PRIVATE)

    var xOffset: Int
        get() = prefs.getInt("x_offset", 0)
        set(value) = prefs.edit().putInt("x_offset", value).apply()

    var yOffset: Int
        get() = prefs.getInt("y_offset", 40)
        set(value) = prefs.edit().putInt("y_offset", value).apply()

    var widthDp: Int
        get() = prefs.getInt("width_dp", 110)
        set(value) = prefs.edit().putInt("width_dp", value).apply()

    var heightDp: Int
        get() = prefs.getInt("height_dp", 36)
        set(value) = prefs.edit().putInt("height_dp", value).apply()

    var cornerRadiusDp: Int
        get() = prefs.getInt("corner_radius_dp", 18)
        set(value) = prefs.edit().putInt("corner_radius_dp", value).apply()

    var isBatteryEnabled: Boolean
        get() = prefs.getBoolean("is_battery_enabled", true)
        set(value) = prefs.edit().putBoolean("is_battery_enabled", value).apply()

    var isBluetoothEnabled: Boolean
        get() = prefs.getBoolean("is_bluetooth_enabled", true)
        set(value) = prefs.edit().putBoolean("is_bluetooth_enabled", value).apply()

    var isMusicEnabled: Boolean
        get() = prefs.getBoolean("is_music_enabled", true)
        set(value) = prefs.edit().putBoolean("is_music_enabled", value).apply()

    var isTimerEnabled: Boolean
        get() = prefs.getBoolean("is_timer_enabled", true)
        set(value) = prefs.edit().putBoolean("is_timer_enabled", value).apply()

    var isNetworkEnabled: Boolean
        get() = prefs.getBoolean("is_network_enabled", true)
        set(value) = prefs.edit().putBoolean("is_network_enabled", value).apply()

    var isSoundProfileEnabled: Boolean
        get() = prefs.getBoolean("is_sound_profile_enabled", true)
        set(value) = prefs.edit().putBoolean("is_sound_profile_enabled", value).apply()

    var isUsbEnabled: Boolean
        get() = prefs.getBoolean("is_usb_enabled", true)
        set(value) = prefs.edit().putBoolean("is_usb_enabled", value).apply()

    var isLockStateEnabled: Boolean
        get() = prefs.getBoolean("is_lock_state_enabled", true)
        set(value) = prefs.edit().putBoolean("is_lock_state_enabled", value).apply()

    var isNavigationEnabled: Boolean
        get() = prefs.getBoolean("is_navigation_enabled", true)
        set(value) = prefs.edit().putBoolean("is_navigation_enabled", value).apply()

    var isProgressEnabled: Boolean
        get() = prefs.getBoolean("is_progress_enabled", true)
        set(value) = prefs.edit().putBoolean("is_progress_enabled", value).apply()

    var isNotificationsEnabled: Boolean
        get() = prefs.getBoolean("is_notifications_enabled", true)
        set(value) = prefs.edit().putBoolean("is_notifications_enabled", value).apply()

    var isCalibrationMode: Boolean
        get() = prefs.getBoolean("is_calibration_mode", false)
        set(value) = prefs.edit().putBoolean("is_calibration_mode", value).apply()

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean("is_service_enabled", false)
        set(value) = prefs.edit().putBoolean("is_service_enabled", value).apply()

    var bluetoothImagePath: String?
        get() = prefs.getString("bluetooth_image_path", null)
        set(value) = prefs.edit().putString("bluetooth_image_path", value).apply()

    var cameraPosition: String
        get() = prefs.getString("camera_position", "Center") ?: "Center"
        set(value) = prefs.edit().putString("camera_position", value).apply()

    var cameraWidthDp: Int
        get() = prefs.getInt("camera_width_dp", 36)
        set(value) = prefs.edit().putInt("camera_width_dp", value).apply()

    var compactDurationSec: Int
        get() = prefs.getInt("compact_duration_sec", 5)
        set(value) = prefs.edit().putInt("compact_duration_sec", value).apply()

    var expandedDurationSec: Int
        get() = prefs.getInt("expanded_duration_sec", 8)
        set(value) = prefs.edit().putInt("expanded_duration_sec", value).apply()

    var dismissDelaySec: Int
        get() = prefs.getInt("dismiss_delay_sec", 3)
        set(value) = prefs.edit().putInt("dismiss_delay_sec", value).apply()

    var maxPopupWidthPercent: Int
        get() = prefs.getInt("max_popup_width_percent", 100)
        set(value) = prefs.edit().putInt("max_popup_width_percent", value).apply()

    var edgeRoundingPercent: Int
        get() = prefs.getInt("edge_rounding_percent", 60)
        set(value) = prefs.edit().putInt("edge_rounding_percent", value).apply()

    var showAsNotch: Boolean
        get() = prefs.getBoolean("show_as_notch", false)
        set(value) = prefs.edit().putBoolean("show_as_notch", value).apply()

    var addBackground: Boolean
        get() = prefs.getBoolean("add_background", true)
        set(value) = prefs.edit().putBoolean("add_background", value).apply()

    var showImages: Boolean
        get() = prefs.getBoolean("show_images", true)
        set(value) = prefs.edit().putBoolean("show_images", value).apply()

    var quickAnimations: Boolean
        get() = prefs.getBoolean("quick_animations", false)
        set(value) = prefs.edit().putBoolean("quick_animations", value).apply()

    var premiumAnimations: Boolean
        get() = prefs.getBoolean("premium_animations", true)
        set(value) = prefs.edit().putBoolean("premium_animations", value).apply()

    var reverseOrder: Boolean
        get() = prefs.getBoolean("reverse_order", false)
        set(value) = prefs.edit().putBoolean("reverse_order", value).apply()

    var maxTextLines: Int
        get() = prefs.getInt("max_text_lines", 20)
        set(value) = prefs.edit().putInt("max_text_lines", value).apply()

    var defaultColor: Int
        get() = prefs.getInt("default_color", 0xFF000000.toInt())
        set(value) = prefs.edit().putInt("default_color", value).apply()

    var autoColor: Boolean
        get() = prefs.getBoolean("auto_color", false)
        set(value) = prefs.edit().putBoolean("auto_color", value).apply()

    var useAppColors: Boolean
        get() = prefs.getBoolean("use_app_colors", false)
        set(value) = prefs.edit().putBoolean("use_app_colors", value).apply()

    var showMusicVisualizer: Boolean
        get() = prefs.getBoolean("show_music_visualizer", true)
        set(value) = prefs.edit().putBoolean("show_music_visualizer", value).apply()

    var useAndroidMusicControls: Boolean
        get() = prefs.getBoolean("use_android_music_controls", true)
        set(value) = prefs.edit().putBoolean("use_android_music_controls", value).apply()

    var iconOption: Int
        get() = prefs.getInt("icon_option", 0)
        set(value) = prefs.edit().putInt("icon_option", value).apply()

    var allowTwoPopups: Boolean
        get() = prefs.getBoolean("allow_two_popups", true)
        set(value) = prefs.edit().putBoolean("allow_two_popups", value).apply()

    var autoExpand: Boolean
        get() = prefs.getBoolean("auto_expand", true)
        set(value) = prefs.edit().putBoolean("auto_expand", value).apply()

    var sendReplies: Boolean
        get() = prefs.getBoolean("send_replies", true)
        set(value) = prefs.edit().putBoolean("send_replies", value).apply()

    var hideInForeground: Boolean
        get() = prefs.getBoolean("hide_in_foreground", true)
        set(value) = prefs.edit().putBoolean("hide_in_foreground", value).apply()

    var showInLandscape: Boolean
        get() = prefs.getBoolean("show_in_landscape", false)
        set(value) = prefs.edit().putBoolean("show_in_landscape", value).apply()

    var showAlways: Boolean
        get() = prefs.getBoolean("show_always", false)
        set(value) = prefs.edit().putBoolean("show_always", value).apply()

    var quickAccessApps: Boolean
        get() = prefs.getBoolean("quick_access_apps", false)
        set(value) = prefs.edit().putBoolean("quick_access_apps", value).apply()

    var showOnLockscreen: Boolean
        get() = prefs.getBoolean("show_on_lockscreen", true)
        set(value) = prefs.edit().putBoolean("show_on_lockscreen", value).apply()

    var hideOnNotificationPanel: Boolean
        get() = prefs.getBoolean("hide_on_notification_panel", true)
        set(value) = prefs.edit().putBoolean("hide_on_notification_panel", value).apply()

    var hideStatusbar: Boolean
        get() = prefs.getBoolean("hide_statusbar", true)
        set(value) = prefs.edit().putBoolean("hide_statusbar", value).apply()

    var notificationCountOption: Int
        get() = prefs.getInt("notification_count_option", 1)
        set(value) = prefs.edit().putInt("notification_count_option", value).apply()

    var autoHideSmallPopupHours: Int
        get() = prefs.getInt("auto_hide_small_popup_hours", 24)
        set(value) = prefs.edit().putInt("auto_hide_small_popup_hours", value).apply()

    var autoHideExpandedPopupSec: Int
        get() = prefs.getInt("auto_hide_expanded_popup_sec", 10)
        set(value) = prefs.edit().putInt("auto_hide_expanded_popup_sec", value).apply()

    var hideWhenTouchingOutside: Boolean
        get() = prefs.getBoolean("hide_when_touching_outside", true)
        set(value) = prefs.edit().putBoolean("hide_when_touching_outside", value).apply()

    // "Left" or "Right" — which side the split circle appears on
    var splitPosition: String
        get() = prefs.getString("split_position", "Right") ?: "Right"
        set(value) = prefs.edit().putString("split_position", value).apply()
}
