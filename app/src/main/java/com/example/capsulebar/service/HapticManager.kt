package com.example.capsulebar.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.capsulebar.data.CapsuleSettings

/**
 * HapticManager — Material You & Google Pixel grade tactile haptics for Capsule Bar.
 * Utilizes VibrationEffect.Composition primitives on Android 12+ (API 31+),
 * falling back gracefully to predefined effects and tuned micro-waveforms.
 */
object HapticManager {

    private var vibrator: Vibrator? = null
    private var settings: CapsuleSettings? = null

    fun init(context: Context) {
        val appCtx = context.applicationContext
        settings = CapsuleSettings(appCtx)
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

    /** Clean tactile click (buttons, cards, selections) */
    fun vibrateClick() {
        if (!isEnabled()) return
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val comp = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.75f)
                v.vibrate(comp.compose())
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(25, 90))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(25)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Subtle micro tick (slider steps, stepper notches, calibration) */
    fun vibrateTick() {
        if (!isEnabled()) return
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val comp = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.45f)
                v.vibrate(comp.compose())
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(12, 50))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(12)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Snappy punchy tick when notch capsule is directly touched/tapped */
    fun vibrateTap() {
        if (!isEnabled()) return
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val comp = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.85f)
                v.vibrate(comp.compose())
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(30, 120))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(30)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Sensory micro-swell when capsule or a new event appears at the notch */
    fun vibrateAppear() {
        if (!isEnabled()) return
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val comp = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.5f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.7f, 25)
                v.vibrate(comp.compose())
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 18, 20, 25)
                val amplitudes = intArrayOf(0, 60, 0, 130)
                v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(35)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Organic expansion bloom into full card */
    fun vibrateExpand() {
        if (!isEnabled()) return
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val comp = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.6f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.85f, 20)
                v.vibrate(comp.compose())
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(45, 140))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(45)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Springy snap tick when collapsing back into punch hole */
    fun vibrateCollapse() {
        if (!isEnabled()) return
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val comp = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.7f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.4f, 15)
                v.vibrate(comp.compose())
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(20, 75))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(20)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Tactile switch toggle: Rising pop on enable, subtle drop on disable */
    fun vibrateToggle(isOn: Boolean) {
        if (!isEnabled()) return
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val comp = VibrationEffect.startComposition()
                if (isOn) {
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f)
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.9f, 30)
                } else {
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.65f)
                }
                v.vibrate(comp.compose())
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isOn) {
                    val timings = longArrayOf(0, 15, 25, 22)
                    val amplitudes = intArrayOf(0, 80, 0, 160)
                    v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    v.vibrate(VibrationEffect.createOneShot(18, 65))
                }
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(if (isOn) 35 else 18)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Rhythmic attention pulse for incoming calls */
    fun vibrateIncomingCall() {
        if (!isEnabled()) return
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val comp = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.8f, 50)
                v.vibrate(comp.compose())
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 30, 40, 50)
                val amplitudes = intArrayOf(0, 120, 0, 180)
                v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(60)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Firm, heavy bump — for long-press, swipe-to-dismiss, destructive actions */
    fun vibrateHeavy() {
        if (!isEnabled()) return
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val comp = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.9f)
                v.vibrate(comp.compose())
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(55, 190))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(55)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Double pulse — service start/stop, success confirmation */
    fun vibrateDouble() {
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 30, 60, 35)
                val amplitudes = intArrayOf(0, 110, 0, 180)
                v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(longArrayOf(0, 30, 60, 35), -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Triple tick — clear all / dismiss */
    fun vibrateTripleTick() {
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 18, 35, 18, 35, 18)
                val amplitudes = intArrayOf(0, 70, 0, 70, 0, 70)
                v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(longArrayOf(0, 18, 35, 18, 35, 18), -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancel() {
        vibrator?.cancel()
    }
}
