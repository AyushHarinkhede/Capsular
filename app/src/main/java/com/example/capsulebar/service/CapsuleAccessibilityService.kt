package com.example.capsulebar.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class CapsuleAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Can monitor system events to react to window transitions
    }

    override fun onInterrupt() {
        // Service interrupted
    }
}
