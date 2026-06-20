package com.example.capsulebar.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class CapsuleAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: ""
            
            val settings = com.example.capsulebar.data.CapsuleSettings(this)
            if (settings.hideOnNotificationPanel) {
                val keyguardManager = getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
                val isLocked = keyguardManager.isKeyguardLocked

                val isSystemUiActive = packageName == "com.android.systemui"
                if (isSystemUiActive && !isLocked) {
                    com.example.capsulebar.data.CapsuleStateManager.setIsNotificationPanelVisible(true)
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

        fun lockDeviceScreen(): Boolean {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                return instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) ?: false
            }
            return false
        }
    }
}
