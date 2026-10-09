package com.example.capsulebar.service

/**
 * HapticSoundManager — unified dispatcher coordinating tactile haptics
 * and authentic Android/Material You audio cues across the app and capsule.
 */
object HapticSoundManager {

    /** Button taps, card clicks, option selections */
    fun playClick() {
        HapticManager.vibrateClick()
        SoundManager.playClick()
    }

    /** Sliders, steppers, calibration notches */
    fun playTick() {
        HapticManager.vibrateTick()
        SoundManager.playTick()
    }

    /** Switch toggle transitions (ON / OFF) */
    fun playToggle(isOn: Boolean) {
        HapticManager.vibrateToggle(isOn)
        SoundManager.playToggle(isOn)
    }

    /** Capsule or new event appears at the punch-hole notch */
    fun playCapsuleAppear() {
        HapticManager.vibrateAppear()
        SoundManager.playAppear()
    }

    /** Direct tap on the capsule pill or split circle */
    fun playCapsuleTap() {
        HapticManager.vibrateTap()
        SoundManager.playClick()
    }

    /** Expansion from notch pill into rich card */
    fun playExpand() {
        HapticManager.vibrateExpand()
        SoundManager.playExpand()
    }

    /** Collapse from card back into notch pill */
    fun playCollapse() {
        HapticManager.vibrateCollapse()
        SoundManager.playCollapse()
    }

    /** Rhythmic alert for incoming phone calls */
    fun playIncomingCall() {
        HapticManager.vibrateIncomingCall()
        SoundManager.playCallAlert()
    }

    /** Destructive / long-press actions */
    fun playHeavy() {
        HapticManager.vibrateHeavy()
        SoundManager.playClick()
    }

    /** Service started or confirmation pulse */
    fun playSuccess() {
        HapticManager.vibrateDouble()
        SoundManager.playSuccess()
    }
}
