package com.example.capsulebar.service

/**
 * HapticSoundManager — unified dispatcher for tactile haptics + crisp audio cues.
 */
object HapticSoundManager {

    /** Button taps, icon clicks, card selection */
    fun playClick() {
        HapticManager.vibrateClick()
        SoundManager.playClick()
    }

    /** Sliders, stepper detents, calibration micro adjustments */
    fun playTick() {
        HapticManager.vibrateTick()
        SoundManager.playTick()
    }

    /** Toggle switches (ON / OFF) */
    fun playToggle(isOn: Boolean) {
        HapticManager.vibrateToggle(isOn)
        SoundManager.playToggle(isOn)
    }

    /** Capsule expansion / bloom into card */
    fun playExpand() {
        HapticManager.vibrateHeavy()
        SoundManager.playExpand()
    }

    /** Capsule collapse / dismiss back into pill */
    fun playCollapse() {
        HapticManager.vibrateTick()
        SoundManager.playCollapse()
    }

    /** Destructive / heavy action */
    fun playHeavy() {
        HapticManager.vibrateHeavy()
        SoundManager.playClick()
    }

    /** Service started or confirmation pulse */
    fun playSuccess() {
        HapticManager.vibrateDouble()
        SoundManager.playExpand()
    }
}
