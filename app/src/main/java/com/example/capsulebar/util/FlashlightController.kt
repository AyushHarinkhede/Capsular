package com.example.capsulebar.util

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FlashlightController {
    var isFlashlightOn: Boolean = false
        private set

    private val _intensityFlow = MutableStateFlow(0.5f)
    val intensityFlow: StateFlow<Float> = _intensityFlow.asStateFlow()

    fun turnOn(context: Context, intensity: Float) {
        val coerced = intensity.coerceIn(0.1f, 1.0f)
        _intensityFlow.value = coerced
        isFlashlightOn = true
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: "0"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val maxLevel = characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                if (maxLevel > 1) {
                    val strength = (coerced * maxLevel).toInt().coerceIn(1, maxLevel)
                    cameraManager.turnOnTorchWithStrengthLevel(cameraId, strength)
                    return
                }
            }
            // Fallback
            cameraManager.setTorchMode(cameraId, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun turnOff(context: Context) {
        isFlashlightOn = false
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: "0"
            cameraManager.setTorchMode(cameraId, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setIntensity(context: Context, intensity: Float) {
        val coerced = intensity.coerceIn(0.1f, 1.0f)
        _intensityFlow.value = coerced
        if (isFlashlightOn) {
            turnOn(context, coerced)
        }
    }
}
