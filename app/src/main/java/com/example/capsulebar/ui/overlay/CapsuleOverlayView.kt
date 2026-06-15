package com.example.capsulebar.ui.overlay

import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
            is CapsuleEvent.LockState -> Color(0xFF37474F)
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
    var splitPosition by remember { mutableStateOf(settings.splitPosition) } // "Left" or "Right"

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
                "bluetooth_image_path" -> bluetoothImagePath = settings.bluetoothImagePath
                "camera_position" -> cameraPosition = settings.cameraPosition
                "camera_width_dp" -> cameraWidthDp = settings.cameraWidthDp
                "edge_rounding_percent" -> edgeRoundingPercent = settings.edgeRoundingPercent
                "default_color" -> defaultColor = settings.defaultColor
                "auto_color" -> autoColor = settings.autoColor
                "use_app_colors" -> useAppColors = settings.useAppColors
                "max_popup_width_percent" -> maxPopupWidthPercent = settings.maxPopupWidthPercent
                "quick_animations" -> quickAnimations = settings.quickAnimations
                "split_position" -> splitPosition = settings.splitPosition
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    if (isCalibrationMode) {
        CalibrationScreen(
            settings = settings,
            widthDp = widthDp,
            heightDp = heightDp,
            cornerRadiusDp = cornerRadiusDp,
            cameraPosition = cameraPosition,
            cameraWidthDp = cameraWidthDp
        )
        return
    }

    if (uiState.isHidden) {
        return
    }

    // Read screen width for responsive expanded size
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    // ── SPRING PHYSICS ─────────────────────────────────────────────────────────
    // iOS Dynamic Island uses an underdamped spring (dampingRatio < 1) for the
    // characteristic "overshoot and settle" bounce. StiffnessMediumLow gives the
    // right speed: not too snappy, not sluggish.
    val springSpec = remember(quickAnimations) {
        spring<Float>(
            dampingRatio = if (quickAnimations) 0.50f else 0.42f,
            stiffness = if (quickAnimations) Spring.StiffnessMedium else Spring.StiffnessMediumLow
        )
    }

    // ── IDLE STATE ─────────────────────────────────────────────────────────────
    // When no event is active the capsule rests as a minimal black pill that just
    // covers the camera hardware — exactly like the Dynamic Island at rest.
    val isIdleState = uiState.mainEvent == null
    val idlePillWidthDp = (cameraWidthDp + 24).toFloat()

    // ── EXPANDED DIMENSIONS ────────────────────────────────────────────────────
    val expandedWidthDp = (screenWidthDp * 0.94f * (maxPopupWidthPercent / 100f)).coerceAtLeast(280f)
    val expandedHeightDp = when (val ev = uiState.mainEvent) {
        is CapsuleEvent.Music        -> 260f
        is CapsuleEvent.Call         -> 230f
        is CapsuleEvent.Recording    -> 210f
        is CapsuleEvent.Timer        -> 210f
        is CapsuleEvent.Stopwatch    -> 210f
        is CapsuleEvent.Notification -> 255f
        is CapsuleEvent.Progress     -> 200f
        is CapsuleEvent.Navigation   -> 200f
        is CapsuleEvent.SystemToggle -> if (ev.name.lowercase() == "flashlight") 240f else 210f
        is CapsuleEvent.HourlyTracker -> 200f
        is CapsuleEvent.CalendarEvent -> 200f
        is CapsuleEvent.Weather -> 200f
        is CapsuleEvent.Alarm -> 220f
        else                         -> 210f
    }

    // Width: idle pill ──spring──► compact pill ──spring──► expanded card
    val leftWidthTarget = when (uiState.displayMode) {
        DisplayMode.COLLAPSED -> if (isIdleState) idlePillWidthDp else widthDp.coerceAtLeast(heightDp * 2).toFloat()
        DisplayMode.EXPANDED  -> expandedWidthDp
        DisplayMode.SPLIT     -> widthDp.coerceAtLeast(heightDp * 2).toFloat()
        DisplayMode.HIDDEN    -> idlePillWidthDp
    }
    val leftHeightTarget = when (uiState.displayMode) {
        DisplayMode.COLLAPSED -> heightDp.toFloat()
        DisplayMode.EXPANDED  -> expandedHeightDp
        DisplayMode.SPLIT     -> heightDp.toFloat()
        DisplayMode.HIDDEN    -> heightDp.toFloat()
    }
    // Compact = always fully-rounded pill; Expanded = fixed 28dp card corner (iOS standard)
    val leftRadiusTarget = when (uiState.displayMode) {
        DisplayMode.COLLAPSED -> (heightDp.toFloat() / 2f)
        DisplayMode.EXPANDED  -> 28f
        DisplayMode.SPLIT     -> (heightDp.toFloat() / 2f)
        DisplayMode.HIDDEN    -> (heightDp.toFloat() / 2f)
    }

    val leftWidth  by animateFloatAsState(targetValue = leftWidthTarget,  animationSpec = springSpec, label = "leftWidth")
    val leftHeight by animateFloatAsState(targetValue = leftHeightTarget, animationSpec = springSpec, label = "leftHeight")
    val leftRadius by animateFloatAsState(targetValue = leftRadiusTarget, animationSpec = springSpec, label = "leftRadius")

    // ── ENTRANCE SCALE ANIMATION ───────────────────────────────────────────────
    val enterScale by animateFloatAsState(
        targetValue = if (uiState.isHidden) 0f else 1f,
        animationSpec = spring(dampingRatio = 0.38f, stiffness = Spring.StiffnessMediumLow),
        label = "enterScale"
    )

    var scaleTarget by remember { mutableStateOf(1f) }
    val prevEventId = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState.mainEvent?.id) {
        val newId = uiState.mainEvent?.id
        if (newId != null && newId != prevEventId.value) {
            scaleTarget = 0.88f
            kotlinx.coroutines.delay(55)
            scaleTarget = 1f
            prevEventId.value = newId
        }
    }
    val capsuleScale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
        label = "capsuleScale"
    )

    // 2. Middle Spacer (Only shows in Split mode)
    val spacerWidthTarget = if (uiState.displayMode == DisplayMode.SPLIT) {
        (cameraWidthDp + 16).toFloat()
    } else {
        0f
    }
    val spacerWidth by animateFloatAsState(targetValue = spacerWidthTarget, animationSpec = springSpec, label = "spacerWidth")

    // 3. Right Bubble Dimensions (Split Event)
    val rightWidthTarget = if (uiState.displayMode == DisplayMode.SPLIT) heightDp.toFloat() else 0f
    val rightHeightTarget = if (uiState.displayMode == DisplayMode.SPLIT) heightDp.toFloat() else 0f
    val rightRadiusTarget = if (uiState.displayMode == DisplayMode.SPLIT) (heightDp.toFloat() / 2f) else 0f

    val rightWidth by animateFloatAsState(targetValue = rightWidthTarget, animationSpec = springSpec, label = "rightWidth")
    val rightHeight by animateFloatAsState(targetValue = rightHeightTarget, animationSpec = springSpec, label = "rightHeight")
    val rightRadius by animateFloatAsState(targetValue = rightRadiusTarget, animationSpec = springSpec, label = "rightRadius")

    val scope = rememberCoroutineScope()
    var cutLineStart by remember { mutableStateOf<Offset?>(null) }
    var cutLineEnd by remember { mutableStateOf<Offset?>(null) }
    var isSlashing by remember { mutableStateOf(false) }
    var showCutHint by remember { mutableStateOf(false) }
    var hintJob by remember { mutableStateOf<Job?>(null) }

    @Composable
    fun MainPill() {
        Box(
            modifier = Modifier
                .width(leftWidth.dp)
                .height(leftHeight.dp)
                .graphicsLayer {
                    scaleX = enterScale * capsuleScale
                    scaleY = enterScale * capsuleScale
                }
                .clip(RoundedCornerShape(leftRadius.dp))
                .background(
                    if (uiState.displayMode == DisplayMode.EXPANDED) {
                        Color(0xEE0B0B0B)
                    } else {
                        getCapsuleColor(uiState.mainEvent, defaultColor, autoColor, useAppColors, context)
                    }
                )
                .then(
                    if (uiState.displayMode == DisplayMode.EXPANDED) {
                        Modifier.border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(leftRadius.dp))
                    } else {
                        Modifier
                    }
                )
                // ── SLASH / DRAG GESTURE DETECTOR ──
                .pointerInput(uiState.displayMode) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            if (uiState.displayMode != DisplayMode.EXPANDED) {
                                cutLineStart = startOffset
                                cutLineEnd = startOffset
                                isSlashing = true
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (isSlashing) {
                                cutLineEnd = change.position
                            } else {
                                val dy = dragAmount.y
                                if (uiState.displayMode == DisplayMode.EXPANDED && dy < -8f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    CapsuleStateManager.collapseToCompact()
                                }
                            }
                        },
                        onDragEnd = {
                            if (isSlashing) {
                                val start = cutLineStart
                                val end = cutLineEnd
                                if (start != null && end != null) {
                                    val dist = (end - start).getDistance()
                                    if (dist > 100f) { // Swipe Cut slash threshold
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        CapsuleStateManager.setDisplayMode(DisplayMode.EXPANDED)
                                    } else {
                                        // Tap -> play wiggle animation & show interactive hint
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        scaleTarget = 0.92f
                                        hintJob?.cancel()
                                        hintJob = scope.launch {
                                            showCutHint = true
                                            delay(60)
                                            scaleTarget = 1.06f
                                            delay(60)
                                            scaleTarget = 1f
                                            delay(1800)
                                            showCutHint = false
                                        }
                                    }
                                }
                                cutLineStart = null
                                cutLineEnd = null
                                isSlashing = false
                            }
                        },
                        onDragCancel = {
                            cutLineStart = null
                            cutLineEnd = null
                            isSlashing = false
                        }
                    )
                }
                .padding(horizontal = if (uiState.displayMode == DisplayMode.EXPANDED) 16.dp else 8.dp),
            contentAlignment = Alignment.Center
        ) {
            uiState.mainEvent?.let { event ->
                MainContent(
                    event = event,
                    displayMode = uiState.displayMode,
                    showCutHint = showCutHint
                )
            }

            // Draw glowing slash/cut line during active drag
            if (isSlashing && cutLineStart != null && cutLineEnd != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.9f),
                        start = cutLineStart!!,
                        end = cutLineEnd!!,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }

    @Composable
    fun SplitCircle() {
        if (rightWidth > 0f) {
            Box(
                modifier = Modifier
                    .width(rightWidth.dp)
                    .height(rightHeight.dp)
                    .clip(RoundedCornerShape(rightRadius.dp))
                    .background(getCapsuleColor(uiState.splitEvent, defaultColor, autoColor, useAppColors, context))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        CapsuleStateManager.toggleExpanded()
                    },
                contentAlignment = Alignment.Center
            ) {
                uiState.splitEvent?.let { event ->
                    SplitContent(event = event)
                }
            }
        }
    }

    @Composable
    fun SplitSpacer() {
        if (spacerWidth > 0f) {
            Spacer(modifier = Modifier.width(spacerWidth.dp))
        }
    }

    val hideStatusbarVal = settings.hideStatusbar && uiState.mainEvent != null

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (hideStatusbarVal) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(Color.Black)
            )
        }

        if (splitPosition == "Left") {
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SplitCircle()
                SplitSpacer()
                MainPill()
            }
        } else {
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainPill()
                SplitSpacer()
                SplitCircle()
            }
        }
    }
}

@Composable
private fun MainContent(
    event: CapsuleEvent,
    displayMode: DisplayMode,
    showCutHint: Boolean
) {
    Crossfade(
        targetState = displayMode,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "contentTransition"
    ) { mode ->
        if (mode == DisplayMode.EXPANDED) {
            ExpandedCard(event = event)
        } else {
            // Main capsule always shows its full collapsed content regardless of split mode.
            // Only the side SplitContent composable uses compact dot/icon style.
            CollapsedBubbleContent(event = event, isSplit = false, showCutHint = showCutHint)
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
    showThumb: Boolean = false
) {
    val phaseShift = if (showThumb) {
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

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(if (showThumb) height * 3 else height)
    ) {
        val width = size.width
        val canvasHeight = size.height
        val barHeight = height.toPx()
        val thumbRadius = barHeight * 1.25f
        
        val centerY = canvasHeight / 2
        
        // Draw track
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, centerY - barHeight / 2),
            size = androidx.compose.ui.geometry.Size(width, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2, barHeight / 2)
        )
        
        // Draw active track
        val activeWidth = width * progress.coerceIn(0f, 1f)
        if (activeWidth > 0) {
            if (showThumb) {
                val path = androidx.compose.ui.graphics.Path()
                val waveLength = 20.dp.toPx()
                val waveAmplitude = 3.dp.toPx()
                
                path.moveTo(0f, centerY)
                var x = 0f
                val step = 2f
                while (x <= activeWidth) {
                    val relativeX = (x / waveLength) * (2 * Math.PI.toFloat()) - phaseShift
                    val y = centerY + Math.sin(relativeX.toDouble()).toFloat() * waveAmplitude
                    path.lineTo(x, y)
                    x += step
                }
                
                drawPath(
                    path = path,
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
        
        // Draw thumb
        if (showThumb) {
            drawCircle(
                color = color,
                radius = thumbRadius,
                center = Offset(activeWidth.coerceIn(0f, width), centerY)
            )
        }
    }
}

@Composable
private fun CollapsedBubbleContent(
    event: CapsuleEvent,
    isSplit: Boolean,
    showCutHint: Boolean
) {
    val context = LocalContext.current
    val settings = remember { CapsuleSettings(context) }
    val cameraPosition = settings.cameraPosition
    val cameraWidthDp = settings.cameraWidthDp

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
                    Icon(
                        imageVector = if (event.isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                        contentDescription = null,
                        tint = MaterialYouPastelBlue,
                        modifier = Modifier.size(16.dp)
                    )
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
                        Icon(
                            imageVector = if (event.isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            contentDescription = null,
                            tint = MaterialYouPastelBlue,
                            modifier = Modifier.size(16.dp)
                        )
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
                        MusicVisualizer(isPlaying = event.isPlaying)
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
                        Text(
                            text = if (event.isLocked) "Locked" else "Unlocked",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is CapsuleEvent.Timer -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatTime(event.remainingSeconds),
                                color = MaterialYouPeach,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            RotatingHourglass()
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
                        val m = (event.elapsedSeconds % 3600) / 60
                        val s = event.elapsedSeconds % 60
                        Text(
                            text = "%02d:%02d".format(m, s),
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
                Text(
                    text = "${event.remainingSeconds / 60}m",
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
                    modifier = Modifier.size(12.dp)
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
            else -> {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = MaterialYouWarmGray,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpandedCard(event: CapsuleEvent) {
    val context = LocalContext.current
    val settings = remember { CapsuleSettings(context) }
    val haptic = LocalHapticFeedback.current

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = contentAlpha }
            .padding(top = settings.heightDp.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val dy = dragAmount.y
                    if (dy < -8f) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        CapsuleStateManager.collapseToCompact()
                    }
                }
            },
        verticalArrangement = Arrangement.Center
    ) {
        when (event) {
            is CapsuleEvent.Music -> {
                var currentPosition by remember(event.id, event.position) { mutableStateOf(event.position) }
                LaunchedEffect(event.isPlaying, event.position) {
                    if (event.isPlaying) {
                        while (true) {
                            kotlinx.coroutines.delay(500)
                            currentPosition = (currentPosition + 500).coerceAtMost(event.duration)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (event.albumArt != null) {
                        Image(
                            bitmap = event.albumArt.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialYouLavender),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.MusicNote, null, tint = Color.Black)
                        }
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
                            text = event.artist,
                            color = MaterialYouMutedGray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                val progress = if (event.duration > 0) currentPosition.toFloat() / event.duration else 0.4f
                MaterialYouProgressBar(
                    progress = progress,
                    color = MaterialYouLavender,
                    trackColor = Color(0x33D0BCFF),
                    height = 6.dp,
                    showThumb = true
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { CapsuleStateManager.sendMediaAction("previous") }) {
                        Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White)
                    }
                    IconButton(
                        onClick = {
                            val nextAction = if (event.isPlaying) "pause" else "play"
                            CapsuleStateManager.sendMediaAction(nextAction)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialYouLavender, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (event.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black
                        )
                    }
                    IconButton(onClick = { CapsuleStateManager.sendMediaAction("next") }) {
                        Icon(Icons.Rounded.SkipNext, null, tint = Color.White)
                    }
                }
            }
            is CapsuleEvent.Battery -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (event.isCharging) "Charging..." else if (event.isLow) "Low Battery Alert" else "Battery Status",
                            color = if (event.isLow) MaterialYouCoral else Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${event.level}% Capacity",
                            color = MaterialYouMutedGray,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (event.isCharging) Color(0x22C4E1A5) else if (event.isLow) Color(0x22F2B8B5) else Color(0x22E3E2E6))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (event.isCharging) Icons.Rounded.BatteryChargingFull else if (event.isLow) Icons.Rounded.BatteryAlert else Icons.Rounded.BatteryFull,
                                contentDescription = null,
                                tint = if (event.isCharging) MaterialYouSageGreen else if (event.isLow) MaterialYouCoral else MaterialYouWarmGray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${event.level}%",
                                color = if (event.isCharging) MaterialYouSageGreen else if (event.isLow) MaterialYouCoral else MaterialYouWarmGray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialYouPastelBlue, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22A8C7FA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Bluetooth,
                                    null,
                                    tint = MaterialYouPastelBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = event.deviceName,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (event.batteryLevel >= 0) "Connected" else "Active",
                                color = MaterialYouMutedGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    if (event.batteryLevel >= 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x22A8C7FA))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Battery5Bar,
                                    null,
                                    tint = MaterialYouPastelBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${event.batteryLevel}%",
                                    color = MaterialYouPastelBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            is CapsuleEvent.Timer -> {
                var localRemaining by remember(event.id, event.remainingSeconds) { mutableStateOf(event.remainingSeconds) }
                var timerRunning by remember(event.id, event.isRunning) { mutableStateOf(event.isRunning) }
                
                LaunchedEffect(timerRunning) {
                    if (timerRunning) {
                        while (localRemaining > 0) {
                            delay(1000)
                            localRemaining--
                            CapsuleStateManager.postEvent(
                                event.copy(remainingSeconds = localRemaining, isRunning = true)
                            )
                        }
                        if (localRemaining == 0L) {
                            timerRunning = false
                            CapsuleStateManager.removeEvent(event.id)
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = event.label.ifEmpty { "Timer" }.uppercase(),
                            color = MaterialYouMutedGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = formatTime(localRemaining),
                            color = MaterialYouPeach,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                timerRunning = !timerRunning
                                CapsuleStateManager.postEvent(
                                    event.copy(isRunning = timerRunning, remainingSeconds = localRemaining)
                                )
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x22FFE082), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (timerRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MaterialYouYellow
                            )
                        }
                        IconButton(
                            onClick = {
                                localRemaining = 300L
                                CapsuleStateManager.postEvent(
                                    event.copy(remainingSeconds = 300L, isRunning = timerRunning)
                                )
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x11FFFFFF), CircleShape)
                        ) {
                            Icon(Icons.Rounded.Refresh, null, tint = Color.White)
                        }
                        IconButton(
                            onClick = { CapsuleStateManager.removeEvent(event.id) },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x22F2B8B5), CircleShape)
                        ) {
                            Icon(Icons.Rounded.Close, null, tint = MaterialYouCoral)
                        }
                    }
                }
            }
            is CapsuleEvent.Navigation -> {
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
                            tint = MaterialYouPastelBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = event.instruction,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = event.distance,
                            color = MaterialYouPastelBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            is CapsuleEvent.Progress -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = event.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    val progress = if (event.max > 0) event.progress.toFloat() / event.max else 0f
                    MaterialYouProgressBar(
                        progress = progress,
                        color = MaterialYouPastelBlue,
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
                            color = MaterialYouPastelBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${event.progress}/${event.max}",
                            color = MaterialYouMutedGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            is CapsuleEvent.Notification -> {
                var replyText by remember { mutableStateOf("") }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (event.appIcon != null) {
                            Image(
                                bitmap = event.appIcon.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22A8C7FA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Message, null, tint = MaterialYouPastelBlue)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.appName.uppercase(),
                                color = MaterialYouMutedGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = event.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = event.text,
                                color = MaterialYouWarmGray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val chips = listOf("OK", "On my way", "Yes", "No", "Thanks")
                        chips.forEach { chip ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x1EFFFFFF))
                                    .clickable {
                                        replyText = chip
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(chip, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Reply...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0x12FFFFFF),
                                unfocusedContainerColor = Color(0x12FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(40.dp),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                if (replyText.isNotEmpty()) {
                                    com.example.capsulebar.service.CapsuleNotificationListener.replyToNotification(event.id, replyText)
                                    replyText = ""
                                    CapsuleStateManager.removeEvent(event.id)
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialYouPastelBlue, CircleShape)
                        ) {
                            Icon(Icons.Rounded.Send, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = {
                                com.example.capsulebar.service.CapsuleNotificationListener.dismissNotification(event.id)
                                CapsuleStateManager.removeEvent(event.id)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x22F2B8B5), CircleShape)
                        ) {
                            Icon(Icons.Rounded.Delete, null, tint = MaterialYouCoral, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            is CapsuleEvent.Authentication -> {
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
                                imageVector = if (event.success) Icons.Rounded.VerifiedUser else Icons.Rounded.GppBad,
                                contentDescription = null,
                                tint = if (event.success) MaterialYouSageGreen else MaterialYouCoral,
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
            is CapsuleEvent.Call -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x22C4E1A5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Person, null, tint = MaterialYouSageGreen, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.contactName,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (event.isIncoming) "Incoming Call" else "Active Call",
                                color = MaterialYouMutedGray,
                                fontSize = 12.sp
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
                                    text = event.durationText,
                                    color = MaterialYouSageGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (event.isIncoming) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    com.example.capsulebar.service.CapsuleNotificationListener.triggerNotificationAction(event.id, "answer")
                                    CapsuleStateManager.postEvent(
                                        event.copy(isIncoming = false, durationText = "00:00")
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialYouSageGreen),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Icon(Icons.Rounded.Call, null, tint = Color.Black)
                                Spacer(Modifier.width(8.dp))
                                Text("Accept", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    com.example.capsulebar.service.CapsuleNotificationListener.triggerNotificationAction(event.id, "decline")
                                    CapsuleStateManager.removeEvent(event.id)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialYouCoral),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
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
                                onClick = { },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0x11FFFFFF), CircleShape)
                            ) {
                                Icon(Icons.Rounded.MicOff, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            IconButton(
                                onClick = { },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0x11FFFFFF), CircleShape)
                            ) {
                                Icon(Icons.Rounded.VolumeUp, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            IconButton(
                                onClick = {
                                    com.example.capsulebar.service.CapsuleNotificationListener.dismissNotification(event.id)
                                    CapsuleStateManager.removeEvent(event.id)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialYouCoral, CircleShape)
                            ) {
                                Icon(Icons.Rounded.CallEnd, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
            is CapsuleEvent.Recording -> {
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
                                .background(Color(0x22F2B8B5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (event.type == "screen") Icons.Rounded.FiberManualRecord else Icons.Rounded.Mic,
                                contentDescription = null,
                                tint = MaterialYouCoral,
                                modifier = Modifier.size(24.dp)
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
                            Text("Background activity active", color = MaterialYouMutedGray, fontSize = 11.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x22F2B8B5))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = event.durationText,
                            color = MaterialYouCoral,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            is CapsuleEvent.Hotspot -> {
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
                            imageVector = Icons.Rounded.WifiTethering,
                            contentDescription = null,
                            tint = MaterialYouPastelBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Personal Hotspot Active", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("${event.connections} Connected devices", color = MaterialYouPastelBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            is CapsuleEvent.Delivery -> {
                val icon = when (event.appName.lowercase()) {
                    "uber", "ola", "ride sharing", "cab" -> Icons.Rounded.DirectionsCar
                    else -> Icons.Rounded.DeliveryDining
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
                                .background(Color(0x22FFE082)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null, tint = MaterialYouYellow, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(event.appName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(event.statusText, color = MaterialYouYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    MaterialYouProgressBar(
                        progress = event.progress,
                        color = MaterialYouYellow,
                        trackColor = Color(0x22FFE082),
                        height = 6.dp,
                        showThumb = false
                    )
                }
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
                val toggleColor = if (event.isEnabled) MaterialYouSageGreen else MaterialYouMutedGray
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
                                        if (event.name.lowercase() == "flashlight") {
                                            val nextState = !event.isEnabled
                                            if (nextState) {
                                                FlashlightController.turnOn(context, FlashlightController.intensityFlow.value)
                                                CapsuleStateManager.postEvent(event.copy(isEnabled = true, durationMs = 0))
                                            } else {
                                                FlashlightController.turnOff(context)
                                                CapsuleStateManager.removeEvent(event.id)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null, tint = toggleColor, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(event.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(if (event.isEnabled) "Enabled" else "Disabled", color = toggleColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Switch(
                            checked = event.isEnabled,
                            onCheckedChange = { nextState ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (event.name.lowercase() == "flashlight") {
                                    if (nextState) {
                                        FlashlightController.turnOn(context, FlashlightController.intensityFlow.value)
                                        CapsuleStateManager.postEvent(event.copy(isEnabled = true, durationMs = 0))
                                    } else {
                                        FlashlightController.turnOff(context)
                                        CapsuleStateManager.removeEvent(event.id)
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = toggleColor
                            )
                        )
                    }
                    
                    if (event.name.lowercase() == "flashlight" && event.isEnabled) {
                        Spacer(Modifier.height(16.dp))
                        val intensity by FlashlightController.intensityFlow.collectAsStateWithLifecycle(initialValue = 0.5f)
                        
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Flashlight Intensity", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("${(intensity * 100).toInt()}%", color = toggleColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp).clickable {
                                        val newIntensity = (intensity - 0.1f).coerceAtLeast(0.1f)
                                        FlashlightController.setIntensity(context, newIntensity)
                                    }
                                )
                                Slider(
                                    value = intensity,
                                    onValueChange = { FlashlightController.setIntensity(context, it) },
                                    valueRange = 0.1f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = toggleColor,
                                        activeTrackColor = toggleColor,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp).clickable {
                                        val newIntensity = (intensity + 0.1f).coerceIn(0.1f, 1.0f)
                                        FlashlightController.setIntensity(context, newIntensity)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            is CapsuleEvent.Stopwatch -> {
                var displayElapsed by remember(event.id, event.elapsedSeconds) {
                    mutableStateOf(event.elapsedSeconds)
                }
                LaunchedEffect(event.isRunning, event.elapsedSeconds) {
                    if (event.isRunning) {
                        while (true) {
                            kotlinx.coroutines.delay(1000)
                            displayElapsed++
                            CapsuleStateManager.postEvent(
                                event.copy(elapsedSeconds = displayElapsed, isRunning = true)
                            )
                        }
                    }
                }
                val hours   = displayElapsed / 3600
                val minutes = (displayElapsed % 3600) / 60
                val secs    = displayElapsed % 60
                val timeStr = if (hours > 0) {
                    "%d:%02d:%02d".format(hours, minutes, secs)
                } else {
                    "%02d:%02d".format(minutes, secs)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = event.label.uppercase(),
                            color = MaterialYouMutedGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = timeStr,
                            color = MaterialYouPastelBlue,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            text = if (event.isRunning) "Running" else "Paused",
                            color = if (event.isRunning) MaterialYouSageGreen else MaterialYouMutedGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val nextRunning = !event.isRunning
                                CapsuleStateManager.postEvent(
                                    event.copy(isRunning = nextRunning, elapsedSeconds = displayElapsed)
                                )
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x22A8C7FA), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (event.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MaterialYouPastelBlue
                            )
                        }
                        IconButton(
                            onClick = {
                                displayElapsed = 0L
                                CapsuleStateManager.postEvent(
                                    event.copy(elapsedSeconds = 0L, isRunning = event.isRunning)
                                )
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x11FFFFFF), CircleShape)
                        ) {
                            Icon(Icons.Rounded.Refresh, null, tint = Color.White)
                        }
                        IconButton(
                            onClick = { CapsuleStateManager.removeEvent(event.id) },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x22F2B8B5), CircleShape)
                        ) {
                            Icon(Icons.Rounded.Close, null, tint = MaterialYouCoral)
                        }
                    }
                }
            }
            is CapsuleEvent.HourlyTracker -> {
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
                            Icon(icon, null, tint = MaterialYouSageGreen, modifier = Modifier.size(24.dp))
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
                                color = MaterialYouSageGreen,
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
                        color = MaterialYouSageGreen,
                        trackColor = Color(0x11FFFFFF),
                        height = 6.dp,
                        showThumb = false
                    )
                }
            }
            is CapsuleEvent.Alarm -> {
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
            is CapsuleEvent.CalendarEvent -> {
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
                            Icon(Icons.Rounded.CalendarToday, null, tint = Color(0xFF90CAF9), modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = event.timeText,
                                color = Color(0xFF90CAF9),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (event.location.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocationOn, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(event.location, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }
                }
            }
            is CapsuleEvent.Weather -> {
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
                    }
                }
            }
            else -> {
                Text(
                    text = "Active",
                    color = Color.White,
                    fontSize = 14.sp
                )
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
    settings: CapsuleSettings,
    widthDp: Int,
    heightDp: Int,
    cornerRadiusDp: Int,
    cameraPosition: String,
    cameraWidthDp: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Main Capsule outline
            Box(
                modifier = Modifier
                    .width(widthDp.dp)
                    .height(heightDp.dp)
                    .clip(RoundedCornerShape(cornerRadiusDp.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .border(2.dp, Color(0xFF00D2FF), RoundedCornerShape(cornerRadiusDp.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Draw camera circle inside
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val alignment = when (cameraPosition) {
                        "Left" -> Alignment.CenterStart
                        "Right" -> Alignment.CenterEnd
                        else -> Alignment.Center
                    }
                    val startPadding = if (cameraPosition == "Left") 12.dp else 0.dp
                    val endPadding = if (cameraPosition == "Right") 12.dp else 0.dp
                    
                    Box(
                        modifier = Modifier
                            .align(alignment)
                            .padding(start = startPadding, end = endPadding)
                            .size(cameraWidthDp.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30).copy(alpha = 0.4f))
                            .border(1.5.dp, Color(0xFFFF3B30), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Tiny dot inside camera
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }

                Text(
                    text = "ALIGN CAPSULE & CAMERA",
                    color = Color(0xFF00D2FF),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Calibration visual aid bars (ticks below the capsule)
            Row(
                modifier = Modifier
                    .width(widthDp.dp)
                    .height(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                repeat(7) { index ->
                    val color = if (
                        (index == 0 && cameraPosition == "Left") ||
                        (index == 3 && cameraPosition == "Center") ||
                        (index == 6 && cameraPosition == "Right")
                    ) {
                        Color(0xFFFF3B30) // Match active camera position
                    } else {
                        Color(0xFF00D2FF).copy(alpha = 0.5f)
                    }
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .height(if (index % 3 == 0) 8.dp else 5.dp)
                            .background(color)
                    )
                }
            }
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
