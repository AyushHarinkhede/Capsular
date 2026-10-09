package com.example.capsulebar.service

import android.content.Context
import android.media.AudioManager
import com.example.capsulebar.data.CapsuleSettings

/**
 * SoundManager — crisp, tactile audio feedback for Capsule Bar.
 * Respects silent/vibrate ringer mode automatically.
 */
object SoundManager {
    private var audioManager: AudioManager? = null
    private var settings: CapsuleSettings? = null

    fun init(context: Context) {
        val appCtx = context.applicationContext
        audioManager = appCtx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        settings = CapsuleSettings(appCtx)
    }

    private fun canPlay(): Boolean {
        val am = audioManager ?: return false
        val cfg = settings ?: return false
        if (!cfg.soundEffectsEnabled) return false

        // Automatically respect phone's silent or vibrate mode
        val ringerMode = am.ringerMode
        return ringerMode == AudioManager.RINGER_MODE_NORMAL
    }

    /** Plays standard tactile click (button press, card click) */
    fun playClick() {
        if (!canPlay()) return
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.40f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Plays tactile micro-tick (slider steps, calibration, subtle feedback) */
    fun playTick() {
        if (!canPlay()) return
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR, 0.30f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Plays toggle on / off sound */
    fun playToggle(isOn: Boolean) {
        if (!canPlay()) return
        try {
            val effect = if (isOn) AudioManager.FX_KEYPRESS_RETURN else AudioManager.FX_KEYPRESS_DELETE
            audioManager?.playSoundEffect(effect, 0.50f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Plays pleasant expand popup sound */
    fun playExpand() {
        if (!canPlay()) return
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN, 0.60f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Plays gentle collapse / dismiss sound */
    fun playCollapse() {
        if (!canPlay()) return
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE, 0.35f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
