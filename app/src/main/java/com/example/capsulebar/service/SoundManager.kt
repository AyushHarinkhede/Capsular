package com.example.capsulebar.service

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import com.example.capsulebar.data.CapsuleSettings

/**
 * SoundManager — authentic Material You and Android system audio feedback.
 * Uses native AudioManager system sound effects and tuned acoustic cues.
 * Automatically respects device silent/vibrate ringer mode.
 */
object SoundManager {
    private var audioManager: AudioManager? = null
    private var settings: CapsuleSettings? = null
    private var toneGenerator: ToneGenerator? = null

    fun init(context: Context) {
        val appCtx = context.applicationContext
        audioManager = appCtx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        settings = CapsuleSettings(appCtx)
        try {
            // Low volume (30%) tone generator for subtle Pixel-style ambient cues
            toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 30)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun canPlay(): Boolean {
        val am = audioManager ?: return false
        val cfg = settings ?: return false
        if (!cfg.soundEffectsEnabled) return false

        // Respect silent and vibrate mode
        return am.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }

    /** Standard crisp tactile click (buttons, cards, selections) */
    fun playClick() {
        if (!canPlay()) return
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.45f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Micro tick cue (sliders, calibration, stepper) */
    fun playTick() {
        if (!canPlay()) return
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR, 0.30f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Toggle switch ON / OFF sound */
    fun playToggle(isOn: Boolean) {
        if (!canPlay()) return
        try {
            val effect = if (isOn) AudioManager.FX_KEYPRESS_RETURN else AudioManager.FX_KEYPRESS_DELETE
            val vol = if (isOn) 0.55f else 0.40f
            audioManager?.playSoundEffect(effect, vol)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Island or event appear cue at top notch */
    fun playAppear() {
        if (!canPlay()) return
        try {
            audioManager?.playSoundEffect(AudioManager.FX_FOCUS_NAVIGATION_UP, 0.40f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Capsule expand bloom sound */
    fun playExpand() {
        if (!canPlay()) return
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN, 0.60f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Capsule collapse snap sound */
    fun playCollapse() {
        if (!canPlay()) return
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE, 0.35f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Ambient chime for incoming call or urgent live activity */
    fun playCallAlert() {
        if (!canPlay()) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
        } catch (e: Exception) {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN, 0.70f)
        }
    }

    /** Confirmation success chime */
    fun playSuccess() {
        if (!canPlay()) return
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN, 0.65f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
