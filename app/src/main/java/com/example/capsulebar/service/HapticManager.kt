package com.example.capsulebar.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * HapticManager — premium haptic feedback for Capsule Bar.
 *
 * Usage:
 *   HapticManager.init(context)
 *   HapticManager.vibrateClick()    // light tap (toggle, button)
 *   HapticManager.vibrateTick()     // very light tick (new event arrives)
 *   HapticManager.vibrateHeavy()    // firm press (swipe to hide)
 *   HapticManager.vibrateDouble()   // double pulse (service start/stop)
 *   HapticManager.vibrateWave(pattern) // custom timing pattern
 */
object HapticManager {

    private var vibrator: Vibrator? = null
    private var settings: com.example.capsulebar.data.CapsuleSettings? = null

    fun init(context: Context) {
        val appCtx = context.applicationContext
        settings = com.example.capsulebar.data.CapsuleSettings(appCtx)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = appCtx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appCtx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun isEnabled(): Boolean {
        return settings?.hapticsEnabled != false
    }

    /** Short, light click — for buttons, card taps. */
    fun vibrateClick() {
        if (!isEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(30, 80))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(30)
        }
    }

    /** Very light, quick tick — for slider steps, calibration adjustments. */
    fun vibrateTick() {
        if (!isEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(12, 50))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(12)
        }
    }

    /** Tactile toggle on / off feedback */
    fun vibrateToggle(isOn: Boolean) {
        if (!isEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isOn) {
                // Energetic crisp double tap
                val timings = longArrayOf(0, 15, 30, 20)
                val amplitudes = intArrayOf(0, 100, 0, 180)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                // Soft falling tick
                vibrator?.vibrate(VibrationEffect.createOneShot(18, 70))
            }
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(if (isOn) 35 else 20)
        }
    }

    /** Firm, heavy bump — for long-press, swipe-to-hide, destructive actions. */
    fun vibrateHeavy() {
        if (!isEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(60, 200))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(60)
        }
    }

    /** Double pulse — for service start/stop, clear-all confirmation. */
    fun vibrateDouble() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Two pulses: on, off, on — at 60% and 100% amplitude
            val timings = longArrayOf(0, 35, 70, 35)
            val amplitudes = intArrayOf(0, 120, 0, 200)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 35, 70, 35), -1)
        }
    }

    /** Triple tick — for event dismissed / auto-cleared. */
    fun vibrateTripleTick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 20, 40, 20, 40, 20)
            val amplitudes = intArrayOf(0, 80, 0, 80, 0, 80)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 20, 40, 20, 40, 20), -1)
        }
    }

    /** Custom wave pattern. timings in ms, amplitudes 0-255, repeat=-1 for no repeat. */
    fun vibrateWave(timings: LongArray, amplitudes: IntArray, repeat: Int = -1) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, repeat))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(timings, repeat)
        }
    }

    fun cancel() {
        vibrator?.cancel()
    }
}
