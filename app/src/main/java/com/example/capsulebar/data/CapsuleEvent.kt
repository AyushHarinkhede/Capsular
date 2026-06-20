package com.example.capsulebar.data

import android.graphics.Bitmap

sealed interface CapsuleEvent {
    val id: String
    val priority: Int // Higher number = higher priority
    val durationMs: Long // Auto-hide duration if > 0 (0 means persistent)

    data class Battery(
        override val id: String = "battery",
        val level: Int,
        val isCharging: Boolean,
        val isLow: Boolean,
        override val priority: Int = 500,
        override val durationMs: Long = 4000
    ) : CapsuleEvent

    data class Music(
        override val id: String = "music",
        val title: String,
        val artist: String,
        val isPlaying: Boolean,
        val duration: Long = 0,
        val position: Long = 0,
        val albumArt: Bitmap? = null,
        val packageName: String = "",
        override val priority: Int = 900,
        override val durationMs: Long = 0
    ) : CapsuleEvent

    data class Bluetooth(
        override val id: String = "bluetooth",
        val deviceName: String,
        val isConnected: Boolean,
        val batteryLevel: Int = -1,
        override val priority: Int = 460,
        override val durationMs: Long = 4000
    ) : CapsuleEvent

    data class Network(
        override val id: String = "network",
        val type: String, // "wifi", "airplane"
        val statusText: String,
        override val priority: Int = 420,
        override val durationMs: Long = 3000
    ) : CapsuleEvent

    data class SoundProfile(
        override val id: String = "sound_profile",
        val profile: String, // "Ring", "Vibrate", "Silent"
        override val priority: Int = 700,
        override val durationMs: Long = 3000
    ) : CapsuleEvent

    data class USB(
        override val id: String = "usb",
        val isConnected: Boolean,
        override val priority: Int = 440,
        override val durationMs: Long = 3000
    ) : CapsuleEvent

    data class LockState(
        override val id: String = "lock_state",
        val isLocked: Boolean,
        override val priority: Int = 480,
        override val durationMs: Long = 2500
    ) : CapsuleEvent

    data class Timer(
        override val id: String = "timer",
        val label: String,
        val remainingSeconds: Long,
        val isRunning: Boolean,
        override val priority: Int = 350,
        override val durationMs: Long = 0
    ) : CapsuleEvent

    data class Navigation(
        override val id: String = "navigation",
        val instruction: String,
        val distance: String,
        override val priority: Int = 330,
        override val durationMs: Long = 0
    ) : CapsuleEvent

    data class Progress(
        override val id: String = "progress",
        val title: String,
        val progress: Int,
        val max: Int,
        override val priority: Int = 310,
        override val durationMs: Long = 0
    ) : CapsuleEvent

    data class Notification(
        override val id: String,
        val packageName: String,
        val appName: String,
        val title: String,
        val text: String,
        val appIcon: Bitmap? = null,
        override val priority: Int = 600,
        override val durationMs: Long = 5000
    ) : CapsuleEvent

    data class Authentication(
        override val id: String = "auth",
        val label: String, // "Face ID" or "Apple Pay"
        val success: Boolean,
        override val priority: Int = 550,
        override val durationMs: Long = 3000
    ) : CapsuleEvent

    data class Call(
        override val id: String = "call",
        val contactName: String,
        val durationText: String,
        val isIncoming: Boolean = false,
        override val priority: Int = 1000, // HIGHEST — always wins main capsule
        override val durationMs: Long = 0
    ) : CapsuleEvent

    data class Recording(
        override val id: String = "recording",
        val type: String, // "screen" or "voice"
        val durationText: String,
        override val priority: Int = 800,
        override val durationMs: Long = 0
    ) : CapsuleEvent

    data class Hotspot(
        override val id: String = "hotspot",
        val connections: Int,
        override val priority: Int = 280,
        override val durationMs: Long = 0
    ) : CapsuleEvent

    data class Delivery(
        override val id: String,
        val appName: String, // "Ride Sharing", "Food Delivery", etc.
        val statusText: String,
        val progress: Float, // 0.0f to 1.0f
        override val priority: Int = 300,
        override val durationMs: Long = 0
    ) : CapsuleEvent

    data class SystemToggle(
        override val id: String,
        val name: String, // "Wi-Fi", "Mobile Data", "DND", "Flashlight", "Low Power Mode", "Silent", "Ring", "Vibrate"
        val isEnabled: Boolean,
        // Flashlight toggle gets highest SystemToggle priority (750); others default to 400
        override val priority: Int = 400,
        override val durationMs: Long = 2500
    ) : CapsuleEvent

    data class Stopwatch(
        override val id: String = "stopwatch",
        val elapsedSeconds: Long,   // total elapsed time in seconds
        val isRunning: Boolean,
        val label: String = "Stopwatch",
        override val priority: Int = 340,
        override val durationMs: Long = 0  // persistent until dismissed
    ) : CapsuleEvent

    data class HourlyTracker(
        override val id: String = "hourly_tracker",
        val trackerName: String,
        val countText: String,
        val progress: Float, // 0.0f to 1.0f
        override val priority: Int = 260,
        override val durationMs: Long = 0
    ) : CapsuleEvent

    data class CalendarEvent(
        override val id: String = "calendar_event",
        val title: String,
        val timeText: String,
        val location: String = "",
        override val priority: Int = 270,
        override val durationMs: Long = 6000
    ) : CapsuleEvent

    data class Weather(
        override val id: String = "weather",
        val tempText: String,
        val condition: String,
        override val priority: Int = 250,
        override val durationMs: Long = 4000
    ) : CapsuleEvent

    data class Alarm(
        override val id: String = "alarm",
        val timeText: String,
        val label: String,
        val isFiring: Boolean,
        override val priority: Int = 950,
        override val durationMs: Long = 0
    ) : CapsuleEvent
}

