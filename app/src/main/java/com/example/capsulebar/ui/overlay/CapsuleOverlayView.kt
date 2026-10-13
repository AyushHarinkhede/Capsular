package com.example.capsulebar.ui.overlay

import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import com.example.capsulebar.data.*
import com.example.capsulebar.data.DisplayMode
import com.example.capsulebar.util.FlashlightController

fun getCapsuleColor(
    event: CapsuleEvent?,
    defaultColorVal: Int,
    autoColor: Boolean,
    useAppColors: Boolean,
    context: android.content.Context
): Color {
    if (event != null && useAppColors) {
        val packageName = when (event) {
            is CapsuleEvent.Music -> event.packageName
            is CapsuleEvent.Notification -> event.packageName
            else -> ""
        }
        if (packageName.isNotEmpty()) {
            val fallbackColor = when {
                packageName.contains("spotify") -> Color(0xFF1DB954)
                packageName.contains("youtube") || packageName.contains("ytm") -> Color(0xFFFF0000)
                packageName.contains("whatsapp") -> Color(0xFF25D366)
                packageName.contains("telegram") -> Color(0xFF0088CC)
                packageName.contains("instagram") -> Color(0xFFE1306C)
                packageName.contains("facebook") -> Color(0xFF1877F2)
                packageName.contains("twitter") || packageName.contains("x") -> Color.Black
                packageName.contains("gmail") -> Color(0xFFEA4335)
                packageName.contains("chrome") -> Color(0xFF4285F4)
                else -> null
            }
            if (fallbackColor != null) return fallbackColor
        }
    }
    
    if (event != null && autoColor) {
        return when (event) {
            is CapsuleEvent.Battery -> if (event.isCharging) Color(0xFF2E7D32) else if (event.isLow) Color(0xFFC62828) else Color(0xFF37474F)
            is CapsuleEvent.Bluetooth -> Color(0xFF1565C0)
            is CapsuleEvent.Music -> Color(0xFF6A1B9A)
            is CapsuleEvent.Network -> Color(0xFF00838F)
            is CapsuleEvent.SoundProfile -> Color(0xFFAD1457)
            is CapsuleEvent.USB -> Color(0xFF2E7D32)
            is CapsuleEvent.LockState -> if (event.isLocked) Color(0xFF000000) else Color(0xFF0E1C11)
            is CapsuleEvent.Timer -> Color(0xFFE65100)
            is CapsuleEvent.Navigation -> Color(0xFF0277BD)
            is CapsuleEvent.Progress -> Color(0xFF00695C)
            is CapsuleEvent.Notification -> Color(0xFF37474F)
            is CapsuleEvent.Authentication -> if (event.success) Color(0xFF2E7D32) else Color(0xFFC62828)
            is CapsuleEvent.Call -> Color(0xFF2E7D32)
            is CapsuleEvent.Recording -> Color(0xFFC62828)
            is CapsuleEvent.Hotspot -> Color(0xFF1565C0)
            is CapsuleEvent.Delivery -> Color(0xFFF9A825)
            is CapsuleEvent.SystemToggle -> if (event.isEnabled) Color(0xFF2E7D32) else Color(0xFF37474F)
            is CapsuleEvent.Stopwatch -> Color(0xFF1565C0)
            is CapsuleEvent.HourlyTracker -> Color(0xFF2E7D32)
            is CapsuleEvent.CalendarEvent -> Color(0xFF1565C0)
            is CapsuleEvent.Weather -> Color(0xFF00838F)
            is CapsuleEvent.Alarm -> Color(0xFFE65100)
        }
    }

    return Color(defaultColorVal)
}

@Composable
fun CapsuleOverlayScreen(settings: CapsuleSettings) {
    val uiState by CapsuleStateManager.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Reactive Compose states for settings
    var xOffset by remember { mutableStateOf(settings.xOffset) }
    var yOffset by remember { mutableStateOf(settings.yOffset) }
    var widthDp by remember { mutableStateOf(settings.widthDp) }
    var heightDp by remember { mutableStateOf(settings.heightDp) }
    var cornerRadiusDp by remember { mutableStateOf(settings.cornerRadiusDp) }
    var isCalibrationMode by remember { mutableStateOf(settings.isCalibrationMode) }
    var bluetoothImagePath by remember { mutableStateOf(settings.bluetoothImagePath) }
    var cameraPosition by remember { mutableStateOf(settings.cameraPosition) }
    var cameraWidthDp by remember { mutableStateOf(settings.cameraWidthDp) }
    var edgeRoundingPercent by remember { mutableStateOf(settings.edgeRoundingPercent) }
    var defaultColor by remember { mutableStateOf(settings.defaultColor) }
    var autoColor by remember { mutableStateOf(settings.autoColor) }
    var useAppColors by remember { mutableStateOf(settings.useAppColors) }
    var maxPopupWidthPercent by remember { mutableStateOf(settings.maxPopupWidthPercent) }
    var quickAnimations by remember { mutableStateOf(settings.quickAnimations) }
    var premiumAnimations by remember { mutableStateOf(settings.premiumAnimations) }
    var splitPosition by remember { mutableStateOf(settings.splitPosition) } // "Left" or "Right"
    var showAsNotch by remember { mutableStateOf(settings.showAsNotch) }
    var addBackground by remember { mutableStateOf(settings.addBackground) }
    var showImages by remember { mutableStateOf(settings.showImages) }
    var maxTextLines by remember { mutableStateOf(settings.maxTextLines) }
    var showMusicVisualizer by remember { mutableStateOf(settings.showMusicVisualizer) }
    var useAndroidMusicControls by remember { mutableStateOf(settings.useAndroidMusicControls) }
    var iconOption by remember { mutableStateOf(settings.iconOption) }
    var sendReplies by remember { mutableStateOf(settings.sendReplies) }
    var showAlways by remember { mutableStateOf(settings.showAlways) }
    var quickAccessApps by remember { mutableStateOf(settings.quickAccessApps) }
    var isPositionLocked by remember { mutableStateOf(settings.isPositionLocked) }

    DisposableEffect(settings) {
        val prefs = context.getSharedPreferences("capsule_settings", Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "x_offset" -> xOffset = settings.xOffset
                "y_offset" -> yOffset = settings.yOffset
                "width_dp" -> widthDp = settings.widthDp
                "height_dp" -> heightDp = settings.heightDp
                "corner_radius_dp" -> cornerRadiusDp = settings.cornerRadiusDp
                "is_calibration_mode" -> isCalibrationMode = settings.isCalibrationMode
                "is_position_locked" -> isPositionLocked = settings.isPositionLocked
                "bluetooth_image_path" -> bluetoothImagePath = settings.bluetoothImagePath
                "camera_position" -> cameraPosition = settings.cameraPosition
                "camera_width_dp" -> cameraWidthDp = settings.cameraWidthDp
                "edge_rounding_percent" -> edgeRoundingPercent = settings.edgeRoundingPercent
                "default_color" -> defaultColor = settings.defaultColor
                "auto_color" -> autoColor = settings.autoColor
                "use_app_colors" -> useAppColors = settings.useAppColors
                "max_popup_width_percent" -> maxPopupWidthPercent = settings.maxPopupWidthPercent
                "quick_animations" -> quickAnimations = settings.quickAnimations
                "premium_animations" -> premiumAnimations = settings.premiumAnimations
                "split_position" -> splitPosition = settings.splitPosition
                "show_as_notch" -> showAsNotch = settings.showAsNotch
                "add_background" -> addBackground = settings.addBackground
                "show_images" -> showImages = settings.showImages
                "max_text_lines" -> maxTextLines = settings.maxTextLines
                "show_music_visualizer" -> showMusicVisualizer = settings.showMusicVisualizer
                "use_android_music_controls" -> useAndroidMusicControls = settings.useAndroidMusicControls
                "icon_option" -> iconOption = settings.iconOption
                "send_replies" -> sendReplies = settings.sendReplies
                "show_always" -> showAlways = settings.showAlways
                "quick_access_apps" -> quickAccessApps = settings.quickAccessApps
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    if (isCalibrationMode) {
        CalibrationScreen(
            xOffset = xOffset,
            yOffset = yOffset,
            widthDp = widthDp,
            heightDp = heightDp,
            edgeRoundingPercent = edgeRoundingPercent,
            cameraPosition = cameraPosition,
            cameraWidthDp = cameraWidthDp,
            splitPosition = splitPosition,
            showAsNotch = showAsNotch,
            isPositionLocked = isPositionLocked
        )
        return
    }

    val density = LocalDensity.current.density
    val screenWidthFloatDp = LocalConfiguration.current.screenWidthDp.toFloat()

    // ── UNIFIED CAPSULE AREA ANCHOR COORDINATES ──
    val baseCameraCenterXDp = when (cameraPosition) {
        "Left"  -> (cameraWidthDp / 2f + 16f)
        "Right" -> (screenWidthFloatDp - (cameraWidthDp / 2f + 16f))
        else    -> screenWidthFloatDp / 2f
    }
    val minAnchorX = (cameraWidthDp / 2f + 4f)
    val maxAnchorX = (screenWidthFloatDp - cameraWidthDp / 2f - 4f).coerceAtLeast(minAnchorX)
    val cameraCenterXDp = (baseCameraCenterXDp + (xOffset / density)).coerceIn(minAnchorX, maxAnchorX)
    val topOffsetDp = if (showAsNotch) 0f else (yOffset / density).coerceAtLeast(0f)

    val springBouncy = remember(quickAnimations, premiumAnimations) {
        spring<Float>(
            dampingRatio = if (quickAnimations) 0.50f else if (premiumAnimations) 0.42f else 0.44f,
            stiffness = if (quickAnimations) 580f else if (premiumAnimations) 320f else 390f
        )
    }

    // ── IDLE / RESTING STATE & UNIFIED DIMENSIONS ──
    val isIdleState = uiState.mainEvent == null
    val idlePunchHoleWidth = if (showAlways) widthDp.coerceAtLeast(heightDp).toFloat() else (cameraWidthDp + 6f).coerceAtLeast(heightDp.toFloat())
    val activeCollapsedWidth = widthDp.coerceAtLeast(heightDp * 2).toFloat()
    val expandedWidthDp = (screenWidthFloatDp * 0.94f * (maxPopupWidthPercent / 100f)).coerceAtLeast(280f)
    val extraHeaderDp = (heightDp - 36).coerceAtLeast(0).toFloat()

    val expandedHeightDp = extraHeaderDp + when (val ev = uiState.mainEvent) {
        is CapsuleEvent.Music         -> 295f
        is CapsuleEvent.Call          -> 230f
        is CapsuleEvent.Recording     -> 210f
        is CapsuleEvent.Timer         -> 265f
        is CapsuleEvent.Stopwatch     -> 265f
        is CapsuleEvent.Notification  -> 255f
        is CapsuleEvent.Progress      -> 210f
        is CapsuleEvent.Navigation    -> 220f
        is CapsuleEvent.SystemToggle  -> if (ev.name.lowercase() == "flashlight") 260f else 225f
        is CapsuleEvent.HourlyTracker -> 220f
        is CapsuleEvent.CalendarEvent -> 210f
        is CapsuleEvent.Weather       -> 260f
        is CapsuleEvent.Alarm         -> 220f
        is CapsuleEvent.Battery       -> 230f
        is CapsuleEvent.Bluetooth     -> 220f
        is CapsuleEvent.Network       -> 240f
        is CapsuleEvent.SoundProfile  -> 265f
        is CapsuleEvent.USB           -> 240f
        is CapsuleEvent.LockState     -> 210f
        is CapsuleEvent.Hotspot       -> 230f
        else                          -> 230f
    }

    val collapsedRadiusDp = (heightDp.toFloat() / 2f) * (edgeRoundingPercent / 60f).coerceIn(0.15f, 1f)

    // Target width: punch-hole circle -> expands symmetrically out to collapsed width -> card
    val leftWidthTarget = when (uiState.displayMode) {
        DisplayMode.COLLAPSED -> if (isIdleState) idlePunchHoleWidth else activeCollapsedWidth
        DisplayMode.EXPANDED  -> expandedWidthDp
        DisplayMode.SPLIT     -> activeCollapsedWidth
        DisplayMode.HIDDEN    -> idlePunchHoleWidth
    }
    val leftHeightTarget = when (uiState.displayMode) {
        DisplayMode.COLLAPSED -> heightDp.toFloat()
        DisplayMode.EXPANDED  -> expandedHeightDp
        DisplayMode.SPLIT     -> heightDp.toFloat()
        DisplayMode.HIDDEN    -> heightDp.toFloat()
    }
    val leftRadiusTarget = when (uiState.displayMode) {
        DisplayMode.COLLAPSED -> collapsedRadiusDp
        DisplayMode.EXPANDED  -> 28f
        DisplayMode.SPLIT     -> collapsedRadiusDp
        DisplayMode.HIDDEN    -> collapsedRadiusDp
    }

    val isVisible = (!uiState.isHidden && uiState.mainEvent != null) || showAlways

    val leftWidth by animateFloatAsState(
        targetValue = if (!isVisible && !showAlways) idlePunchHoleWidth else leftWidthTarget,
        animationSpec = springBouncy,
        label = "leftWidth"
    )
    val leftHeight by animateFloatAsState(
        targetValue = if (!isVisible && !showAlways) heightDp.toFloat() else leftHeightTarget,
        animationSpec = springBouncy,
        label = "leftHeight"
    )
    val leftRadius by animateFloatAsState(targetValue = leftRadiusTarget, animationSpec = springBouncy, label = "leftRadius")

    val capsuleAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = if (isVisible) {
            tween(durationMillis = 160)
        } else {
            // Stay fully visible while liquid-retracting into punch-hole, fade only in final 200ms
            tween(durationMillis = 200, delayMillis = 180)
        },
        label = "capsuleAlpha"
    )

    // Dynamic Island scale bounce: pop on emerge from notch, squeeze on retract into notch
    val scaleAnim = remember { Animatable(1f) }
    val prevEventId = remember { mutableStateOf<String?>(null) }
    val prevVisible = remember { mutableStateOf(isVisible) }

    LaunchedEffect(uiState.mainEvent?.id, isVisible) {
        val newId = uiState.mainEvent?.id
        if (isVisible && !prevVisible.value) {
            // Emerge from punch-hole: rapid pop and bouncy overshoot
            scaleAnim.snapTo(0.78f)
            scaleAnim.animateTo(
                targetValue = 1.06f,
                animationSpec = spring(dampingRatio = 0.40f, stiffness = 520f)
            )
            scaleAnim.animateTo(
                targetValue = 1.00f,
                animationSpec = spring(dampingRatio = 0.44f, stiffness = 380f)
            )
        } else if (newId != null && newId != prevEventId.value) {
            // New event arrival bounce
            scaleAnim.animateTo(
                targetValue = 1.07f,
                animationSpec = spring(dampingRatio = 0.40f, stiffness = 600f)
            )
            scaleAnim.animateTo(
                targetValue = 1.00f,
                animationSpec = spring(dampingRatio = 0.44f, stiffness = 380f)
            )
        } else if (!isVisible && prevVisible.value) {
            // Retract Anticipation Bounce into notch
            scaleAnim.animateTo(
                targetValue = 1.02f,
                animationSpec = tween(durationMillis = 40)
            )
            scaleAnim.animateTo(
                targetValue = 0.90f,
                animationSpec = spring(dampingRatio = 0.48f, stiffness = 420f)
            )
            scaleAnim.animateTo(
                targetValue = 1.00f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = 350f)
            )
        }
        prevEventId.value = newId
        prevVisible.value = isVisible
    }

    // Split circle animation
    val isSplit = uiState.displayMode == DisplayMode.SPLIT && uiState.splitEvent != null
    val splitTargetSize = if (isSplit) heightDp.toFloat() else 0f
    val splitSize by animateFloatAsState(targetValue = splitTargetSize, animationSpec = springBouncy, label = "splitSize")
    val splitGap = 8f

    // ── UNIFIED SAME-AREA ANCHOR COMPUTATIONS ──
    val collapsedBaseLeftDp = when (cameraPosition) {
        "Left" -> (cameraCenterXDp - (cameraWidthDp / 2f) - 4f).coerceIn(2f, (screenWidthFloatDp - activeCollapsedWidth - 2f).coerceAtLeast(2f))
        "Right" -> (cameraCenterXDp + (cameraWidthDp / 2f) + 4f - activeCollapsedWidth).coerceIn(2f, (screenWidthFloatDp - activeCollapsedWidth - 2f).coerceAtLeast(2f))
        else -> (cameraCenterXDp - (activeCollapsedWidth / 2f)).coerceIn(2f, (screenWidthFloatDp - activeCollapsedWidth - 2f).coerceAtLeast(2f))
    }
    val capsuleCenterXDp = collapsedBaseLeftDp + (activeCollapsedWidth / 2f)

    val pillLeftDp = when {
        uiState.displayMode == DisplayMode.EXPANDED -> {
            val minMargin = ((screenWidthFloatDp - leftWidth) / 2f).coerceIn(0f, 6f)
            val maxLeft = (screenWidthFloatDp - leftWidth - minMargin).coerceAtLeast(minMargin)
            (capsuleCenterXDp - (leftWidth / 2f)).coerceIn(minMargin, maxLeft)
        }
        cameraPosition == "Left" -> {
            val leftMargin = 4f
            (cameraCenterXDp - (cameraWidthDp / 2f) - leftMargin).coerceIn(2f, (screenWidthFloatDp - leftWidth - 2f).coerceAtLeast(2f))
        }
        cameraPosition == "Right" -> {
            val rightMargin = 4f
            (cameraCenterXDp + (cameraWidthDp / 2f) + rightMargin - leftWidth).coerceIn(2f, (screenWidthFloatDp - leftWidth - 2f).coerceAtLeast(2f))
        }
        else -> {
            (cameraCenterXDp - (leftWidth / 2f)).coerceIn(2f, (screenWidthFloatDp - leftWidth - 2f).coerceAtLeast(2f))
        }
    }

    // Secondary split circle stays right next to the main capsule in the exact same area (flips side automatically if near screen edge)
    val rightSplitCandidate = pillLeftDp + leftWidth + splitGap
    val leftSplitCandidate = pillLeftDp - splitGap - splitSize
    val splitLeftDp = if (splitPosition == "Right") {
        if (rightSplitCandidate + splitSize <= screenWidthFloatDp - 2f) rightSplitCandidate else leftSplitCandidate.coerceAtLeast(2f)
    } else {
        if (leftSplitCandidate >= 2f) leftSplitCandidate else rightSplitCandidate.coerceAtMost((screenWidthFloatDp - splitSize - 2f).coerceAtLeast(2f))
    }

    val pillShape = if (showAsNotch && uiState.displayMode != DisplayMode.EXPANDED) {
        RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = leftRadius.dp, bottomEnd = leftRadius.dp)
    } else {
        RoundedCornerShape(leftRadius.dp)
    }

    val scope = rememberCoroutineScope()
    var cutLineStart by remember { mutableStateOf<Offset?>(null) }
    var cutLineEnd by remember { mutableStateOf<Offset?>(null) }
    var isSlashing by remember { mutableStateOf(false) }
    var showCutHint by remember { mutableStateOf(false) }
    var hintJob by remember { mutableStateOf<Job?>(null) }

    val lockShakeX = remember { androidx.compose.animation.core.Animatable(0f) }
    val lockScale = remember { androidx.compose.animation.core.Animatable(1f) }

    val mainLockEvent = uiState.mainEvent as? CapsuleEvent.LockState
    val isUnlockEvent = mainLockEvent != null && !mainLockEvent.isLocked

    LaunchedEffect(mainLockEvent?.isLocked) {
        if (mainLockEvent != null && !mainLockEvent.isLocked) {
            // High-precision physical shake animation
            launch {
                lockShakeX.snapTo(0f)
                lockShakeX.animateTo(-8f, tween(35, easing = LinearEasing))
                lockShakeX.animateTo(8f, tween(40, easing = LinearEasing))
                lockShakeX.animateTo(-6f, tween(40, easing = LinearEasing))
                lockShakeX.animateTo(6f, tween(40, easing = LinearEasing))
                lockShakeX.animateTo(-3f, tween(35, easing = LinearEasing))
                lockShakeX.animateTo(3f, tween(35, easing = LinearEasing))
                lockShakeX.animateTo(0f, spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium))
            }
            // Dynamic Island unlock bounce
            launch {
                lockScale.snapTo(1f)
                lockScale.animateTo(1.08f, tween(70, easing = FastOutSlowInEasing))
                lockScale.animateTo(0.96f, tween(60, easing = FastOutSlowInEasing))
                lockScale.animateTo(1.0f, spring(dampingRatio = 0.5f, stiffness = 400f))
            }
        } else {
            lockShakeX.snapTo(0f)
            lockScale.snapTo(1f)
        }
    }

    if (!isVisible && capsuleAlpha == 0f) {
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (uiState.displayMode == DisplayMode.COLLAPSED) {
                    Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            if (dragAmount > 20f) {
                                com.example.capsulebar.service.CapsuleAccessibilityService.expandNotificationShade()
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        // Top status bar shield in EXPANDED or SPLIT mode: completely conceals system status bar & icons
        if (uiState.displayMode == DisplayMode.EXPANDED || uiState.displayMode == DisplayMode.SPLIT) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((heightDp + 2).dp)
                    .background(Color.Black)
            )
        }
        // Outside backdrop tap to collapse when expanded
        if (uiState.displayMode == DisplayMode.EXPANDED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        com.example.capsulebar.service.HapticSoundManager.playCollapse()
                        CapsuleStateManager.collapseToCompact()
                    }
            )
        }

        // ── SPLIT CIRCLE (DOUBLE CAPSULE IN SAME AREA) ──
        if (splitSize > 0.5f && uiState.splitEvent != null) {
            Box(
                modifier = Modifier
                    .offset(x = splitLeftDp.dp, y = topOffsetDp.dp)
                    .size(splitSize.dp)
                    .graphicsLayer {
                        alpha = capsuleAlpha
                    }
                    .clip(CircleShape)
                    .background(getCapsuleColor(uiState.splitEvent, defaultColor, autoColor, useAppColors, context))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .clickable {
                        com.example.capsulebar.service.HapticSoundManager.playExpand()
                        CapsuleStateManager.swapMainAndSplit(expandAfterSwap = true)
                    },
                contentAlignment = Alignment.Center
            ) {
                SplitContent(event = uiState.splitEvent!!)
            }
        }

        // ── MAIN PILL ──
        Box(
            modifier = Modifier
                .offset(x = pillLeftDp.dp, y = topOffsetDp.dp)
                .width(leftWidth.dp)
                .height(leftHeight.dp)
                .graphicsLayer {
                    alpha = capsuleAlpha
                    scaleX = scaleAnim.value * lockScale.value
                    scaleY = scaleAnim.value * lockScale.value
                    translationX = lockShakeX.value
                }
                .clip(pillShape)
                .background(
                    if (uiState.displayMode == DisplayMode.EXPANDED) {
                        Color(0xEE0B0B0B)
                    } else {
                        getCapsuleColor(uiState.mainEvent, defaultColor, autoColor, useAppColors, context)
                    }
                )
                .then(
                    if (uiState.displayMode == DisplayMode.EXPANDED) {
                        Modifier.border(1.dp, Color(0x22FFFFFF), pillShape)
                    } else if (isUnlockEvent) {
                        Modifier.border(1.2.dp, Color(0x6681C784), pillShape)
                    } else if (mainLockEvent != null && mainLockEvent.isLocked) {
                        Modifier.border(0.8.dp, Color(0x2AFFFFFF), pillShape)
                    } else {
                        Modifier
                    }
                )
                // ── ROCK-SOLID TAP TO EXPAND & DRAG-UP TO COLLAPSE ──
                .then(
                    if (uiState.displayMode != DisplayMode.EXPANDED) {
                        Modifier.clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            com.example.capsulebar.service.HapticSoundManager.playCapsuleTap()
                            com.example.capsulebar.service.HapticSoundManager.playExpand()
                            CapsuleStateManager.setDisplayMode(DisplayMode.EXPANDED)
                            hintJob?.cancel()
                            hintJob = scope.launch {
                                scaleAnim.animateTo(0.94f, tween(45))
                                scaleAnim.animateTo(1.03f, tween(55))
                                scaleAnim.animateTo(1.00f, spring(dampingRatio = 0.46f, stiffness = 400f))
                            }
                        }
                    } else {
                        Modifier
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                CapsuleStateManager.keepExpandedAlive()
                            }
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { change, dragAmount ->
                                    change.consume()
                                    if (dragAmount < -8f) {
                                        com.example.capsulebar.service.HapticSoundManager.playCollapse()
                                        CapsuleStateManager.collapseToCompact()
                                    }
                                }
                            }
                    }
                )
                .padding(horizontal = if (uiState.displayMode == DisplayMode.EXPANDED) 16.dp else 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.mainEvent != null) {
                // Content blooms smoothly once width has expanded past the camera punch hole
                val showContent = leftWidth > (cameraWidthDp + 8f) || uiState.displayMode == DisplayMode.EXPANDED
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(140)),
                    exit = fadeOut(animationSpec = tween(80))
                ) {
                    MainContent(
                        event = uiState.mainEvent!!,
                        displayMode = uiState.displayMode,
                        showCutHint = false,
                        cameraPosition = cameraPosition,
                        cameraWidthDp = cameraWidthDp,
                        heightDp = heightDp
                    )
                }
            } else if (uiState.displayMode == DisplayMode.EXPANDED) {
                QuickAccessAppsBar(context = context, haptic = haptic)
            }
        }
    }
}

@Composable
private fun MainContent(
    event: CapsuleEvent,
    displayMode: DisplayMode,
    showCutHint: Boolean,
    cameraPosition: String,
    cameraWidthDp: Int,
    heightDp: Int
) {
    Crossfade(
        targetState = displayMode,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "contentTransition"
    ) { mode ->
        if (mode == DisplayMode.EXPANDED) {
            ExpandedCard(
                event = event,
                cameraPosition = cameraPosition,
                cameraWidthDp = cameraWidthDp,
                heightDp = heightDp
            )
        } else {
            // Main capsule always shows its full collapsed content regardless of split mode.
            // Only the side SplitContent composable uses compact dot/icon style.
            CollapsedBubbleContent(
                event = event,
                isSplit = false,
                showCutHint = showCutHint,
                cameraPosition = cameraPosition,
                cameraWidthDp = cameraWidthDp
            )
        }
    }
}

// Material You Pastel Color Palette (Dynamic M3 mapped)
private val MaterialYouLavender: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary

private val MaterialYouPastelBlue: Color
    @Composable
    get() = MaterialTheme.colorScheme.secondary

private val MaterialYouSageGreen: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary

private val MaterialYouCoral: Color
    @Composable
    get() = MaterialTheme.colorScheme.error

private val MaterialYouWarmGray: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurface

private val MaterialYouMutedGray: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

private val MaterialYouPeach: Color
    @Composable
    get() = MaterialTheme.colorScheme.tertiary

private val MaterialYouYellow: Color
    @Composable
    get() = MaterialTheme.colorScheme.tertiary

@Composable
private fun MaterialYouProgressBar(
    progress: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 6.dp,
    showThumb: Boolean = false,
    isPlaying: Boolean = true,
    onSeek: ((Float) -> Unit)? = null
) {
    val phaseShift = if (showThumb && isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "squiggly")
        val phase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
        phase
    } else {
        0f
    }

    val seekModifier = if (onSeek != null) {
        Modifier
            .pointerInput(onSeek) {
                detectTapGestures { offset ->
                    if (size.width > 0) {
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        CapsuleStateManager.keepExpandedAlive()
                        onSeek(fraction)
                    }
                }
            }
            .pointerInput(onSeek) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    if (size.width > 0) {
                        val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        CapsuleStateManager.keepExpandedAlive()
                        onSeek(fraction)
                    }
                }
            }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(if (showThumb) height * 4.5f else height)
            .then(seekModifier)
    ) {
        val width = size.width
        val canvasHeight = size.height
        val barHeight = height.toPx()
        val centerY = canvasHeight / 2
        val activeWidth = (width * progress.coerceIn(0f, 1f)).coerceIn(0f, width)

        // 1. Draw Inactive Background Track (Straight sleek pill track)
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, centerY - barHeight / 2),
            size = androidx.compose.ui.geometry.Size(width, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2, barHeight / 2)
        )

        // 2. Draw Active Track (Android 13/14 Material You Squiggly Wavy Track when playing)
        if (activeWidth > 0f) {
            if (showThumb && isPlaying) {
                val wavePath = androidx.compose.ui.graphics.Path()
                val waveLength = 26.dp.toPx()
                val waveAmplitude = 3.2.dp.toPx()
                
                wavePath.moveTo(0f, centerY)
                var x = 0f
                val step = 1.5f
                while (x <= activeWidth) {
                    // Dampen amplitude near the very beginning (0..waveLength) for smooth rounded start
                    val dampening = (x / waveLength).coerceIn(0f, 1f)
                    val relativeX = (x / waveLength) * (2 * Math.PI.toFloat()) - phaseShift
                    val y = centerY + (Math.sin(relativeX.toDouble()).toFloat() * waveAmplitude * dampening)
                    wavePath.lineTo(x, y)
                    x += step
                }

                drawPath(
                    path = wavePath,
                    color = color,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = barHeight,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            } else {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(0f, centerY - barHeight / 2),
                    size = androidx.compose.ui.geometry.Size(activeWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2, barHeight / 2)
                )
            }
        }

        // 3. Draw Material You Distinctive Pill / Dot Thumb
        if (showThumb) {
            val thumbX = activeWidth.coerceIn(barHeight / 2, width - barHeight / 2)
            val thumbWidth = barHeight * 0.9f
            val thumbHeight = barHeight * 2.8f
            // Vertical pill thumb (Material You media style)
            drawRoundRect(
                color = color,
                topLeft = Offset(thumbX - thumbWidth / 2, centerY - thumbHeight / 2),
                size = androidx.compose.ui.geometry.Size(thumbWidth, thumbHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(thumbWidth / 2, thumbWidth / 2)
            )
        }
    }
}

@Composable
private fun CollapsedBubbleContent(
    event: CapsuleEvent,
    isSplit: Boolean,
    showCutHint: Boolean,
    cameraPosition: String = "Center",
    cameraWidthDp: Int = 36
) {
    val context = LocalContext.current
    val settings = remember { CapsuleSettings(context) }

    if (isSplit) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (event) {
                is CapsuleEvent.Battery -> {
                    Icon(
                        imageVector = if (event.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.Battery5Bar,
                        contentDescription = null,
                        tint = if (event.isCharging) MaterialYouSageGreen else if (event.isLow) MaterialYouCoral else MaterialYouWarmGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                is CapsuleEvent.Music -> {
                    if (event.albumArt != null) {
                        Image(
                            bitmap = event.albumArt.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialYouLavender,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                is CapsuleEvent.Bluetooth -> {
                    val imagePath = settings.bluetoothImagePath
                    val bitmap = if (imagePath != null) {
                        remember(imagePath) {
                            try {
                                android.graphics.BitmapFactory.decodeFile(imagePath)
                            } catch (e: Exception) {
                                null
                            }
                        }
                    } else {
                        null
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Bluetooth,
                            contentDescription = null,
                            tint = MaterialYouPastelBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                is CapsuleEvent.Network -> {
                    Icon(
                        imageVector = if (event.type == "airplane") Icons.Rounded.AirplaneTicket else Icons.Rounded.Wifi,
                        contentDescription = null,
                        tint = MaterialYouPastelBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                is CapsuleEvent.SoundProfile -> {
                    val icon = when (event.profile) {
                        "Silent" -> Icons.Rounded.VolumeOff
                        "Vibrate" -> Icons.Rounded.Vibration
                        else -> Icons.Rounded.VolumeUp
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (event.profile == "Silent") MaterialYouCoral else MaterialYouPastelBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                is CapsuleEvent.USB -> {
                    Icon(
                        imageVector = Icons.Rounded.Usb,
                        contentDescription = null,
                        tint = MaterialYouWarmGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                is CapsuleEvent.LockState -> {
                    LockStateAnimatedIcon(isLocked = event.isLocked, size = 16.dp)
                }
                is CapsuleEvent.Timer -> {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = MaterialYouPeach,
                        modifier = Modifier.size(16.dp)
                    )
                }
                is CapsuleEvent.Navigation -> {
                    Icon(
                        imageVector = Icons.Rounded.Navigation,
                        contentDescription = null,
                        tint = MaterialYouPastelBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                is CapsuleEvent.Progress -> {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = MaterialYouPastelBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                is CapsuleEvent.HourlyTracker -> {
                    val icon = when {
                        event.trackerName.lowercase().contains("step") -> Icons.Rounded.DirectionsRun
                        event.trackerName.lowercase().contains("water") -> Icons.Rounded.LocalDrink
                        else -> Icons.Rounded.Equalizer
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialYouSageGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }
                is CapsuleEvent.Notification -> {
                    if (event.appIcon != null) {
                        Image(
                            bitmap = event.appIcon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Message,
                            contentDescription = null,
                            tint = MaterialYouPastelBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                is CapsuleEvent.Authentication -> {
                    Icon(
                        imageVector = if (event.success) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                        contentDescription = null,
                        tint = if (event.success) MaterialYouSageGreen else MaterialYouCoral,
                        modifier = Modifier.size(16.dp)
                    )
                }
                is CapsuleEvent.Call -> {
                    Icon(
                        imageVector = if (event.isIncoming) Icons.Rounded.PhoneCallback else Icons.Rounded.Call,
                        contentDescription = null,
                        tint = MaterialYouSageGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
                is CapsuleEvent.Recording -> {
                    Icon(
                        imageVector = if (event.type == "screen") Icons.Rounded.FiberManualRecord else Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = MaterialYouCoral,
                        modifier = Modifier.size(14.dp)
                    )
                }
                is CapsuleEvent.Hotspot -> {
                    Icon(
                        imageVector = Icons.Rounded.WifiTethering,
                        contentDescription = null,
                        tint = MaterialYouPastelBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                is CapsuleEvent.Delivery -> {
                    val icon = when (event.appName.lowercase()) {
                        "uber", "ola", "ride sharing", "cab" -> Icons.Rounded.DirectionsCar
                        else -> Icons.Rounded.DeliveryDining
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialYouYellow,
                        modifier = Modifier.size(16.dp)
                    )
                }
                is CapsuleEvent.SystemToggle -> {
                    val icon = when (event.name.lowercase()) {
                        "wi-fi" -> Icons.Rounded.Wifi
                        "flashlight" -> Icons.Rounded.FlashlightOn
                        "dnd" -> Icons.Rounded.DoNotDisturbOn
                        "silent" -> Icons.Rounded.VolumeOff
                        "ring" -> Icons.Rounded.VolumeUp
                        "vibrate" -> Icons.Rounded.Vibration
                        "airplane" -> Icons.Rounded.AirplaneTicket
                        "low power" -> Icons.Rounded.BatterySaver
                        else -> Icons.Rounded.Settings
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (event.isEnabled) MaterialYouSageGreen else MaterialYouMutedGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                is CapsuleEvent.Stopwatch -> {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = MaterialYouPastelBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                is CapsuleEvent.CalendarEvent -> {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF90CAF9),
                        modifier = Modifier.size(16.dp)
                    )
                }
                is CapsuleEvent.Weather -> {
                    val icon = if (event.condition.lowercase().contains("rain")) Icons.Rounded.Umbrella else Icons.Rounded.WbSunny
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF80DEEA),
                        modifier = Modifier.size(16.dp)
                    )
                }
                is CapsuleEvent.Alarm -> {
                    Icon(
                        imageVector = Icons.Rounded.Alarm,
                        contentDescription = null,
                        tint = MaterialYouPeach,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    } else {
        CutoutAwareRow(
            cameraPosition = cameraPosition,
            cameraWidthDp = cameraWidthDp,
            leftContent = {
                when (event) {
                    is CapsuleEvent.Battery -> {
                        Icon(
                            imageVector = if (event.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.Battery5Bar,
                            contentDescription = null,
                            tint = if (event.isCharging) MaterialYouSageGreen else if (event.isLow) MaterialYouCoral else MaterialYouWarmGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    is CapsuleEvent.Music -> {
                        if (event.albumArt != null) {
                            Image(
                                bitmap = event.albumArt.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = MaterialYouLavender,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    is CapsuleEvent.Bluetooth -> {
                        val imagePath = settings.bluetoothImagePath
                        val bitmap = if (imagePath != null) {
                            remember(imagePath) {
                                try {
                                    android.graphics.BitmapFactory.decodeFile(imagePath)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        } else {
                            null
                        }

                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Bluetooth,
                                contentDescription = null,
                                tint = MaterialYouPastelBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    is CapsuleEvent.Network -> {
                        Icon(
                            imageVector = if (event.type == "airplane") Icons.Rounded.AirplaneTicket else Icons.Rounded.Wifi,
                            contentDescription = null,
                            tint = MaterialYouPastelBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    is CapsuleEvent.SoundProfile -> {
                        val icon = when (event.profile) {
                            "Silent" -> Icons.Rounded.VolumeOff
                            "Vibrate" -> Icons.Rounded.Vibration
                            else -> Icons.Rounded.VolumeUp
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (event.profile == "Silent") MaterialYouCoral else MaterialYouPastelBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    is CapsuleEvent.USB -> {
                        Icon(
                            imageVector = Icons.Rounded.Usb,
                            contentDescription = null,
                            tint = MaterialYouWarmGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    is CapsuleEvent.LockState -> {
                        LockStateAnimatedIcon(isLocked = event.isLocked, size = 16.dp)
                    }
                    is CapsuleEvent.Timer -> {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = MaterialYouPeach,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    is CapsuleEvent.Navigation -> {
                        Icon(
                            imageVector = Icons.Rounded.Navigation,
                            contentDescription = null,
                            tint = MaterialYouPastelBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    is CapsuleEvent.Progress -> {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = MaterialYouPastelBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    is CapsuleEvent.HourlyTracker -> {
                        val icon = when {
                            event.trackerName.lowercase().contains("step") -> Icons.Rounded.DirectionsRun
                            event.trackerName.lowercase().contains("water") -> Icons.Rounded.LocalDrink
                            else -> Icons.Rounded.Equalizer
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialYouSageGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    is CapsuleEvent.Notification -> {
                        if (event.appIcon != null) {
                            Image(
                                bitmap = event.appIcon.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Message,
                                contentDescription = null,
                                tint = MaterialYouPastelBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    is CapsuleEvent.Authentication -> {
                        Icon(
                            imageVector = if (event.success) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                            contentDescription = null,
                            tint = if (event.success) MaterialYouSageGreen else MaterialYouCoral,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    is CapsuleEvent.Call -> {
                        Icon(
                            imageVector = if (event.isIncoming) Icons.Rounded.PhoneCallback else Icons.Rounded.Call,
                            contentDescription = null,
                            tint = MaterialYouSageGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    is CapsuleEvent.Recording -> {
                        Icon(
                            imageVector = if (event.type == "screen") Icons.Rounded.FiberManualRecord else Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = MaterialYouCoral,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    is CapsuleEvent.Hotspot -> {
                        Icon(
                            imageVector = Icons.Rounded.WifiTethering,
                            contentDescription = null,
                            tint = MaterialYouPastelBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    is CapsuleEvent.Delivery -> {
                        val icon = when (event.appName.lowercase()) {
                            "uber", "ola", "ride sharing", "cab" -> Icons.Rounded.DirectionsCar
                            else -> Icons.Rounded.DeliveryDining
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialYouYellow,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    is CapsuleEvent.SystemToggle -> {
                        val icon = when (event.name.lowercase()) {
                            "wi-fi" -> Icons.Rounded.Wifi
                            "flashlight" -> Icons.Rounded.FlashlightOn
                            "dnd" -> Icons.Rounded.DoNotDisturbOn
                            "silent" -> Icons.Rounded.VolumeOff
                            "ring" -> Icons.Rounded.VolumeUp
                            "vibrate" -> Icons.Rounded.Vibration
                            "airplane" -> Icons.Rounded.AirplaneTicket
                            "low power" -> Icons.Rounded.BatterySaver
                            else -> Icons.Rounded.Settings
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (event.isEnabled) MaterialYouSageGreen else MaterialYouMutedGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    is CapsuleEvent.Stopwatch -> {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = MaterialYouPastelBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    is CapsuleEvent.CalendarEvent -> {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFF90CAF9),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    is CapsuleEvent.Weather -> {
                        val icon = if (event.condition.lowercase().contains("rain")) Icons.Rounded.Umbrella else Icons.Rounded.WbSunny
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(0xFF80DEEA),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    is CapsuleEvent.Alarm -> {
                        Icon(
                            imageVector = Icons.Rounded.Alarm,
                            contentDescription = null,
                            tint = MaterialYouPeach,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            rightContent = {
                when (event) {
                    is CapsuleEvent.Battery -> {
                        Text(
                            text = "${event.level}%",
                            color = if (event.isCharging) MaterialYouSageGreen else if (event.isLow) MaterialYouCoral else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is CapsuleEvent.Music -> {
                        if (settings.showMusicVisualizer) {
                            MusicVisualizer(isPlaying = event.isPlaying)
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Equalizer,
                                contentDescription = null,
                                tint = MaterialYouLavender,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    is CapsuleEvent.Bluetooth -> {
                        Text(
                            text = "${event.deviceName} (${if (event.batteryLevel >= 0) "${event.batteryLevel}%" else "ON"})",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    is CapsuleEvent.Network -> {
                        Text(
                            text = event.statusText,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is CapsuleEvent.SoundProfile -> {
                        Text(
                            text = event.profile,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is CapsuleEvent.USB -> {
                        Text(
                            text = "USB",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is CapsuleEvent.LockState -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!event.isLocked) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF81C784),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = if (event.isLocked) "Locked" else "Unlocked",
                                color = if (event.isLocked) Color.White.copy(alpha = 0.85f) else Color(0xFF81C784),
                                fontSize = 11.sp,
                                fontWeight = if (event.isLocked) FontWeight.SemiBold else FontWeight.ExtraBold,
                                letterSpacing = 0.2.sp
                            )
                        }
                    }
                    is CapsuleEvent.Timer -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (event.isFiring) "TIME'S UP" else formatTime(event.remainingSeconds),
                                color = if (event.isFiring) MaterialYouCoral else MaterialYouPeach,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            if (event.isRunning) {
                                RotatingHourglass()
                            } else {
                                Icon(
                                    imageVector = if (event.isFiring) Icons.Rounded.NotificationsActive else Icons.Rounded.Pause,
                                    contentDescription = null,
                                    tint = if (event.isFiring) MaterialYouCoral else MaterialYouPeach,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                    is CapsuleEvent.Navigation -> {
                        Text(
                            text = event.distance,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is CapsuleEvent.Progress -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${event.progress}%",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            CircularProgressIndicator(
                                progress = event.progress.toFloat() / 100f,
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialYouPastelBlue,
                                trackColor = Color(0x33FFFFFF)
                            )
                        }
                    }
                    is CapsuleEvent.HourlyTracker -> {
                        Text(
                            text = event.countText,
                            color = MaterialYouSageGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is CapsuleEvent.CalendarEvent -> {
                        Text(
                            text = event.title,
                            color = Color(0xFF90CAF9),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    is CapsuleEvent.Weather -> {
                        Text(
                            text = event.tempText,
                            color = Color(0xFF80DEEA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is CapsuleEvent.Notification -> {
                        Text(
                            text = event.title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    is CapsuleEvent.Authentication -> {
                        Text(
                            text = if (event.success) "Approved" else "Failed",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is CapsuleEvent.Call -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = event.contactName,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(6.dp))
                            VoiceWaveVisualizer()
                        }
                    }
                    is CapsuleEvent.Recording -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = event.durationText,
                                color = MaterialYouCoral,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            VoiceWaveVisualizer()
                        }
                    }
                    is CapsuleEvent.Hotspot -> {
                        Text(
                            text = "${event.connections}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is CapsuleEvent.Delivery -> {
                        Text(
                            text = event.statusText,
                            color = Color.White,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    is CapsuleEvent.SystemToggle -> {
                        Text(
                            text = if (event.isEnabled) "ON" else "OFF",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                    is CapsuleEvent.Stopwatch -> {
                        val h = event.elapsedSeconds / 3600
                        val m = (event.elapsedSeconds % 3600) / 60
                        val s = event.elapsedSeconds % 60
                        val fmt = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
                        Text(
                            text = fmt,
                            color = MaterialYouPastelBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is CapsuleEvent.Alarm -> {
                        Text(
                            text = event.timeText,
                            color = MaterialYouPeach,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun SplitContent(event: CapsuleEvent) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (event) {
            is CapsuleEvent.Timer -> {
                val mins = event.remainingSeconds / 60
                val secs = event.remainingSeconds % 60
                Text(
                    text = if (mins > 0) "${mins}m" else "${secs}s",
                    color = MaterialYouPeach,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            is CapsuleEvent.Battery -> {
                Text(
                    text = "${event.level}%",
                    color = if (event.isCharging) MaterialYouSageGreen else if (event.isLow) MaterialYouCoral else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            is CapsuleEvent.Music -> {
                MusicVisualizer(isPlaying = event.isPlaying)
            }
            is CapsuleEvent.Call -> {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = null,
                    tint = MaterialYouSageGreen,
                    modifier = Modifier.size(13.dp)
                )
            }
            is CapsuleEvent.Recording -> {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialYouCoral)
                )
            }
            is CapsuleEvent.Hotspot -> {
                Text(
                    text = "${event.connections}",
                    color = MaterialYouPastelBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            is CapsuleEvent.Stopwatch -> {
                val m = (event.elapsedSeconds % 3600) / 60
                val s = event.elapsedSeconds % 60
                Text(
                    text = "%02d:%02d".format(m, s),
                    color = MaterialYouPastelBlue,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            is CapsuleEvent.Bluetooth -> {
                Icon(
                    imageVector = Icons.Rounded.Headphones,
                    contentDescription = null,
                    tint = MaterialYouPastelBlue,
                    modifier = Modifier.size(13.dp)
                )
            }
            is CapsuleEvent.Navigation -> {
                Icon(
                    imageVector = Icons.Rounded.Directions,
                    contentDescription = null,
                    tint = MaterialYouSageGreen,
                    modifier = Modifier.size(13.dp)
                )
            }
            is CapsuleEvent.Progress -> {
                val pct = if (event.max > 0) (event.progress * 100) / event.max else event.progress
                Text(
                    text = "$pct%",
                    color = MaterialYouPastelBlue,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            is CapsuleEvent.Delivery -> {
                Icon(
                    imageVector = Icons.Rounded.LocalShipping,
                    contentDescription = null,
                    tint = MaterialYouPeach,
                    modifier = Modifier.size(13.dp)
                )
            }
            is CapsuleEvent.Notification -> {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = null,
                    tint = MaterialYouLavender,
                    modifier = Modifier.size(13.dp)
                )
            }
            is CapsuleEvent.Alarm -> {
                Icon(
                    imageVector = Icons.Rounded.Alarm,
                    contentDescription = null,
                    tint = MaterialYouCoral,
                    modifier = Modifier.size(13.dp)
                )
            }
            is CapsuleEvent.Weather -> {
                Text(
                    text = event.tempText,
                    color = MaterialYouPeach,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            is CapsuleEvent.CalendarEvent -> {
                Icon(
                    imageVector = Icons.Rounded.Event,
                    contentDescription = null,
                    tint = MaterialYouLavender,
                    modifier = Modifier.size(13.dp)
                )
            }
            is CapsuleEvent.SystemToggle -> {
                Icon(
                    imageVector = if (event.name.equals("Flashlight", ignoreCase = true)) Icons.Rounded.FlashlightOn else Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = MaterialYouPeach,
                    modifier = Modifier.size(13.dp)
                )
            }
            is CapsuleEvent.SoundProfile -> {
                Icon(
                    imageVector = when (event.profile) {
                        "Silent" -> Icons.Rounded.VolumeOff
                        "Vibrate" -> Icons.Rounded.Vibration
                        else -> Icons.Rounded.VolumeUp
                    },
                    contentDescription = null,
                    tint = MaterialYouLavender,
                    modifier = Modifier.size(13.dp)
                )
            }
            is CapsuleEvent.Network -> {
                Icon(
                    imageVector = if (event.type == "airplane") Icons.Rounded.AirplaneTicket else Icons.Rounded.Wifi,
                    contentDescription = null,
                    tint = if (event.type == "airplane") MaterialYouCoral else MaterialYouSageGreen,
                    modifier = Modifier.size(13.dp)
                )
            }
            is CapsuleEvent.USB -> {
                Icon(
                    imageVector = Icons.Rounded.Usb,
                    contentDescription = null,
                    tint = MaterialYouPastelBlue,
                    modifier = Modifier.size(13.dp)
                )
            }
            is CapsuleEvent.LockState -> {
                LockStateAnimatedIcon(isLocked = event.isLocked, size = 13.dp)
            }
            is CapsuleEvent.Authentication -> {
                Icon(
                    imageVector = Icons.Rounded.Fingerprint,
                    contentDescription = null,
                    tint = MaterialYouPastelBlue,
                    modifier = Modifier.size(13.dp)
                )
            }
            is CapsuleEvent.HourlyTracker -> {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = MaterialYouPeach,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

private fun capsuleEventLabel(event: CapsuleEvent): String = when (event) {
    is CapsuleEvent.Music -> {
        val base = event.appName.ifBlank { "NOW PLAYING" }.uppercase().take(14)
        if (event.isLiveStream) "$base • LIVE" else base
    }
    is CapsuleEvent.Battery -> if (event.isCharging) "CHARGING" else "BATTERY"
    is CapsuleEvent.Bluetooth -> "BLUETOOTH"
    is CapsuleEvent.Timer -> when {
        event.isFiring -> "TIMER • TIME'S UP"
        event.isSystemSynced -> "${event.appName.ifBlank { "CLOCK" }.uppercase()} • SYNCED"
        else -> "TIMER"
    }
    is CapsuleEvent.Navigation -> "NAVIGATION"
    is CapsuleEvent.Progress -> "DOWNLOAD"
    is CapsuleEvent.Notification -> event.appName.uppercase().take(12)
    is CapsuleEvent.Authentication -> "SECURITY"
    is CapsuleEvent.Call -> "ACTIVE CALL"
    is CapsuleEvent.Recording -> "RECORDING"
    is CapsuleEvent.Hotspot -> "HOTSPOT"
    is CapsuleEvent.Delivery -> "LIVE ORDER"
    is CapsuleEvent.SystemToggle -> event.name.uppercase()
    is CapsuleEvent.Stopwatch -> if (event.isSystemSynced) "${event.appName.ifBlank { "CLOCK" }.uppercase()} • SYNCED" else "STOPWATCH"
    is CapsuleEvent.HourlyTracker -> "TRACKER"
    is CapsuleEvent.Alarm -> "ALARM"
    is CapsuleEvent.CalendarEvent -> "CALENDAR"
    is CapsuleEvent.Weather -> "WEATHER"
    is CapsuleEvent.Network -> "NETWORK"
    is CapsuleEvent.SoundProfile -> "SOUND MODE"
    is CapsuleEvent.USB -> "USB PORT"
    is CapsuleEvent.LockState -> "DEVICE LOCK"
}

@Composable
private fun ExpandedCard(
    event: CapsuleEvent,
    cameraPosition: String = "Center",
    cameraWidthDp: Int = 36,
    heightDp: Int = 36
) {
    val context = LocalContext.current
    val settings = remember { CapsuleSettings(context) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val uiState by CapsuleStateManager.uiState.collectAsStateWithLifecycle()

    // ── CONTENT FADE-IN DELAY ─────────────────────────────────────────────────
    var contentVisible by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "contentAlpha"
    )
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(115)
        contentVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = contentAlpha }
            .pointerInput(event.id) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        CapsuleStateManager.keepExpandedAlive()
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val dy = dragAmount.y
                    if (dy < -8f) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        CapsuleStateManager.collapseToCompact()
                    }
                }
            }
    ) {
        // ── CUTOUT-AWARE TOP CAPSULE STATUS STRIP ─────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (cameraPosition == "Center") {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialYouSageGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = capsuleEventLabel(event),
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width((cameraWidthDp + 12).dp))
            } else if (cameraPosition == "Left") {
                Spacer(modifier = Modifier.width((cameraWidthDp + 16).dp))
                Text(
                    text = capsuleEventLabel(event),
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = capsuleEventLabel(event),
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = if (cameraPosition == "Center") Modifier.weight(1f) else Modifier,
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val secondaryEvent = uiState.splitEvent
                if (secondaryEvent != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                CapsuleStateManager.swapMainAndSplit(expandAfterSwap = true)
                            }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = "Switch Activity",
                            tint = MaterialYouPastelBlue,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = capsuleEventLabel(secondaryEvent).take(7),
                            color = MaterialYouPastelBlue,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            CapsuleStateManager.collapseToCompact()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "Minimize",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            CapsuleStateManager.removeEvent(event.id)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(11.dp)
                    )
                }

                if (cameraPosition == "Right") {
                    Spacer(modifier = Modifier.width((cameraWidthDp + 16).dp))
                }
            }
        }

        // ── MAIN EXPANDED FEATURE CONTENT ─────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = heightDp.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            when (event) {
                is CapsuleEvent.Music -> MusicExpandedCard(event)
                is CapsuleEvent.Battery -> BatteryExpandedCard(event, context, haptic)
                is CapsuleEvent.Bluetooth -> BluetoothExpandedCard(event, context, haptic, settings)
                is CapsuleEvent.Timer -> TimerExpandedCard(event)
                is CapsuleEvent.Navigation -> NavigationExpandedCard(event, context, haptic)
                is CapsuleEvent.Progress -> ProgressExpandedCard(event)
                is CapsuleEvent.Notification -> NotificationExpandedCard(event)
                is CapsuleEvent.Authentication -> AuthenticationExpandedCard(event)
                is CapsuleEvent.Call -> CallExpandedCard(event)
                is CapsuleEvent.Recording -> RecordingExpandedCard(event)
                is CapsuleEvent.Hotspot -> HotspotExpandedCard(event, context, haptic)
                is CapsuleEvent.Delivery -> DeliveryExpandedCard(event)
                is CapsuleEvent.SystemToggle -> SystemToggleExpandedCard(event, context, haptic)
                is CapsuleEvent.Stopwatch -> StopwatchExpandedCard(event)
                is CapsuleEvent.HourlyTracker -> HourlyTrackerExpandedCard(event, context)
                is CapsuleEvent.Alarm -> AlarmExpandedCard(event)
                is CapsuleEvent.CalendarEvent -> CalendarEventExpandedCard(event)
                is CapsuleEvent.Weather -> WeatherExpandedCard(event)
                is CapsuleEvent.Network -> NetworkExpandedCard(event, context, haptic, scope)
                is CapsuleEvent.SoundProfile -> SoundProfileExpandedCard(event, context, haptic)
                is CapsuleEvent.USB -> UsbExpandedCard(event, context, haptic)
                is CapsuleEvent.LockState -> LockStateExpandedCard(event, context, haptic)
            }
        }
    }
}

@Composable
fun MusicVisualizer(isPlaying: Boolean) {
    val isRealActive by CapsuleStateManager.isRealVisualizerActive.collectAsStateWithLifecycle()
    val realAmplitudes by CapsuleStateManager.visualizerAmplitudes.collectAsStateWithLifecycle()

    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")

    // ── 4-BAR ORGANIC VISUALIZER ──────────────────────────────────────────────
    // Each bar has a deliberately different duration so they never sync up —
    // creating the irregular, alive-feeling waveform of the iOS Dynamic Island.
    // FastOutSlowInEasing gives a natural "breath" curve instead of robotic linear.
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.20f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(280, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.55f, targetValue = 1.00f,
        animationSpec = infiniteRepeatable(tween(340, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(260, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 0.40f, targetValue = 0.90f,
        animationSpec = infiniteRepeatable(tween(315, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar4"
    )

    val bar1 = if (isPlaying) { if (isRealActive && realAmplitudes.size > 0) realAmplitudes[0] else h1 } else 0.22f
    val bar2 = if (isPlaying) { if (isRealActive && realAmplitudes.size > 1) realAmplitudes[1] else h2 } else 0.22f
    val bar3 = if (isPlaying) { if (isRealActive && realAmplitudes.size > 2) realAmplitudes[2] else h3 } else 0.22f
    val bar4 = if (isPlaying) { if (isRealActive && realAmplitudes.size > 3) realAmplitudes[3] else h4 } else 0.22f

    Row(
        modifier = Modifier
            .height(16.dp)
            .width(22.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(bar1, bar2, bar3, bar4).forEach { h ->
            Box(
                Modifier
                    .fillMaxHeight(h.coerceIn(0.08f, 1.0f))
                    .weight(1f)
                    .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(percent = 50))
            )
        }
    }
}

private fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format("%02d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}

@Composable
fun VoiceWaveVisualizer() {
    val isRealActive by CapsuleStateManager.isRealVisualizerActive.collectAsStateWithLifecycle()
    val realAmplitudes by CapsuleStateManager.visualizerAmplitudes.collectAsStateWithLifecycle()

    val infiniteTransition = rememberInfiniteTransition(label = "voice")
    // ── 4-BAR ORGANIC VOICE WAVEFORM ──────────────────────────────────────────
    // Used for calls and voice recording — green tinted, same organic timing logic
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 0.90f,
        animationSpec = infiniteRepeatable(tween(290, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "v1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.50f, targetValue = 1.00f,
        animationSpec = infiniteRepeatable(tween(245, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "v2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(335, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "v3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(275, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "v4"
    )

    val b1 = if (isRealActive && realAmplitudes.size > 0) realAmplitudes[0] else h1
    val b2 = if (isRealActive && realAmplitudes.size > 1) realAmplitudes[1] else h2
    val b3 = if (isRealActive && realAmplitudes.size > 2) realAmplitudes[2] else h3
    val b4 = if (isRealActive && realAmplitudes.size > 3) realAmplitudes[3] else h4

    Row(
        modifier = Modifier
            .height(16.dp)
            .width(22.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(b1, b2, b3, b4).forEach { h ->
            Box(
                Modifier
                    .fillMaxHeight(h.coerceIn(0.08f, 1.0f))
                    .weight(1f)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.92f), RoundedCornerShape(percent = 50))
            )
        }
    }
}

@Composable
fun RotatingHourglass() {
    val infiniteTransition = rememberInfiniteTransition(label = "hourglass")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    Icon(
        imageVector = Icons.Rounded.HourglassEmpty,
        contentDescription = null,
        tint = MaterialYouPeach,
        modifier = Modifier
            .size(12.dp)
            .graphicsLayer(rotationZ = rotation)
    )
}

@Composable
fun CalibrationScreen(
    xOffset: Int,
    yOffset: Int,
    widthDp: Int,
    heightDp: Int,
    edgeRoundingPercent: Int,
    cameraPosition: String,
    cameraWidthDp: Int,
    splitPosition: String,
    showAsNotch: Boolean,
    isPositionLocked: Boolean
) {
    val configuration = LocalConfiguration.current
    val screenWidthFloatDp = configuration.screenWidthDp.toFloat()
    val density = LocalDensity.current.density

    val baseCameraCenterXDp = when (cameraPosition) {
        "Left"  -> (cameraWidthDp / 2f + 16f)
        "Right" -> (screenWidthFloatDp - (cameraWidthDp / 2f + 16f))
        else    -> screenWidthFloatDp / 2f
    }
    val minAnchorX = (cameraWidthDp / 2f + 4f)
    val maxAnchorX = (screenWidthFloatDp - cameraWidthDp / 2f - 4f).coerceAtLeast(minAnchorX)
    val cameraCenterXDp = (baseCameraCenterXDp + (xOffset / density)).coerceIn(minAnchorX, maxAnchorX)
    val topOffsetDp = if (showAsNotch) 0f else (yOffset / density).coerceAtLeast(0f)

    val effectiveWidthDp = widthDp.coerceAtLeast(heightDp * 2).toFloat()
    val pillLeftDp = when {
        cameraPosition == "Left" -> {
            val leftMargin = 4f
            (cameraCenterXDp - (cameraWidthDp / 2f) - leftMargin).coerceIn(2f, (screenWidthFloatDp - effectiveWidthDp - 2f).coerceAtLeast(2f))
        }
        cameraPosition == "Right" -> {
            val rightMargin = 4f
            (cameraCenterXDp + (cameraWidthDp / 2f) + rightMargin - effectiveWidthDp).coerceIn(2f, (screenWidthFloatDp - effectiveWidthDp - 2f).coerceAtLeast(2f))
        }
        else -> {
            (cameraCenterXDp - (effectiveWidthDp / 2f)).coerceIn(2f, (screenWidthFloatDp - effectiveWidthDp - 2f).coerceAtLeast(2f))
        }
    }

    val splitGap = 8f
    val splitSizeDp = heightDp.toFloat()
    val rightSplitCandidate = pillLeftDp + effectiveWidthDp + splitGap
    val leftSplitCandidate = pillLeftDp - splitGap - splitSizeDp
    val splitLeftDp = if (splitPosition == "Right") {
        if (rightSplitCandidate + splitSizeDp <= screenWidthFloatDp - 2f) rightSplitCandidate else leftSplitCandidate.coerceAtLeast(2f)
    } else {
        if (leftSplitCandidate >= 2f) leftSplitCandidate else rightSplitCandidate.coerceAtMost((screenWidthFloatDp - splitSizeDp - 2f).coerceAtLeast(2f))
    }

    val collapsedRadiusDp = (heightDp.toFloat() / 2f) * (edgeRoundingPercent / 60f).coerceIn(0.15f, 1f)
    val pillShape = if (showAsNotch) {
        RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = collapsedRadiusDp.dp, bottomEnd = collapsedRadiusDp.dp)
    } else {
        RoundedCornerShape(collapsedRadiusDp.dp)
    }

    val accentColor = if (isPositionLocked) Color(0xFF00E676) else Color(0xFF00E5FF)

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Capsule Outline + Internal Layout Preview (Left / Camera Hole / Right in same area)
        Box(
            modifier = Modifier
                .offset(x = pillLeftDp.dp, y = topOffsetDp.dp)
                .width(effectiveWidthDp.dp)
                .height(heightDp.dp)
                .clip(pillShape)
                .background(Color.Black.copy(alpha = 0.92f))
                .border(2.dp, accentColor, pillShape)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            CutoutAwareRow(
                cameraPosition = cameraPosition,
                cameraWidthDp = cameraWidthDp,
                leftContent = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(
                            imageVector = if (isPositionLocked) Icons.Rounded.Lock else Icons.Rounded.CenterFocusStrong,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isPositionLocked) "FIXED" else "MAIN",
                            color = accentColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                },
                rightContent = {
                    Text(
                        text = "${widthDp}×${heightDp}",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }

        // Camera Punch Hole alignment marker (Red target ring with center crosshair dot)
        Box(
            modifier = Modifier
                .offset(
                    x = (cameraCenterXDp - (cameraWidthDp / 2f)).dp,
                    y = (topOffsetDp + (heightDp - cameraWidthDp) / 2f).dp
                )
                .size(cameraWidthDp.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF3B30).copy(alpha = 0.45f))
                .border(2.dp, Color(0xFFFF3B30), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(Color.White, CircleShape)
            )
        }

        // Side Split Capsule preview outline in the exact same area
        Box(
            modifier = Modifier
                .offset(x = splitLeftDp.dp, y = topOffsetDp.dp)
                .size(heightDp.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.90f))
                .border(1.5.dp, Color(0xFFFF9800), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SPLIT",
                color = Color(0xFFFF9800),
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Compact non-intrusive status badge directly below the capsule in the same area
        val badgeWidthDp = 180f
        val badgeLeftDp = (pillLeftDp + (effectiveWidthDp - badgeWidthDp) / 2f)
            .coerceIn(4f, (screenWidthFloatDp - badgeWidthDp - 4f).coerceAtLeast(4f))
        Box(
            modifier = Modifier
                .offset(
                    x = badgeLeftDp.dp,
                    y = (topOffsetDp + heightDp + 4f).dp
                )
                .width(badgeWidthDp.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xDD0B0E14))
                .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${if (isPositionLocked) "🔒 FIXED" else "🎯 ALIGN"} • X:${xOffset}px Y:${yOffset}px • ${cameraPosition}",
                color = accentColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CutoutAwareRow(
    cameraPosition: String,
    cameraWidthDp: Int,
    modifier: Modifier = Modifier,
    leftContent: @Composable () -> Unit,
    rightContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (cameraPosition) {
            "Left" -> {
                // Spacer for camera cutout on the left
                Spacer(modifier = Modifier.width(cameraWidthDp.dp))
                Spacer(modifier = Modifier.width(8.dp)) // padding from camera
                Box(modifier = Modifier.wrapContentSize(), contentAlignment = Alignment.CenterStart) {
                    leftContent()
                }
                if (rightContent != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.wrapContentSize(), contentAlignment = Alignment.CenterEnd) {
                        rightContent()
                    }
                }
            }
            "Right" -> {
                Box(modifier = Modifier.wrapContentSize(), contentAlignment = Alignment.CenterStart) {
                    leftContent()
                }
                if (rightContent != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.wrapContentSize(), contentAlignment = Alignment.CenterEnd) {
                        rightContent()
                    }
                }
                Spacer(modifier = Modifier.width(8.dp)) // padding from camera
                // Spacer for camera cutout on the right
                Spacer(modifier = Modifier.width(cameraWidthDp.dp))
            }
            else -> { // Center
                // Left content on the left
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.Start),
                    contentAlignment = Alignment.CenterStart
                ) {
                    leftContent()
                }
                // Spacer for camera cutout in the center
                Spacer(modifier = Modifier.width(cameraWidthDp.dp))
                // Right content on the right
                if (rightContent != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentWidth(Alignment.End),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        rightContent()
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun safeStartActivity(context: Context, intent: Intent) {
    try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val mainIntent = Intent(android.provider.Settings.ACTION_SETTINGS)
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(mainIntent)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}

private fun formatTrackDurationMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

@Composable
private fun MusicExpandedCard(event: CapsuleEvent.Music) {
    val context = LocalContext.current
    val settings = remember { CapsuleSettings(context) }
    val haptic = LocalHapticFeedback.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var currentPosition by remember(event.id, event.position) { mutableStateOf(event.position) }
    var showRemainingTime by remember { mutableStateOf(false) }
    var showVolumeControl by remember { mutableStateOf(false) }
    var musicVolume by remember {
        mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
    val maxMusicVolume = remember {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }

    // Dynamic accent color per streaming app
    val accentColor = remember(event.packageName) {
        val pkg = event.packageName.lowercase()
        when {
            pkg.contains("spotify") -> Color(0xFF1ED760)
            pkg.contains("youtube") || pkg.contains("ytm") -> Color(0xFFFF4E45)
            pkg.contains("saavn") -> Color(0xFF2BC5B4)
            pkg.contains("wynk") || pkg.contains("apple") -> Color(0xFFFF2D55)
            pkg.contains("soundcloud") -> Color(0xFFFF5500)
            pkg.contains("twitch") -> Color(0xFF9146FF)
            pkg.contains("audible") || pkg.contains("pocketfm") -> Color(0xFFFFB300)
            else -> Color(0xFFBA68C8)
        }
    }

    LaunchedEffect(event.isPlaying, event.position, event.playbackSpeed) {
        if (event.isPlaying) {
            while (true) {
                kotlinx.coroutines.delay(500)
                val delta = (500 * event.playbackSpeed.coerceIn(0.5f, 3.0f)).toLong()
                if (event.duration > 0) {
                    currentPosition = (currentPosition + delta).coerceAtMost(event.duration)
                } else {
                    currentPosition += delta
                }
            }
        }
    }

    // ── 1. HEADER: ALBUM ART + TRACK METADATA + AUDIO OUTPUT + EXPLICIT OPEN APP BUTTON ──
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tapping Album Art toggles Play/Pause directly inside the capsule
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.25f))
                .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .clickable {
                    CapsuleStateManager.keepExpandedAlive()
                    val nextAction = if (event.isPlaying) "pause" else "play"
                    com.example.capsulebar.service.HapticSoundManager.playToggle(!event.isPlaying)
                    CapsuleStateManager.sendMediaAction(nextAction)
                },
            contentAlignment = Alignment.Center
        ) {
            if (settings.showImages && event.albumArt != null) {
                Image(
                    bitmap = event.albumArt.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = if (event.isLiveStream) Icons.Rounded.Podcasts else Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (event.isLiveStream) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE53935))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    text = event.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val subtitle = buildString {
                append(event.artist)
                if (event.album.isNotBlank() && !event.album.equals(event.artist, ignoreCase = true)) {
                    append(" • ")
                    append(event.album)
                }
            }
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(6.dp))

        // Visualizer
        MusicVisualizer(isPlaying = event.isPlaying)

        Spacer(Modifier.width(6.dp))

        // Dedicated "Open App" button (only opens the external app when user explicitly taps here)
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    com.example.capsulebar.service.CapsuleNotificationListener.openNotification(
                        context,
                        event.id,
                        event.packageName
                    )
                    CapsuleStateManager.collapseToCompact()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.OpenInNew,
                contentDescription = "Open ${event.appName}",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(14.dp)
            )
        }
    }

    Spacer(Modifier.height(6.dp))

    // ── 2. SEEK / STREAM PROGRESS BAR OR INLINE VOLUME SLIDER ──
    if (showVolumeControl) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (musicVolume == 0) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeDown,
                contentDescription = "Volume Down",
                tint = accentColor,
                modifier = Modifier
                    .size(18.dp)
                    .clickable {
                        CapsuleStateManager.keepExpandedAlive()
                        val newVol = (musicVolume - 1).coerceAtLeast(0)
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                        musicVolume = newVol
                        com.example.capsulebar.service.HapticSoundManager.playClick()
                    }
            )
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                MaterialYouProgressBar(
                    progress = (musicVolume.toFloat() / maxMusicVolume).coerceIn(0f, 1f),
                    color = accentColor,
                    trackColor = Color.White.copy(alpha = 0.18f),
                    height = 5.dp,
                    showThumb = false,
                    onSeek = { ratio ->
                        CapsuleStateManager.keepExpandedAlive()
                        val newVol = (ratio * maxMusicVolume).toInt().coerceIn(0, maxMusicVolume)
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                        musicVolume = newVol
                    }
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.VolumeUp,
                contentDescription = "Volume Up",
                tint = accentColor,
                modifier = Modifier
                    .size(18.dp)
                    .clickable {
                        CapsuleStateManager.keepExpandedAlive()
                        val newVol = (musicVolume + 1).coerceAtMost(maxMusicVolume)
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                        musicVolume = newVol
                        com.example.capsulebar.service.HapticSoundManager.playClick()
                    }
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "${((musicVolume * 100) / maxMusicVolume)}%",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        val effectiveDuration = if (event.duration > 0L) event.duration else 210_000L
        val progress = if (event.isLiveStream) {
            1f
        } else {
            (currentPosition.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
        }

        MaterialYouProgressBar(
            progress = progress,
            color = if (event.isLiveStream) Color(0xFFE53935) else accentColor,
            trackColor = Color.White.copy(alpha = 0.16f),
            height = 5.dp,
            showThumb = !event.isLiveStream,
            isPlaying = event.isPlaying,
            onSeek = if (!event.isLiveStream) { ratio ->
                CapsuleStateManager.keepExpandedAlive()
                val targetMs = (ratio * effectiveDuration).toLong().coerceIn(0L, effectiveDuration)
                currentPosition = targetMs
                com.example.capsulebar.service.HapticSoundManager.playClick()
                CapsuleStateManager.sendMediaAction("seek:$targetMs")
            } else null
        )

        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (event.isLiveStream) "LIVE STREAM" else formatTrackDurationMs(currentPosition),
                color = if (event.isLiveStream) Color(0xFFFF5252) else Color.White.copy(alpha = 0.58f),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (event.isBuffering) {
                Text(
                    text = "BUFFERING...",
                    color = accentColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            } else if (event.audioOutputName.isNotBlank()) {
                Text(
                    text = event.audioOutputName,
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = when {
                    event.isLiveStream -> event.appName.uppercase()
                    event.duration <= 0L -> "--:--"
                    showRemainingTime -> "-${formatTrackDurationMs((event.duration - currentPosition).coerceAtLeast(0L))}"
                    else -> formatTrackDurationMs(event.duration)
                },
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    CapsuleStateManager.keepExpandedAlive()
                    showRemainingTime = !showRemainingTime
                }
            )
        }
    }

    Spacer(Modifier.height(5.dp))

    // ── 3. PRIMARY TRANSPORT & RATING ROW (LIKE, REVERSE -10S, PREV, PLAY/PAUSE, NEXT, FORWARD +10S, DISLIKE) ──
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Like / Heart Toggle
        IconButton(
            onClick = {
                CapsuleStateManager.keepExpandedAlive()
                com.example.capsulebar.service.HapticSoundManager.playToggle(!event.isLiked)
                CapsuleStateManager.sendMediaAction("toggle_like")
            },
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (event.isLiked) Color(0x33FF4081) else Color.Transparent)
        ) {
            Icon(
                imageVector = if (event.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (event.isLiked) "Unlike" else "Like",
                tint = if (event.isLiked) Color(0xFFFF4081) else Color.White.copy(alpha = 0.78f),
                modifier = Modifier.size(18.dp)
            )
        }

        // Reverse -10s (Rewind)
        IconButton(
            onClick = {
                CapsuleStateManager.keepExpandedAlive()
                com.example.capsulebar.service.HapticSoundManager.playClick()
                currentPosition = (currentPosition - 10_000L).coerceAtLeast(0L)
                CapsuleStateManager.sendMediaAction("rewind")
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Replay10,
                contentDescription = "Reverse 10s",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Previous Track
        IconButton(
            onClick = {
                CapsuleStateManager.keepExpandedAlive()
                com.example.capsulebar.service.HapticSoundManager.playClick()
                currentPosition = 0L
                CapsuleStateManager.sendMediaAction("previous")
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Play / Pause Hero Button
        IconButton(
            onClick = {
                CapsuleStateManager.keepExpandedAlive()
                val nextAction = if (event.isPlaying) "pause" else "play"
                com.example.capsulebar.service.HapticSoundManager.playToggle(!event.isPlaying)
                CapsuleStateManager.sendMediaAction(nextAction)
            },
            modifier = Modifier
                .size(42.dp)
                .background(accentColor, CircleShape)
        ) {
            Icon(
                imageVector = if (event.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (event.isPlaying) "Pause" else "Play",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }

        // Next Track
        IconButton(
            onClick = {
                CapsuleStateManager.keepExpandedAlive()
                com.example.capsulebar.service.HapticSoundManager.playClick()
                currentPosition = 0L
                CapsuleStateManager.sendMediaAction("next")
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Forward +10s (Fast Forward)
        IconButton(
            onClick = {
                CapsuleStateManager.keepExpandedAlive()
                com.example.capsulebar.service.HapticSoundManager.playClick()
                val cap = if (event.duration > 0L) event.duration else (currentPosition + 10_000L)
                currentPosition = (currentPosition + 10_000L).coerceAtMost(cap)
                CapsuleStateManager.sendMediaAction("fast_forward")
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Forward10,
                contentDescription = "Forward 10s",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Dislike / Thumbs Down Toggle
        IconButton(
            onClick = {
                CapsuleStateManager.keepExpandedAlive()
                com.example.capsulebar.service.HapticSoundManager.playToggle(!event.isDisliked)
                CapsuleStateManager.sendMediaAction("toggle_dislike")
            },
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (event.isDisliked) Color(0x33FF6E40) else Color.Transparent)
        ) {
            Icon(
                imageVector = Icons.Rounded.ThumbDown,
                contentDescription = if (event.isDisliked) "Remove Dislike" else "Dislike",
                tint = if (event.isDisliked) Color(0xFFFF6E40) else Color.White.copy(alpha = 0.65f),
                modifier = Modifier.size(16.dp)
            )
        }
    }

    Spacer(Modifier.height(6.dp))

    // ── 4. APP-ADAPTIVE STREAM & CUSTOM ACTIONS STRIP ──
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle Chip
        MediaActionChip(
            icon = Icons.Rounded.Shuffle,
            label = if (event.isShuffleEnabled) "Shuffle On" else "Shuffle",
            isActive = event.isShuffleEnabled,
            accentColor = accentColor,
            onClick = {
                CapsuleStateManager.keepExpandedAlive()
                com.example.capsulebar.service.HapticSoundManager.playToggle(!event.isShuffleEnabled)
                CapsuleStateManager.sendMediaAction("toggle_shuffle")
            }
        )

        // Repeat Chip (3 states: Off -> Repeat All -> Repeat One -> Off)
        val repeatIcon = when (event.repeatMode) {
            2 -> Icons.Rounded.RepeatOne
            1 -> Icons.Rounded.RepeatOn
            else -> if (event.isRepeatEnabled) Icons.Rounded.RepeatOn else Icons.Rounded.Repeat
        }
        val repeatLabel = when (event.repeatMode) {
            2 -> "Repeat 1"
            1 -> "Repeat All"
            else -> if (event.isRepeatEnabled) "Repeat All" else "Repeat Off"
        }
        val isRepeatActive = event.repeatMode > 0 || event.isRepeatEnabled

        MediaActionChip(
            icon = repeatIcon,
            label = repeatLabel,
            isActive = isRepeatActive,
            accentColor = accentColor,
            onClick = {
                CapsuleStateManager.keepExpandedAlive()
                com.example.capsulebar.service.HapticSoundManager.playClick()
                CapsuleStateManager.sendMediaAction("toggle_repeat")
            }
        )

        // Stream Playback Speed Chip (1.0x -> 1.25x -> 1.5x -> 2.0x -> 0.75x)
        val speedLabel = if (event.playbackSpeed % 1f == 0f) {
            "${event.playbackSpeed.toInt()}.0x"
        } else {
            "${event.playbackSpeed}x"
        }
        MediaActionChip(
            icon = Icons.Rounded.Speed,
            label = speedLabel,
            isActive = event.playbackSpeed != 1.0f,
            accentColor = accentColor,
            onClick = {
                CapsuleStateManager.keepExpandedAlive()
                com.example.capsulebar.service.HapticSoundManager.playClick()
                CapsuleStateManager.sendMediaAction("cycle_speed")
            }
        )

        // In-Capsule Volume / Audio Output Chip
        MediaActionChip(
            icon = if (event.audioOutputName.contains("Speaker", ignoreCase = true)) Icons.Rounded.Speaker else Icons.Rounded.Headphones,
            label = if (showVolumeControl) "Hide Vol" else "Volume",
            isActive = showVolumeControl,
            accentColor = accentColor,
            onClick = {
                CapsuleStateManager.keepExpandedAlive()
                com.example.capsulebar.service.HapticSoundManager.playClick()
                musicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                showVolumeControl = !showVolumeControl
            }
        )

        // App-Specific Custom Actions from MediaSession / Notification (Spotify, YT Music, YouTube, Pocket FM, etc.)
        event.customActions.forEach { customAction ->
            MediaActionChip(
                icon = Icons.Rounded.AutoAwesome,
                label = customAction.label,
                isActive = false,
                accentColor = accentColor,
                onClick = {
                    CapsuleStateManager.keepExpandedAlive()
                    com.example.capsulebar.service.HapticSoundManager.playClick()
                    val cmd = if (customAction.isNotificationAction) {
                        "notif_action:${customAction.id}"
                    } else {
                        "custom:${customAction.id}"
                    }
                    CapsuleStateManager.sendMediaAction(cmd)
                }
            )
        }
    }
}

@Composable
private fun MediaActionChip(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isActive) accentColor.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                color = if (isActive) accentColor.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) accentColor else Color.White.copy(alpha = 0.80f),
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            color = if (isActive) accentColor else Color.White.copy(alpha = 0.85f),
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun BatteryExpandedCard(event: CapsuleEvent.Battery, context: Context, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    val batteryDetails = remember(event.level, event.isCharging) {
        val sticky = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val tempTenths = sticky?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val voltageMv = sticky?.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val plugged = sticky?.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val sourceLabel = when (plugged) {
            android.os.BatteryManager.BATTERY_PLUGGED_AC -> "AC Power"
            android.os.BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            android.os.BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> if (event.isCharging) "Charging" else "On Battery"
        }
        val tempStr = if (tempTenths > 0) "%.1f°C".format(tempTenths / 10f) else "--°C"
        val voltStr = if (voltageMv > 0) {
            val v = if (voltageMv > 100) voltageMv / 1000f else voltageMv.toFloat()
            "%.2fV".format(v)
        } else "--V"
        Triple(sourceLabel, tempStr, voltStr)
    }
    val (sourceLabel, tempStr, voltStr) = batteryDetails
    val accentColor = if (event.isCharging) Color(0xFF81C784) else if (event.isLow) Color(0xFFEF5350) else Color(0xFF90CAF9)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (event.isCharging) "Charging • $sourceLabel" else if (event.isLow) "Low Battery Alert" else "Battery Status",
                    color = if (event.isLow) Color(0xFFEF5350) else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Temp: $tempStr  •  Voltage: $voltStr",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 11.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(accentColor.copy(alpha = 0.16f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (event.isCharging) Icons.Rounded.BatteryChargingFull else if (event.isLow) Icons.Rounded.BatteryAlert else Icons.Rounded.BatteryFull,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "${event.level}%",
                        color = accentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        MaterialYouProgressBar(
            progress = (event.level / 100f).coerceIn(0f, 1f),
            color = accentColor,
            trackColor = accentColor.copy(alpha = 0.2f),
            height = 6.dp,
            showThumb = false
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    safeStartActivity(context, Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS))
                    CapsuleStateManager.collapseToCompact()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1EFFFFFF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Rounded.BatterySaver, null, tint = accentColor, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Battery Saver", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    safeStartActivity(context, Intent(Intent.ACTION_POWER_USAGE_SUMMARY))
                    CapsuleStateManager.collapseToCompact()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1EFFFFFF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Rounded.Settings, null, tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Usage Stats", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BluetoothExpandedCard(event: CapsuleEvent.Bluetooth, context: Context, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback, settings: CapsuleSettings) {
    val imagePath = settings.bluetoothImagePath
    val bitmap = if (imagePath != null) {
        remember(imagePath) {
            try {
                android.graphics.BitmapFactory.decodeFile(imagePath)
            } catch (e: Exception) {
                null
            }
        }
    } else {
        null
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color(0xFF90CAF9), CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x22A8C7FA)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Headphones,
                            null,
                            tint = Color(0xFF90CAF9),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = event.deviceName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (event.batteryLevel >= 0) "Connected • Battery ${event.batteryLevel}%" else "Connected • Audio Active",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }

            if (event.batteryLevel >= 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x22A8C7FA))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Battery5Bar,
                            null,
                            tint = Color(0xFF90CAF9),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${event.batteryLevel}%",
                            color = Color(0xFF90CAF9),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        if (event.batteryLevel >= 0) {
            Spacer(Modifier.height(8.dp))
            MaterialYouProgressBar(
                progress = (event.batteryLevel / 100f).coerceIn(0f, 1f),
                color = Color(0xFF90CAF9),
                trackColor = Color(0x22A8C7FA),
                height = 5.dp,
                showThumb = false
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    try {
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x22A8C7FA)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Rounded.VolumeUp, null, tint = Color(0xFF90CAF9), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text("Volume", color = Color(0xFF90CAF9), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    safeStartActivity(context, Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
                    CapsuleStateManager.collapseToCompact()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1EFFFFFF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Rounded.Settings, null, tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text("Devices", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TimerExpandedCard(event: CapsuleEvent.Timer) {
    val context = LocalContext.current
    val accentColor = if (event.isFiring) Color(0xFFFF5252) else Color(0xFFFFCA28)
    val totalSec = event.totalSeconds.coerceAtLeast(event.remainingSeconds.coerceAtLeast(1L))
    val progress = if (event.isFiring) 1f else (event.remainingSeconds.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── 1. HEADER: LABEL + SYNC STATUS + COUNTDOWN READOUT + PRIMARY CONTROLS ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.label.ifEmpty { "Timer" }.uppercase(),
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    event.isFiring -> Color(0x33FF5252)
                                    event.isSystemSynced -> Color(0x2881C784)
                                    else -> Color(0x1FFFFFFF)
                                }
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = when {
                                event.isFiring -> "TIME'S UP!"
                                event.isSystemSynced -> "CLOCK SYNCED"
                                event.isRunning -> "RUNNING"
                                else -> "PAUSED"
                            },
                            color = when {
                                event.isFiring -> Color(0xFFFF5252)
                                event.isSystemSynced -> Color(0xFF81C784)
                                else -> Color(0xFFFFE082)
                            },
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Text(
                    text = if (event.isFiring) "00:00" else formatTime(event.remainingSeconds),
                    color = accentColor,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Play / Pause Button
                IconButton(
                    onClick = {
                        com.example.capsulebar.service.HapticSoundManager.playToggle(!event.isRunning)
                        CapsuleStateManager.sendTimerAction(context, event.id, "toggle")
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .background(accentColor, CircleShape)
                ) {
                    Icon(
                        imageVector = if (event.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (event.isRunning) "Pause" else "Start",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Reset Button
                IconButton(
                    onClick = {
                        com.example.capsulebar.service.HapticSoundManager.playClick()
                        CapsuleStateManager.sendTimerAction(context, event.id, "reset")
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0x16FFFFFF), CircleShape)
                ) {
                    Icon(Icons.Rounded.Refresh, "Reset", tint = Color.White, modifier = Modifier.size(17.dp))
                }

                // Open System Clock App Button
                IconButton(
                    onClick = {
                        com.example.capsulebar.service.HapticSoundManager.playClick()
                        CapsuleStateManager.sendTimerAction(context, event.id, "open_clock")
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0x16FFFFFF), CircleShape)
                ) {
                    Icon(Icons.Rounded.OpenInNew, "Open Clock App", tint = Color(0xFFFFE082), modifier = Modifier.size(16.dp))
                }

                // Stop / Dismiss Button
                IconButton(
                    onClick = {
                        com.example.capsulebar.service.HapticSoundManager.playCollapse()
                        CapsuleStateManager.sendTimerAction(context, event.id, "stop")
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0x22F2B8B5), CircleShape)
                ) {
                    Icon(Icons.Rounded.Close, "Stop Timer", tint = Color(0xFFE57373), modifier = Modifier.size(17.dp))
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // ── 2. COUNTDOWN PROGRESS BAR ──
        MaterialYouProgressBar(
            progress = progress,
            color = accentColor,
            trackColor = Color.White.copy(alpha = 0.14f),
            height = 5.dp,
            showThumb = event.isRunning
        )

        Spacer(Modifier.height(7.dp))

        // ── 3. QUICK EXTEND (+1M / +5M) & SYSTEM-SYNCED PRESETS STRIP ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaActionChip(
                icon = Icons.Rounded.Add,
                label = "+1m",
                isActive = true,
                accentColor = accentColor,
                onClick = {
                    com.example.capsulebar.service.HapticSoundManager.playClick()
                    CapsuleStateManager.sendTimerAction(context, event.id, "+60s")
                }
            )
            MediaActionChip(
                icon = Icons.Rounded.Add,
                label = "+5m",
                isActive = true,
                accentColor = accentColor,
                onClick = {
                    com.example.capsulebar.service.HapticSoundManager.playClick()
                    CapsuleStateManager.sendTimerAction(context, event.id, "+300s")
                }
            )

            if (!event.isSystemSynced && event.remainingSeconds > 0L) {
                MediaActionChip(
                    icon = Icons.Rounded.Sync,
                    label = "Sync Clock",
                    isActive = false,
                    accentColor = Color(0xFF81C784),
                    onClick = {
                        com.example.capsulebar.service.HapticSoundManager.playClick()
                        CapsuleStateManager.sendTimerAction(context, event.id, "sync_system")
                    }
                )
            }

            listOf(60L to "1m", 180L to "3m", 300L to "5m", 600L to "10m", 900L to "15m").forEach { (sec, title) ->
                MediaActionChip(
                    icon = Icons.Rounded.Timer,
                    label = title,
                    isActive = event.totalSeconds == sec,
                    accentColor = accentColor,
                    onClick = {
                        com.example.capsulebar.service.HapticSoundManager.playClick()
                        CapsuleStateManager.sendTimerAction(context, event.id, "preset:$sec")
                    }
                )
            }
        }
    }
}

@Composable
private fun NavigationExpandedCard(event: CapsuleEvent.Navigation, context: Context, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x22A8C7FA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Navigation,
                    null,
                    tint = Color(0xFF90CAF9),
                    modifier = Modifier.size(24.dp).graphicsLayer(rotationZ = 45f)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.instruction,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (event.distance.isNotBlank()) "${event.distance} • Active Route" else "Turn-by-turn navigation active",
                    color = Color(0xFF90CAF9),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    com.example.capsulebar.service.CapsuleNotificationListener.triggerNotificationAction(event.id, "exit")
                    com.example.capsulebar.service.CapsuleNotificationListener.dismissNotification(event.id)
                    CapsuleStateManager.removeEvent(event.id)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x22F2B8B5)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Rounded.Cancel, null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Exit Route", color = Color(0xFFEF5350), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val opened = com.example.capsulebar.service.CapsuleNotificationListener.openNotification(context, event.id)
                    if (!opened) {
                        try {
                            val mapIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=navigation"))
                            safeStartActivity(context, mapIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    CapsuleStateManager.collapseToCompact()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1EFFFFFF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Rounded.Navigation, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Open Map", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProgressExpandedCard(event: CapsuleEvent.Progress) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = event.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Open",
                color = Color(0xFF90CAF9),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x22A8C7FA))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        com.example.capsulebar.service.CapsuleNotificationListener.openNotification(context, event.id)
                        CapsuleStateManager.collapseToCompact()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        val progress = if (event.max > 0) (event.progress.toFloat() / event.max).coerceIn(0f, 1f) else 0f
        MaterialYouProgressBar(
            progress = progress,
            color = Color(0xFF90CAF9),
            trackColor = Color(0x22A8C7FA),
            height = 6.dp,
            showThumb = false
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${(progress * 100).toInt()}%",
                color = Color(0xFF90CAF9),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${event.progress}/${event.max}",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun NotificationExpandedCard(event: CapsuleEvent.Notification) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { CapsuleSettings(context) }
    var replyText by remember { mutableStateOf("") }
    var isReplying by remember { mutableStateOf(false) }
    var actionStatusText by remember { mutableStateOf<String?>(null) }
    var otpCopied by remember(event.id) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    DisposableEffect(Unit) {
        onDispose {
            CapsuleStateManager.setInputFocused(false)
        }
    }

    val detectedOtp = remember(event.title, event.text) {
        val combined = "${event.title} ${event.text}"
        val otpContextKeywords = listOf("otp", "code", "verification", "verify", "pin", "passcode", "auth", "login")
        val hasKeyword = otpContextKeywords.any { combined.contains(it, ignoreCase = true) }
        if (hasKeyword) {
            Regex("\\b(\\d{4,8})\\b").find(combined)?.groupValues?.getOrNull(1)
        } else null
    }

    val notificationActions = remember(event.id) {
        com.example.capsulebar.service.CapsuleNotificationListener.getNotificationActions(event.id)
    }

    val iconBitmap = when (settings.iconOption) {
        0 -> event.largeImage ?: event.appIcon
        1 -> event.appIcon
        2 -> event.smallIcon ?: event.appIcon
        else -> event.appIcon
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // App header & content preview
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    com.example.capsulebar.service.CapsuleNotificationListener.openNotification(
                        context,
                        event.id,
                        event.packageName
                    )
                    CapsuleStateManager.collapseToCompact()
                }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x22A8C7FA)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Message, null, tint = Color(0xFF90CAF9))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.appName.uppercase(),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = event.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = event.text,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    maxLines = settings.maxTextLines.coerceAtLeast(2),
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (detectedOtp != null) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (otpCopied) Color(0x3381C784) else Color(0x3390CAF9))
                        .border(1.dp, if (otpCopied) Color(0xFF81C784) else Color(0xFF90CAF9), RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("OTP", detectedOtp))
                            otpCopied = true
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (otpCopied) "Copied ✓" else "Copy $detectedOtp",
                        color = if (otpCopied) Color(0xFF81C784) else Color(0xFF90CAF9),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Show image if available
        if (settings.showImages && event.largeImage != null) {
            Image(
                bitmap = event.largeImage.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        }

        // Action status indicator (Feedback when Mark as Read, Replied, or Muted)
        if (actionStatusText != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x3381C784))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionStatusText ?: "",
                    color = Color(0xFF81C784),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Dedicated Primary Actions: Mark as Read | Reply | Mute
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Mark as Read
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x2281C784))
                    .border(0.8.dp, Color(0x6681C784), RoundedCornerShape(10.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        actionStatusText = "Marked as read ✓"
                        scope.launch {
                            delay(350)
                            com.example.capsulebar.service.CapsuleNotificationListener.markNotificationAsRead(event.id)
                        }
                    }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.DoneAll,
                        contentDescription = null,
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Mark Read",
                        color = Color(0xFF81C784),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 2. Reply (Toggle reply bar / focus)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isReplying) Color(0x3390CAF9) else Color(0x2290CAF9))
                    .border(0.8.dp, Color(0x6690CAF9), RoundedCornerShape(10.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isReplying = !isReplying
                        if (!isReplying) {
                            CapsuleStateManager.setInputFocused(false)
                        }
                    }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.Reply,
                        contentDescription = null,
                        tint = Color(0xFF90CAF9),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Reply",
                        color = Color(0xFF90CAF9),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 3. Mute
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x22FFB74D))
                    .border(0.8.dp, Color(0x66FFB74D), RoundedCornerShape(10.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        actionStatusText = "Muted ✓"
                        scope.launch {
                            delay(350)
                            com.example.capsulebar.service.CapsuleNotificationListener.muteNotification(
                                context,
                                event.id,
                                event.packageName
                            )
                        }
                    }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.NotificationsOff,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Mute",
                        color = Color(0xFFFFB74D),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Secondary / System Notification Actions (e.g., custom actions provided by the app)
        if (notificationActions.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(notificationActions.size) { idx ->
                    val actionTitle = notificationActions[idx]
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x18FFFFFF))
                            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                com.example.capsulebar.service.CapsuleNotificationListener.triggerNotificationAction(event.id, actionTitle)
                                CapsuleStateManager.removeEvent(event.id)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(actionTitle, color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Reply field & Quick Reply Chips
        if (isReplying || settings.sendReplies) {
            val chipBg = if (settings.addBackground) Color(0x33FFFFFF) else Color(0x18FFFFFF)
            val quickChips = listOf("OK", "On my way", "Yes", "No", "Thanks", "Talk later")
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(quickChips.size) { idx ->
                    val chip = quickChips[idx]
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipBg)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                replyText = chip
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(chip, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Type reply...", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0x18FFFFFF),
                        unfocusedContainerColor = Color(0x12FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .onFocusChanged { focusState ->
                            CapsuleStateManager.setInputFocused(focusState.isFocused)
                        },
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (replyText.isNotEmpty()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            CapsuleStateManager.setInputFocused(false)
                            actionStatusText = "Reply sent ✓"
                            val currentText = replyText
                            replyText = ""
                            scope.launch {
                                delay(300)
                                com.example.capsulebar.service.CapsuleNotificationListener.replyToNotification(event.id, currentText)
                                CapsuleStateManager.removeEvent(event.id)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF90CAF9), CircleShape)
                ) {
                    Icon(Icons.Rounded.Send, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        CapsuleStateManager.setInputFocused(false)
                        com.example.capsulebar.service.CapsuleNotificationListener.dismissNotification(event.id)
                        CapsuleStateManager.removeEvent(event.id)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0x22F2B8B5), CircleShape)
                ) {
                    Icon(Icons.Rounded.Delete, null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AuthenticationExpandedCard(event: CapsuleEvent.Authentication) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(if (event.success) Color(0x11C4E1A5) else Color(0x11F2B8B5))
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (event.success) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                    contentDescription = null,
                    tint = if (event.success) Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (event.success) "${event.label} Approved" else "${event.label} Failed",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CallExpandedCard(event: CapsuleEvent.Call) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var elapsedSeconds by remember(event.id) {
        val parsed = Regex("(\\d+):(\\d+)").find(event.durationText)
        val initial = if (parsed != null) {
            val (m, s) = parsed.destructured
            (m.toLongOrNull() ?: 0L) * 60 + (s.toLongOrNull() ?: 0L)
        } else 0L
        mutableStateOf(initial)
    }
    LaunchedEffect(event.id, event.isIncoming) {
        if (!event.isIncoming) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                elapsedSeconds++
            }
        }
    }
    val callDurationStr = if (elapsedSeconds > 0) {
        val m = elapsedSeconds / 60
        val s = elapsedSeconds % 60
        "%02d:%02d".format(m, s)
    } else {
        event.durationText.ifEmpty { "00:00" }
    }
    var isMuted by remember { mutableStateOf(audioManager.isMicrophoneMute) }
    @Suppress("DEPRECATION")
    var isSpeaker by remember { mutableStateOf(audioManager.isSpeakerphoneOn) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    com.example.capsulebar.service.CapsuleNotificationListener.openNotification(context, event.id)
                    CapsuleStateManager.collapseToCompact()
                }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x22C4E1A5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Person, null, tint = Color(0xFF81C784), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.contactName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (event.isIncoming) "Incoming Call • Tap to open" else "Active Call • Tap to open",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 11.sp
                )
            }
            if (!event.isIncoming) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x22C4E1A5))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = callDurationStr,
                        color = Color(0xFF81C784),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        if (event.isIncoming) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        com.example.capsulebar.service.HapticSoundManager.playClick()
                        com.example.capsulebar.service.CapsuleNotificationListener.triggerNotificationAction(event.id, "answer")
                        CapsuleStateManager.postEvent(
                            event.copy(isIncoming = false, durationText = "00:00")
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Icon(Icons.Rounded.Call, null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("Accept", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        com.example.capsulebar.service.HapticSoundManager.playHeavy()
                        com.example.capsulebar.service.CapsuleNotificationListener.triggerNotificationAction(event.id, "decline")
                        com.example.capsulebar.service.CapsuleNotificationListener.dismissNotification(event.id)
                        CapsuleStateManager.removeEvent(event.id)
                        com.example.capsulebar.service.HapticSoundManager.playCollapse()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Icon(Icons.Rounded.CallEnd, null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("Decline", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        com.example.capsulebar.service.HapticSoundManager.playToggle(isMuted)
                        try {
                            audioManager.isMicrophoneMute = isMuted
                        } catch (_: Exception) {}
                        com.example.capsulebar.service.CapsuleNotificationListener.triggerNotificationAction(event.id, "mute")
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .background(if (isMuted) Color(0x44C62828) else Color(0x16FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                        contentDescription = "Mute",
                        tint = if (isMuted) Color(0xFFEF5350) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = {
                        isSpeaker = !isSpeaker
                        com.example.capsulebar.service.HapticSoundManager.playToggle(isSpeaker)
                        try {
                            @Suppress("DEPRECATION")
                            audioManager.isSpeakerphoneOn = isSpeaker
                        } catch (_: Exception) {}
                        com.example.capsulebar.service.CapsuleNotificationListener.triggerNotificationAction(event.id, "speaker")
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .background(if (isSpeaker) Color(0x332E7D32) else Color(0x16FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isSpeaker) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeDown,
                        contentDescription = "Speaker",
                        tint = if (isSpeaker) Color(0xFF81C784) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        com.example.capsulebar.service.CapsuleNotificationListener.openNotification(context, event.id)
                        CapsuleStateManager.collapseToCompact()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0x16FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = "Open Call",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = {
                        com.example.capsulebar.service.HapticSoundManager.playHeavy()
                        com.example.capsulebar.service.CapsuleNotificationListener.triggerNotificationAction(event.id, "end")
                        com.example.capsulebar.service.CapsuleNotificationListener.dismissNotification(event.id)
                        CapsuleStateManager.removeEvent(event.id)
                        com.example.capsulebar.service.HapticSoundManager.playCollapse()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFFC62828), CircleShape)
                ) {
                    Icon(Icons.Rounded.Call, null, tint = Color.Black, modifier = Modifier.size(20.dp).graphicsLayer(rotationZ = 135f))
                }
            }
        }
    }
}

@Composable
private fun RecordingExpandedCard(event: CapsuleEvent.Recording) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var elapsedSeconds by remember(event.id) {
        val parsed = Regex("(\\d+):(\\d+)").find(event.durationText)
        val initial = if (parsed != null) {
            val (m, s) = parsed.destructured
            (m.toLongOrNull() ?: 0L) * 60 + (s.toLongOrNull() ?: 0L)
        } else 0L
        mutableStateOf(initial)
    }
    LaunchedEffect(event.id) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            elapsedSeconds++
            val m = elapsedSeconds / 60
            val s = elapsedSeconds % 60
            CapsuleStateManager.postEvent(event.copy(durationText = "%02d:%02d".format(m, s)))
        }
    }
    val durationStr = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x22F2B8B5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (event.type == "screen") Icons.Rounded.FiberManualRecord else Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (event.type == "screen") "Screen Recording" else "Voice Recording",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Recording in progress", color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoiceWaveVisualizer()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x22F2B8B5))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = durationStr,
                        color = Color(0xFFEF5350),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    com.example.capsulebar.service.CapsuleNotificationListener.openNotification(context, event.id)
                    CapsuleStateManager.collapseToCompact()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1EFFFFFF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Rounded.OpenInNew, null, tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Open Recorder", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    com.example.capsulebar.service.CapsuleNotificationListener.triggerNotificationAction(event.id, "stop")
                    com.example.capsulebar.service.CapsuleNotificationListener.dismissNotification(event.id)
                    CapsuleStateManager.removeEvent(event.id)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x22F2B8B5)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Rounded.Stop, null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Stop Recording", color = Color(0xFFEF5350), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HotspotExpandedCard(event: CapsuleEvent.Hotspot, context: Context, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x22A8C7FA)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WifiTethering,
                        contentDescription = null,
                        tint = Color(0xFF90CAF9),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Personal Hotspot Active", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("${event.connections} Connected devices", color = Color(0xFF90CAF9), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Data Shared: 450 MB", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                Text("Limit: 2.0 GB", color = Color(0xFF90CAF9), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            MaterialYouProgressBar(
                progress = 0.225f,
                color = Color(0xFF90CAF9),
                trackColor = Color(0x22A8C7FA),
                height = 6.dp,
                showThumb = false
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    CapsuleStateManager.removeEvent(event.id)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x22F2B8B5)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Rounded.WifiTethering, null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Turn Off", color = Color(0xFFEF5350), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    try {
                        val tetherIntent = Intent().setClassName("com.android.settings", "com.android.settings.TetherSettings")
                        safeStartActivity(context, tetherIntent)
                    } catch (e: Exception) {
                        val tetherIntent = Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
                        safeStartActivity(context, tetherIntent)
                    }
                    CapsuleStateManager.collapseToCompact()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1EFFFFFF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Rounded.Settings, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Configure", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DeliveryExpandedCard(event: CapsuleEvent.Delivery) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val icon = when (event.appName.lowercase()) {
        "uber", "ola", "rapido", "ride sharing", "cab" -> Icons.Rounded.DirectionsCar
        else -> Icons.Rounded.DeliveryDining
    }
    val stageLabel = when {
        event.progress >= 0.85f -> "Arriving Soon"
        event.progress >= 0.55f -> "On the Way"
        event.progress >= 0.30f -> "Preparing"
        else -> "Confirmed"
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFE082)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color(0xFFFFB74D), modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${event.appName} • $stageLabel",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = event.statusText,
                        color = Color(0xFFFFB74D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x22FFE082))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        com.example.capsulebar.service.CapsuleNotificationListener.openNotification(context, event.id)
                        CapsuleStateManager.collapseToCompact()
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("Track", color = Color(0xFFFFB74D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        MaterialYouProgressBar(
            progress = event.progress.coerceIn(0f, 1f),
            color = Color(0xFFFFB74D),
            trackColor = Color(0x22FFE082),
            height = 6.dp,
            showThumb = false
        )
    }
}

@Composable
private fun SystemToggleExpandedCard(event: CapsuleEvent.SystemToggle, context: Context, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    val nameLower = event.name.lowercase()
    val isFlashlight = nameLower == "flashlight"
    val isWifi = nameLower == "wi-fi" || nameLower == "wifi"
    val isBluetooth = nameLower == "bluetooth"
    val isDnd = nameLower == "dnd" || nameLower.contains("do not disturb")
    val isSound = nameLower in listOf("silent", "ring", "vibrate", "sound")
    val isHotspot = nameLower == "hotspot" || nameLower.contains("tether")
    val isAirplane = nameLower == "airplane" || nameLower.contains("flight")
    val isLowPower = nameLower.contains("low power") || nameLower.contains("battery saver")
    val isMobileData = nameLower.contains("data") || nameLower.contains("cellular")

    val icon = when {
        isWifi -> Icons.Rounded.Wifi
        isFlashlight -> Icons.Rounded.FlashlightOn
        isBluetooth -> Icons.Rounded.Bluetooth
        isDnd -> Icons.Rounded.DoNotDisturbOn
        nameLower == "silent" -> Icons.Rounded.VolumeOff
        nameLower == "vibrate" -> Icons.Rounded.Vibration
        nameLower == "ring" || isSound -> Icons.Rounded.VolumeUp
        isHotspot -> Icons.Rounded.WifiTethering
        isAirplane -> Icons.Rounded.AirplaneTicket
        isLowPower -> Icons.Rounded.BatterySaver
        else -> Icons.Rounded.Settings
    }
    val toggleColor = if (event.isEnabled) Color(0xFF81C784) else Color.White.copy(alpha = 0.5f)
    val toggleBg = if (event.isEnabled) Color(0x22C4E1A5) else Color(0x11FFFFFF)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(toggleBg)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            CapsuleStateManager.keepExpandedAlive()
                            if (isFlashlight) {
                                val nextState = !event.isEnabled
                                if (nextState) {
                                    FlashlightController.turnOn(context, FlashlightController.intensityFlow.value)
                                    CapsuleStateManager.postEvent(event.copy(id = "toggle_flashlight", isEnabled = true, durationMs = 0))
                                } else {
                                    FlashlightController.turnOff(context)
                                    CapsuleStateManager.removeEvent("toggle_flashlight")
                                    CapsuleStateManager.removeEvent(event.id)
                                }
                            } else {
                                CapsuleStateManager.postEvent(event.copy(isEnabled = !event.isEnabled, durationMs = 0))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = toggleColor, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(event.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    val statusSubtitle = when {
                        isFlashlight && event.isEnabled -> {
                            val curInt = (FlashlightController.intensityFlow.value * 100).toInt()
                            if (FlashlightController.isSosActiveFlow.value) "SOS Strobe Active" else "Torch Active • $curInt%"
                        }
                        isWifi && event.isEnabled -> "Wi-Fi Active"
                        isBluetooth && event.isEnabled -> "Bluetooth Active"
                        isDnd && event.isEnabled -> "DND Enabled"
                        isHotspot && event.isEnabled -> "Hotspot Broadcasting"
                        event.isEnabled -> "Enabled"
                        else -> "Disabled"
                    }
                    Text(statusSubtitle, color = toggleColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Switch(
                checked = event.isEnabled,
                onCheckedChange = { nextState ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    CapsuleStateManager.keepExpandedAlive()
                    if (isFlashlight) {
                        if (nextState) {
                            FlashlightController.turnOn(context, FlashlightController.intensityFlow.value)
                            CapsuleStateManager.postEvent(event.copy(id = "toggle_flashlight", isEnabled = true, durationMs = 0))
                        } else {
                            FlashlightController.turnOff(context)
                            CapsuleStateManager.removeEvent("toggle_flashlight")
                            CapsuleStateManager.removeEvent(event.id)
                        }
                    } else {
                        CapsuleStateManager.postEvent(event.copy(isEnabled = nextState, durationMs = 0))
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = toggleColor
                )
            )
        }

        // ── FLASHLIGHT CONTROLS ──
        if (isFlashlight && event.isEnabled) {
            Spacer(Modifier.height(10.dp))
            val intensity by FlashlightController.intensityFlow.collectAsStateWithLifecycle(initialValue = 0.5f)
            val isSosActive by FlashlightController.isSosActiveFlow.collectAsStateWithLifecycle(initialValue = false)

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Torch Intensity", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("${(intensity * 100).toInt()}%", color = Color(0xFFFFD54F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Decrease",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp).clickable {
                            CapsuleStateManager.keepExpandedAlive()
                            val newIntensity = (intensity - 0.1f).coerceAtLeast(0.1f)
                            FlashlightController.setIntensity(context, newIntensity)
                        }
                    )
                    Slider(
                        value = intensity,
                        onValueChange = {
                            CapsuleStateManager.keepExpandedAlive()
                            FlashlightController.setIntensity(context, it)
                        },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD54F),
                            activeTrackColor = Color(0xFFFFD54F),
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Increase",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp).clickable {
                            CapsuleStateManager.keepExpandedAlive()
                            val newIntensity = (intensity + 0.1f).coerceIn(0.1f, 1.0f)
                            FlashlightController.setIntensity(context, newIntensity)
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))
                // Quick Intensity Presets & SOS strobe mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val presets = listOf(0.25f to "25%", 0.50f to "50%", 0.75f to "75%", 1.0f to "MAX")
                    presets.forEach { (pct, label) ->
                        val isSelected = kotlin.math.abs(intensity - pct) < 0.05f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFFFD54F) else Color(0x1AFFFFFF))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    CapsuleStateManager.keepExpandedAlive()
                                    FlashlightController.setIntensity(context, pct)
                                }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // SOS Mode Button
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSosActive) Color(0xFFFF5252) else Color(0x22FF5252))
                            .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                CapsuleStateManager.keepExpandedAlive()
                                FlashlightController.toggleSos(context)
                            }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSosActive) "SOS ON" else "SOS",
                            color = if (isSosActive) Color.White else Color(0xFFFF8A80),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        } else if (isWifi) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        CapsuleStateManager.keepExpandedAlive()
                        safeStartActivity(context, Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x2281C784)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Rounded.Wifi, null, tint = Color(0xFF81C784), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Wi-Fi Settings", color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        CapsuleStateManager.keepExpandedAlive()
                        safeStartActivity(context, Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Network Panel", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        } else if (isBluetooth) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        CapsuleStateManager.keepExpandedAlive()
                        safeStartActivity(context, Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x2290CAF9)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Rounded.Bluetooth, null, tint = Color(0xFF90CAF9), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Bluetooth Settings", color = Color(0xFF90CAF9), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else if (isDnd) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        CapsuleStateManager.keepExpandedAlive()
                        safeStartActivity(context, Intent(android.provider.Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFB74D)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Rounded.DoNotDisturbOn, null, tint = Color(0xFFFFB74D), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("DND Policy Settings", color = Color(0xFFFFB74D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else if (isSound) {
            Spacer(Modifier.height(8.dp))
            val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
            val profiles = listOf("Ring", "Vibrate", "Silent")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                profiles.forEach { p ->
                    val active = nameLower == p.lowercase()
                    val activeColor = when (p) {
                        "Silent" -> Color(0xFFE57373)
                        "Vibrate" -> Color(0xFF90CAF9)
                        else -> Color(0xFF81C784)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) activeColor.copy(alpha = 0.25f) else Color(0x14FFFFFF))
                            .border(1.dp, if (active) activeColor else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                CapsuleStateManager.keepExpandedAlive()
                                try {
                                    audioManager.ringerMode = when (p) {
                                        "Silent" -> AudioManager.RINGER_MODE_SILENT
                                        "Vibrate" -> AudioManager.RINGER_MODE_VIBRATE
                                        else -> AudioManager.RINGER_MODE_NORMAL
                                    }
                                } catch (_: Exception) {}
                                CapsuleStateManager.postEvent(event.copy(name = p, isEnabled = true, durationMs = 0))
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(p, color = if (active) activeColor else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (isLowPower) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    CapsuleStateManager.keepExpandedAlive()
                    safeStartActivity(context, Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFB74D)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Rounded.BatterySaver, null, tint = Color(0xFFFFB74D), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Battery Saver Settings", color = Color(0xFFFFB74D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    CapsuleStateManager.keepExpandedAlive()
                    safeStartActivity(context, Intent(android.provider.Settings.ACTION_SETTINGS))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Rounded.Settings, null, tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("System Settings", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun StopwatchExpandedCard(event: CapsuleEvent.Stopwatch) {
    val context = LocalContext.current
    val accentColor = Color(0xFF90CAF9)

    // Smooth centiseconds (.00 - .99) animation while running
    var centiseconds by remember(event.id, event.isRunning) { mutableStateOf(0) }
    LaunchedEffect(event.isRunning, event.elapsedSeconds) {
        if (event.isRunning) {
            val startMs = System.currentTimeMillis()
            while (true) {
                kotlinx.coroutines.delay(40)
                centiseconds = (((System.currentTimeMillis() - startMs) % 1000L) / 10L).toInt().coerceIn(0, 99)
            }
        } else if (event.elapsedSeconds == 0L) {
            centiseconds = 0
        }
    }

    val hours = event.elapsedSeconds / 3600
    val minutes = (event.elapsedSeconds % 3600) / 60
    val secs = event.elapsedSeconds % 60
    val mainTimeStr = if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%02d:%02d".format(minutes, secs)
    }
    val centiStr = ".%02d".format(centiseconds)

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── 1. HEADER: STOPWATCH READOUT + SYNC STATUS + ACTION BUTTONS ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.label.ifBlank { "STOPWATCH" }.uppercase(),
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (event.isRunning) Color(0x2881C784) else Color(0x1FFFFFFF))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = when {
                                event.isSystemSynced && event.isRunning -> "CLOCK SYNCED"
                                event.isRunning -> "RUNNING"
                                else -> "PAUSED"
                            },
                            color = if (event.isRunning) Color(0xFF81C784) else Color.White.copy(alpha = 0.65f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = mainTimeStr,
                        color = accentColor,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = centiStr,
                        color = accentColor.copy(alpha = 0.72f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Lap / Split Button
                IconButton(
                    onClick = {
                        com.example.capsulebar.service.HapticSoundManager.playClick()
                        CapsuleStateManager.sendStopwatchAction(context, event.id, "lap")
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0x1A90CAF9), CircleShape)
                ) {
                    Icon(Icons.Rounded.Flag, "Lap", tint = accentColor, modifier = Modifier.size(18.dp))
                }

                // Start / Pause Hero Button
                IconButton(
                    onClick = {
                        com.example.capsulebar.service.HapticSoundManager.playToggle(!event.isRunning)
                        CapsuleStateManager.sendStopwatchAction(context, event.id, "toggle")
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .background(accentColor, CircleShape)
                ) {
                    Icon(
                        imageVector = if (event.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (event.isRunning) "Pause" else "Start",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Reset Button
                IconButton(
                    onClick = {
                        com.example.capsulebar.service.HapticSoundManager.playClick()
                        CapsuleStateManager.sendStopwatchAction(context, event.id, "reset")
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0x16FFFFFF), CircleShape)
                ) {
                    Icon(Icons.Rounded.Refresh, "Reset", tint = Color.White, modifier = Modifier.size(17.dp))
                }

                // Open System Clock App Button
                IconButton(
                    onClick = {
                        com.example.capsulebar.service.HapticSoundManager.playClick()
                        CapsuleStateManager.sendStopwatchAction(context, event.id, "open_clock")
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0x16FFFFFF), CircleShape)
                ) {
                    Icon(Icons.Rounded.OpenInNew, "Open Clock App", tint = accentColor, modifier = Modifier.size(16.dp))
                }

                // Stop / Dismiss Button
                IconButton(
                    onClick = {
                        com.example.capsulebar.service.HapticSoundManager.playCollapse()
                        CapsuleStateManager.sendStopwatchAction(context, event.id, "stop")
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0x22F2B8B5), CircleShape)
                ) {
                    Icon(Icons.Rounded.Close, "Close Stopwatch", tint = Color(0xFFE57373), modifier = Modifier.size(17.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── 2. SCROLLABLE LAP SPLITS & SYSTEM CLOCK ACTION STRIP ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (event.laps.isNotEmpty()) {
                event.laps.forEachIndexed { idx, lapText ->
                    MediaActionChip(
                        icon = Icons.Rounded.Flag,
                        label = lapText,
                        isActive = idx == 0,
                        accentColor = accentColor,
                        onClick = {
                            CapsuleStateManager.keepExpandedAlive()
                        }
                    )
                }
            } else {
                MediaActionChip(
                    icon = Icons.Rounded.Flag,
                    label = "Record Lap Split",
                    isActive = false,
                    accentColor = accentColor,
                    onClick = {
                        com.example.capsulebar.service.HapticSoundManager.playClick()
                        CapsuleStateManager.sendStopwatchAction(context, event.id, "lap")
                    }
                )
                MediaActionChip(
                    icon = Icons.Rounded.OpenInNew,
                    label = "Open System Clock",
                    isActive = false,
                    accentColor = accentColor,
                    onClick = {
                        com.example.capsulebar.service.HapticSoundManager.playClick()
                        CapsuleStateManager.sendStopwatchAction(context, event.id, "open_clock")
                    }
                )
            }
        }
    }
}

@Composable
private fun HourlyTrackerExpandedCard(event: CapsuleEvent.HourlyTracker, context: Context) {
    val isSteps = event.trackerName.lowercase().contains("step")
    val prefKey = if (isSteps) "steps_count" else "water_count"
    val defaultCount = if (isSteps) 4500 else 3
    val target = if (isSteps) 6000 else 8
    val trackerPrefs = remember { context.getSharedPreferences("hourly_tracker_prefs", Context.MODE_PRIVATE) }
    var currentCount by remember(event.id) {
        mutableStateOf(trackerPrefs.getInt(prefKey, defaultCount))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x22C4E1A5)),
                contentAlignment = Alignment.Center
            ) {
                val icon = if (isSteps) Icons.Rounded.DirectionsRun else Icons.Rounded.LocalDrink
                Icon(icon, null, tint = Color(0xFF81C784), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.trackerName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isSteps) "$currentCount / $target Steps" else "$currentCount / $target Glasses (${currentCount * 200}ml)",
                    color = Color(0xFF81C784),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (currentCount > 0) {
                            val nextCount = currentCount - if (isSteps) 500 else 1
                            currentCount = nextCount.coerceAtLeast(0)
                            trackerPrefs.edit().putInt(prefKey, currentCount).apply()

                            val newProgress = (currentCount.toFloat() / target).coerceIn(0f, 1f)
                            val newCountText = if (isSteps) "$currentCount / $target Steps" else "$currentCount / $target Glasses (${currentCount * 200}ml)"
                            CapsuleStateManager.postEvent(
                                event.copy(
                                    countText = newCountText,
                                    progress = newProgress
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0x11FFFFFF), CircleShape)
                ) {
                    Icon(Icons.Rounded.Remove, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = {
                        val nextCount = currentCount + if (isSteps) 500 else 1
                        currentCount = nextCount
                        trackerPrefs.edit().putInt(prefKey, currentCount).apply()

                        val newProgress = (currentCount.toFloat() / target).coerceIn(0f, 1f)
                        val newCountText = if (isSteps) "$currentCount / $target Steps" else "$currentCount / $target Glasses (${currentCount * 200}ml)"
                        CapsuleStateManager.postEvent(
                            event.copy(
                                countText = newCountText,
                                progress = newProgress
                            )
                        )
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0x11FFFFFF), CircleShape)
                ) {
                    Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        val currentProgress = (currentCount.toFloat() / target).coerceIn(0f, 1f)
        MaterialYouProgressBar(
            progress = currentProgress,
            color = Color(0xFF81C784),
            trackColor = Color(0x11FFFFFF),
            height = 6.dp,
            showThumb = false
        )
    }
}

@Composable
private fun AlarmExpandedCard(event: CapsuleEvent.Alarm) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFB74D)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Alarm, null, tint = Color(0xFFFFB74D), modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.label.ifEmpty { "Alarm" },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = event.timeText,
                    color = Color(0xFFFFB74D),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    com.example.capsulebar.service.CapsuleNotificationListener.triggerNotificationAction(event.id, "snooze")
                    CapsuleStateManager.removeEvent(event.id)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Text("Snooze", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    com.example.capsulebar.service.CapsuleNotificationListener.triggerNotificationAction(event.id, "dismiss")
                    CapsuleStateManager.removeEvent(event.id)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Text("Dismiss", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CalendarEventExpandedCard(event: CapsuleEvent.CalendarEvent) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x2290CAF9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.CalendarToday, null, tint = Color(0xFF90CAF9), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = event.timeText,
                    color = Color(0xFF90CAF9),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x2290CAF9))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        try {
                            val calIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
                            safeStartActivity(context, calIntent)
                        } catch (_: Exception) {}
                        CapsuleStateManager.collapseToCompact()
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("Open", color = Color(0xFF90CAF9), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (event.location.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(event.location, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun WeatherExpandedCard(event: CapsuleEvent.Weather) {
    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val baseTemp = remember(event.tempText) {
        Regex("(-?\\d+)").find(event.tempText)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 28
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x2280DEEA)),
                contentAlignment = Alignment.Center
            ) {
                val icon = if (event.condition.lowercase().contains("rain")) Icons.Rounded.Umbrella else Icons.Rounded.WbSunny
                Icon(icon, null, tint = Color(0xFF80DEEA), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Current Weather",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${event.tempText} • ${event.condition}",
                    color = Color(0xFF80DEEA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "H: ${baseTemp + 3}° L: ${baseTemp - 4}°",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(12.dp))

        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            items(4) { idx ->
                val hourText = if (idx == 0) "Now" else "%02d:00".format((currentHour + idx) % 24)
                val tempText = when (idx) {
                    0 -> event.tempText
                    1 -> "${baseTemp}°C"
                    2 -> "${baseTemp - 1}°C"
                    else -> "${baseTemp - 2}°C"
                }
                val icon = when (idx) {
                    0 -> if (event.condition.lowercase().contains("rain")) Icons.Rounded.Umbrella else Icons.Rounded.WbSunny
                    1 -> Icons.Rounded.WbSunny
                    2 -> Icons.Rounded.Cloud
                    else -> Icons.Rounded.Umbrella
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Text(hourText, color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp)
                    Spacer(Modifier.height(4.dp))
                    Icon(icon, null, tint = Color(0xFF80DEEA), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(tempText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocalDrink, null, tint = Color(0xFF80DEEA), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Humidity: 68%", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Cloud, null, tint = Color(0xFF80DEEA), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Wind: 12 km/h", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun NetworkExpandedCard(event: CapsuleEvent.Network, context: Context, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback, scope: CoroutineScope) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x2280DEEA)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (event.type == "airplane") Icons.Rounded.AirplaneTicket else Icons.Rounded.Wifi,
                        contentDescription = null,
                        tint = Color(0xFF80DEEA),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Network & Connections", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(event.statusText, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (event.type == "airplane") "Airplane Mode" else "Wi-Fi: Connected", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                Text("Speed: 75 Mbps", color = Color(0xFF80DEEA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            MaterialYouProgressBar(
                progress = 0.65f,
                color = Color(0xFF80DEEA),
                trackColor = Color(0x2280DEEA),
                height = 6.dp,
                showThumb = false
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    CapsuleStateManager.postEvent(event.copy(statusText = "Refreshing connection..."))
                    scope.launch {
                        delay(1000)
                        CapsuleStateManager.postEvent(event.copy(statusText = if (event.type == "airplane") "Airplane Mode ON" else "Wi-Fi: Active"))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1EFFFFFF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Rounded.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Refresh", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val wifiIntent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                    safeStartActivity(context, wifiIntent)
                    CapsuleStateManager.collapseToCompact()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x2280DEEA)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Rounded.Wifi, null, tint = Color(0xFF80DEEA), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Wi-Fi Settings", color = Color(0xFF80DEEA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SoundProfileExpandedCard(event: CapsuleEvent.SoundProfile, context: Context, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var currentProfile by remember(event.profile) { mutableStateOf(event.profile) }

    val maxMediaVol = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    var mediaVol by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }

    val maxRingVol = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_RING).toFloat() }
    var ringVol by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_RING).toFloat()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Sound",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val profiles = listOf("Ring", "Vibrate", "Silent")
                profiles.forEach { p ->
                    val active = currentProfile == p
                    val icon = when (p) {
                        "Silent" -> Icons.Rounded.VolumeOff
                        "Vibrate" -> Icons.Rounded.Vibration
                        else -> Icons.Rounded.VolumeUp
                    }
                    val activeColor = when (p) {
                        "Silent" -> Color(0xFFE57373)
                        "Vibrate" -> Color(0xFF90CAF9)
                        else -> Color(0xFF81C784)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) activeColor.copy(alpha = 0.25f) else Color(0x11FFFFFF))
                            .border(1.dp, if (active) activeColor else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                currentProfile = p
                                CapsuleStateManager.postEvent(event.copy(profile = p))
                                try {
                                    audioManager.ringerMode = when (p) {
                                        "Silent" -> AudioManager.RINGER_MODE_SILENT
                                        "Vibrate" -> AudioManager.RINGER_MODE_VIBRATE
                                        else -> AudioManager.RINGER_MODE_NORMAL
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, null, tint = if (active) activeColor else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(p, color = if (active) activeColor else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Media Volume", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                Text("${(mediaVol / maxMediaVol * 100).toInt()}%", color = Color(0xFF90CAF9), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = mediaVol,
                onValueChange = {
                    mediaVol = it
                    try {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, it.toInt(), 0)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                valueRange = 0f..maxMediaVol,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF90CAF9),
                    activeTrackColor = Color(0xFF90CAF9),
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.height(24.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ringer Volume", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                Text("${(ringVol / maxRingVol * 100).toInt()}%", color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = ringVol,
                onValueChange = {
                    ringVol = it
                    try {
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, it.toInt(), 0)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                valueRange = 0f..maxRingVol,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF81C784),
                    activeTrackColor = Color(0xFF81C784),
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

@Composable
private fun UsbExpandedCard(event: CapsuleEvent.USB, context: Context, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    var selectedMode by remember { mutableStateOf("File Transfer") }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x22C4E1A5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Usb, null, tint = Color(0xFF81C784), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = "USB Connection Settings", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = if (event.isConnected) "USB cable connected" else "USB disconnected", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Internal Storage Shared", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                Text("64% Used", color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            MaterialYouProgressBar(
                progress = 0.64f,
                color = Color(0xFF81C784),
                trackColor = Color(0x22C4E1A5),
                height = 6.dp,
                showThumb = false
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val modes = listOf("File Transfer", "Charge Only", "MIDI")
            modes.forEach { mode ->
                val active = selectedMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) Color(0x22C4E1A5) else Color(0x11FFFFFF))
                        .border(1.dp, if (active) Color(0xFF81C784) else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedMode = mode
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(mode, color = if (active) Color(0xFF81C784) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun LockStateAnimatedIcon(
    isLocked: Boolean,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 16.dp
) {
    val transition = updateTransition(targetState = isLocked, label = "lockStateTransition")

    val shackleRotation by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                spring(dampingRatio = 0.6f, stiffness = 400f)
            } else {
                keyframes {
                    durationMillis = 350
                    0f at 0
                    -26f at 110 using FastOutSlowInEasing
                    6f at 220 using FastOutSlowInEasing
                    0f at 350 using FastOutSlowInEasing
                }
            }
        },
        label = "shackleRotation"
    ) { _ -> 0f }

    val iconScale by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                tween(200)
            } else {
                keyframes {
                    durationMillis = 350
                    1.0f at 0
                    1.25f at 120 using FastOutSlowInEasing
                    0.94f at 240 using FastOutSlowInEasing
                    1.0f at 350
                }
            }
        },
        label = "iconScale"
    ) { _ -> 1f }

    val iconColor by transition.animateColor(
        transitionSpec = { tween(220) },
        label = "iconColor"
    ) { locked ->
        if (locked) Color.White else Color(0xFF81C784)
    }

    Box(
        modifier = modifier
            .size(size + 6.dp)
            .graphicsLayer {
                scaleX = iconScale
                scaleY = iconScale
            },
        contentAlignment = Alignment.Center
    ) {
        if (!isLocked) {
            Box(
                modifier = Modifier
                    .size(size + 4.dp)
                    .clip(CircleShape)
                    .background(Color(0x3381C784))
            )
        }
        Icon(
            imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
            contentDescription = if (isLocked) "Locked" else "Unlocked",
            tint = iconColor,
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    rotationZ = shackleRotation
                }
        )
    }
}

@Composable
private fun LockStateExpandedCard(event: CapsuleEvent.LockState, context: Context, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (event.isLocked) Color(0x22FFFFFF) else Color(0x2281C784)),
                contentAlignment = Alignment.Center
            ) {
                LockStateAnimatedIcon(isLocked = event.isLocked, size = 24.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = "Security State", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = if (event.isLocked) "Device is secured" else "Device is unlocked",
                    color = if (event.isLocked) Color.White.copy(alpha = 0.5f) else Color(0xFF81C784),
                    fontSize = 12.sp,
                    fontWeight = if (event.isLocked) FontWeight.Normal else FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    com.example.capsulebar.service.CapsuleAccessibilityService.lockDeviceScreen()
                    CapsuleStateManager.removeEvent(event.id)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x22F2B8B5)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Rounded.Lock, null, tint = Color(0xFFE57373), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Lock Screen", color = Color(0xFFE57373), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val securityIntent = Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                    safeStartActivity(context, securityIntent)
                    CapsuleStateManager.collapseToCompact()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1EFFFFFF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Rounded.Settings, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Security settings", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QuickAccessAppsBar(
    context: Context,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var ringerMode by remember { mutableStateOf(audioManager.ringerMode) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CAPSULE QUICK HUB",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Flashlight
            QuickActionTile(
                icon = Icons.Rounded.FlashlightOn,
                label = "Torch",
                active = FlashlightController.isFlashlightOn,
                activeColor = Color(0xFFFFD54F),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (FlashlightController.isFlashlightOn) {
                        FlashlightController.turnOff(context)
                        CapsuleStateManager.removeEvent("toggle_flashlight")
                    } else {
                        FlashlightController.turnOn(context, FlashlightController.intensityFlow.value)
                        CapsuleStateManager.postEvent(
                            CapsuleEvent.SystemToggle(
                                id = "toggle_flashlight",
                                name = "Flashlight",
                                isEnabled = true,
                                priority = 750,
                                durationMs = 0
                            )
                        )
                    }
                }
            )

            // 2. Sound Profile Cycle
            val soundIcon = when (ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> Icons.Rounded.VolumeOff
                AudioManager.RINGER_MODE_VIBRATE -> Icons.Rounded.Vibration
                else -> Icons.Rounded.VolumeUp
            }
            val soundLabel = when (ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> "Silent"
                AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                else -> "Ring"
            }
            QuickActionTile(
                icon = soundIcon,
                label = soundLabel,
                active = ringerMode != AudioManager.RINGER_MODE_NORMAL,
                activeColor = Color(0xFF90CAF9),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val nextMode = when (ringerMode) {
                        AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                        AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                        else -> AudioManager.RINGER_MODE_NORMAL
                    }
                    try {
                        audioManager.ringerMode = nextMode
                        ringerMode = nextMode
                    } catch (_: Exception) {}
                }
            )

            // 3. Instant 1m Timer
            QuickActionTile(
                icon = Icons.Rounded.Timer,
                label = "1m Timer",
                active = false,
                activeColor = Color(0xFFFFE082),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    CapsuleStateManager.postEvent(
                        CapsuleEvent.Timer(
                            id = "quick_timer",
                            label = "Quick Timer",
                            remainingSeconds = 60L,
                            isRunning = true
                        )
                    )
                    CapsuleStateManager.setDisplayMode(DisplayMode.COLLAPSED)
                }
            )

            // 4. Instant Stopwatch
            QuickActionTile(
                icon = Icons.Rounded.AvTimer,
                label = "Stopwatch",
                active = false,
                activeColor = Color(0xFF90CAF9),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    CapsuleStateManager.postEvent(
                        CapsuleEvent.Stopwatch(
                            id = "quick_stopwatch",
                            elapsedSeconds = 0L,
                            isRunning = true,
                            label = "Stopwatch"
                        )
                    )
                    CapsuleStateManager.setDisplayMode(DisplayMode.COLLAPSED)
                }
            )

            // 5. Screenshot
            QuickActionTile(
                icon = Icons.Rounded.ContentCut,
                label = "Shot",
                active = false,
                activeColor = Color.White,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    com.example.capsulebar.service.CapsuleAccessibilityService.takeScreenshot()
                    CapsuleStateManager.setDisplayMode(DisplayMode.COLLAPSED)
                }
            )

            // 6. Lock Screen
            QuickActionTile(
                icon = Icons.Rounded.Lock,
                label = "Lock",
                active = false,
                activeColor = Color(0xFFE57373),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    com.example.capsulebar.service.CapsuleAccessibilityService.lockDeviceScreen()
                    CapsuleStateManager.setDisplayMode(DisplayMode.COLLAPSED)
                }
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (active) activeColor.copy(alpha = 0.25f) else Color(0x1EFFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) activeColor else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = if (active) activeColor else Color.White.copy(alpha = 0.75f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}


