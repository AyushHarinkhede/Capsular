package com.example.capsulebar.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class CapsuleAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: ""
            if (packageName.isNotEmpty() && packageName != "com.android.systemui" && packageName != this.packageName) {
                com.example.capsulebar.data.CapsuleStateManager.updateForegroundPackage(packageName)
                com.example.capsulebar.service.CapsuleNotificationListener.resyncActiveClockNotifications()
            }
            
            val settings = com.example.capsulebar.data.CapsuleSettings(this)
            if (settings.hideOnNotificationPanel) {
                val keyguardManager = getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
                val isLocked = keyguardManager.isKeyguardLocked

                val isSystemUiActive = packageName == "com.android.systemui"
                if (isSystemUiActive && !isLocked) {
                    val currentMode = com.example.capsulebar.data.CapsuleStateManager.uiState.value.displayMode
                    if (currentMode == com.example.capsulebar.data.DisplayMode.EXPANDED || currentMode == com.example.capsulebar.data.DisplayMode.SPLIT) {
                        // Prevent control panel from hiding or covering big/double capsule
                        com.example.capsulebar.data.CapsuleStateManager.setIsNotificationPanelVisible(false)
                        dismissNotificationShade()
                    } else {
                        com.example.capsulebar.data.CapsuleStateManager.setIsNotificationPanelVisible(true)
                    }
                } else if (!isSystemUiActive) {
                    com.example.capsulebar.data.CapsuleStateManager.setIsNotificationPanelVisible(false)
                }
            }
        }
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    companion object {
        private var instance: CapsuleAccessibilityService? = null

        fun expandNotificationShade(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) ?: false
        }

        fun dismissNotificationShade(): Boolean {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                return instance?.performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE) ?: false
            } else {
                return instance?.performGlobalAction(GLOBAL_ACTION_BACK) ?: false
            }
        }

        fun lockDeviceScreen(): Boolean {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                return instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) ?: false
            }
            return false
        }

        fun takeScreenshot(): Boolean {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                return instance?.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT) ?: false
            }
            return false
        }
    }
}
