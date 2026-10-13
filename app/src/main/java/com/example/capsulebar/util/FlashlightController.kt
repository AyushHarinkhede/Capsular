package com.example.capsulebar.util

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FlashlightController {
    var isFlashlightOn: Boolean = false
        private set

    private val _intensityFlow = MutableStateFlow(0.5f)
    val intensityFlow: StateFlow<Float> = _intensityFlow.asStateFlow()

    private val _isSosActiveFlow = MutableStateFlow(false)
    val isSosActiveFlow: StateFlow<Boolean> = _isSosActiveFlow.asStateFlow()

    private var sosJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun turnOn(context: Context, intensity: Float) {
        stopSos()
        val coerced = intensity.coerceIn(0.1f, 1.0f)
        _intensityFlow.value = coerced
        isFlashlightOn = true
        applyTorchHardware(context, true, coerced)
    }

    fun turnOff(context: Context) {
        stopSos()
        isFlashlightOn = false
        applyTorchHardware(context, false, 0f)
    }

    fun setIntensity(context: Context, intensity: Float) {
        val coerced = intensity.coerceIn(0.1f, 1.0f)
        _intensityFlow.value = coerced
        if (isFlashlightOn && !_isSosActiveFlow.value) {
            applyTorchHardware(context, true, coerced)
        }
    }

    fun toggleSos(context: Context) {
        if (_isSosActiveFlow.value) {
            stopSos()
            if (isFlashlightOn) {
                turnOn(context, _intensityFlow.value)
            } else {
                turnOff(context)
            }
        } else {
            startSos(context)
        }
    }

    private fun startSos(context: Context) {
        stopSos()
        _isSosActiveFlow.value = true
        isFlashlightOn = true
        sosJob = scope.launch {
            val dot = 150L
            val dash = 450L
            val gap = 150L
            while (isActive) {
                repeat(3) {
                    applyTorchHardware(context, true, 1.0f)
                    delay(dot)
                    applyTorchHardware(context, false, 0f)
                    delay(gap)
                }
                delay(300L)
                repeat(3) {
                    applyTorchHardware(context, true, 1.0f)
                    delay(dash)
                    applyTorchHardware(context, false, 0f)
                    delay(gap)
                }
                delay(300L)
                repeat(3) {
                    applyTorchHardware(context, true, 1.0f)
                    delay(dot)
                    applyTorchHardware(context, false, 0f)
                    delay(gap)
                }
                delay(1200L)
            }
        }
    }

    private fun stopSos() {
        sosJob?.cancel()
        sosJob = null
        _isSosActiveFlow.value = false
    }

    private fun applyTorchHardware(context: Context, enable: Boolean, intensity: Float) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: "0"

            if (enable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    val maxLevel = characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                    if (maxLevel > 1) {
                        val strength = (intensity * maxLevel).toInt().coerceIn(1, maxLevel)
                        cameraManager.turnOnTorchWithStrengthLevel(cameraId, strength)
                        return
                    }
                }
                cameraManager.setTorchMode(cameraId, true)
            } else {
                cameraManager.setTorchMode(cameraId, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
