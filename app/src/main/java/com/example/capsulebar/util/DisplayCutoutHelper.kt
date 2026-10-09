package com.example.capsulebar.util

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.DisplayCutout

data class CutoutCalibrationResult(
    val position: String, // "Center", "Left", "Right"
    val xOffsetPx: Int,
    val yOffsetPx: Int,
    val cameraWidthDp: Int,
    val description: String
)

object DisplayCutoutHelper {

    /**
     * Inspects the device's hardware WindowInsets / DisplayCutout (Android 9+ / API 28+)
     * and calculates exact pixel alignment for the camera punch hole.
     */
    fun detectCutout(activity: Activity): CutoutCalibrationResult? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null

        val insets = activity.window.decorView.rootWindowInsets ?: return null
        val cutout: DisplayCutout = insets.displayCutout ?: return null
        val rects = cutout.boundingRects
        if (rects.isEmpty()) return null

        // Locate top cutout bounding rect (typical front camera punch-hole / notch)
        val rect: Rect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cutout.boundingRectTop.takeIf { !it.isEmpty } ?: rects.first()
        } else {
            rects.first()
        }

        if (rect.isEmpty) return null

        val metrics = activity.resources.displayMetrics
        val density = metrics.density
        val screenWidthPx = metrics.widthPixels

        val cutoutCenterX = rect.centerX().toFloat()
        val cutoutWidthPx = rect.width().toFloat()
        val cutoutHeightPx = rect.height().toFloat()

        val cameraWidthDp = ((cutoutWidthPx / density).toInt()).coerceIn(18, 64)

        val oneThird = screenWidthPx / 3f
        val twoThirds = screenWidthPx * 2f / 3f

        val position: String
        val xOffsetPx: Int

        if (cutoutCenterX < oneThird) {
            position = "Left"
            val baseCenterXPx = ((cameraWidthDp / 2f + 16f) * density)
            xOffsetPx = (cutoutCenterX - baseCenterXPx).toInt()
        } else if (cutoutCenterX > twoThirds) {
            position = "Right"
            val baseCenterXPx = screenWidthPx - ((cameraWidthDp / 2f + 16f) * density)
            xOffsetPx = (cutoutCenterX - baseCenterXPx).toInt()
        } else {
            position = "Center"
            val baseCenterXPx = screenWidthPx / 2f
            xOffsetPx = (cutoutCenterX - baseCenterXPx).toInt()
        }

        // Vertical offset: rect.top or center vertically
        val yOffsetPx = rect.top.coerceAtLeast(0)

        return CutoutCalibrationResult(
            position = position,
            xOffsetPx = xOffsetPx,
            yOffsetPx = yOffsetPx,
            cameraWidthDp = cameraWidthDp,
            description = "Cutout detected: $position at X:${xOffsetPx}px, Y:${yOffsetPx}px (${cameraWidthDp}dp)"
        )
    }
}
