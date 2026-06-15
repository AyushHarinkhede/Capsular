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

    fun init(context: Context) {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /** Short, light click — for toggles, button presses, expand/collapse tap. */
    fun vibrateClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(30, 80))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(30)
        }
    }

    /** Very light, quick tick — for slider steps, new event arrives. */
    fun vibrateTick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(15, 50))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(15)
        }
    }

    /** Firm, heavy bump — for long-press, swipe-to-hide, destructive actions. */
    fun vibrateHeavy() {
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
