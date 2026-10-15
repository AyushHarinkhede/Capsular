package com.example.capsulebar.ui.main

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import com.example.capsulebar.R
import com.example.capsulebar.data.CapsuleEvent
import com.example.capsulebar.data.CapsuleStateManager
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.composed
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ ->
            viewModel.checkPermissions()
        }
    )

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ ->
            viewModel.checkPermissions()
        }
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ ->
            viewModel.checkPermissions()
        }
    )

    // Observe permission changes and service state on Activity Resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions()
                viewModel.checkServiceStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isOverlayGranted by viewModel.isOverlayPermissionGranted.collectAsStateWithLifecycle()
    val isNotificationGranted by viewModel.isNotificationPermissionGranted.collectAsStateWithLifecycle()
    val isRecordAudioGranted by viewModel.isRecordAudioPermissionGranted.collectAsStateWithLifecycle()
    val isAccessibilityGranted by viewModel.isAccessibilityPermissionGranted.collectAsStateWithLifecycle()
    val isCalendarGranted by viewModel.isCalendarPermissionGranted.collectAsStateWithLifecycle()
    val isLocationGranted by viewModel.isLocationPermissionGranted.collectAsStateWithLifecycle()
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()

    val xOffset by viewModel.xOffset.collectAsStateWithLifecycle()
    val yOffset by viewModel.yOffset.collectAsStateWithLifecycle()
    val widthDp by viewModel.widthDp.collectAsStateWithLifecycle()
    val heightDp by viewModel.heightDp.collectAsStateWithLifecycle()
    val cornerRadiusDp by viewModel.cornerRadiusDp.collectAsStateWithLifecycle()

    val cameraPosition by viewModel.cameraPosition.collectAsStateWithLifecycle()
    val cameraWidthDp by viewModel.cameraWidthDp.collectAsStateWithLifecycle()

    val batteryEnabled by viewModel.isBatteryEnabled.collectAsStateWithLifecycle()
    val musicEnabled by viewModel.isMusicEnabled.collectAsStateWithLifecycle()
    val bluetoothEnabled by viewModel.isBluetoothEnabled.collectAsStateWithLifecycle()
    val timerEnabled by viewModel.isTimerEnabled.collectAsStateWithLifecycle()
    val networkEnabled by viewModel.isNetworkEnabled.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.isNotificationsEnabled.collectAsStateWithLifecycle()
    val isCalibrationMode by viewModel.isCalibrationMode.collectAsStateWithLifecycle()
    val isPositionLocked by viewModel.isPositionLocked.collectAsStateWithLifecycle()
    val bluetoothImagePath by viewModel.bluetoothImagePath.collectAsStateWithLifecycle()

    val compactDurationSec by viewModel.compactDurationSec.collectAsStateWithLifecycle()
    val expandedDurationSec by viewModel.expandedDurationSec.collectAsStateWithLifecycle()
    val dismissDelaySec by viewModel.dismissDelaySec.collectAsStateWithLifecycle()

    // Screen Option State Collections
    val maxPopupWidthPercent by viewModel.maxPopupWidthPercent.collectAsStateWithLifecycle()
    val expandedHeightScale by viewModel.expandedHeightScale.collectAsStateWithLifecycle()
    val edgeRoundingPercent by viewModel.edgeRoundingPercent.collectAsStateWithLifecycle()
    val showAsNotch by viewModel.showAsNotch.collectAsStateWithLifecycle()
    val addBackground by viewModel.addBackground.collectAsStateWithLifecycle()
    val showImages by viewModel.showImages.collectAsStateWithLifecycle()
    val quickAnimations by viewModel.quickAnimations.collectAsStateWithLifecycle()
    val premiumAnimations by viewModel.premiumAnimations.collectAsStateWithLifecycle()
    val reverseOrder by viewModel.reverseOrder.collectAsStateWithLifecycle()
    val maxTextLines by viewModel.maxTextLines.collectAsStateWithLifecycle()
    val defaultColor by viewModel.defaultColor.collectAsStateWithLifecycle()
    val autoColor by viewModel.autoColor.collectAsStateWithLifecycle()
    val useAppColors by viewModel.useAppColors.collectAsStateWithLifecycle()
    val showMusicVisualizer by viewModel.showMusicVisualizer.collectAsStateWithLifecycle()
    val useAndroidMusicControls by viewModel.useAndroidMusicControls.collectAsStateWithLifecycle()
    val iconOption by viewModel.iconOption.collectAsStateWithLifecycle()
    val allowTwoPopups by viewModel.allowTwoPopups.collectAsStateWithLifecycle()
    val autoExpand by viewModel.autoExpand.collectAsStateWithLifecycle()
    val sendReplies by viewModel.sendReplies.collectAsStateWithLifecycle()
    val hideInForeground by viewModel.hideInForeground.collectAsStateWithLifecycle()
    val showInLandscape by viewModel.showInLandscape.collectAsStateWithLifecycle()
    val showAlways by viewModel.showAlways.collectAsStateWithLifecycle()
    val quickAccessApps by viewModel.quickAccessApps.collectAsStateWithLifecycle()
    val showOnLockscreen by viewModel.showOnLockscreen.collectAsStateWithLifecycle()
    val hideOnNotificationPanel by viewModel.hideOnNotificationPanel.collectAsStateWithLifecycle()
    val notificationCountOption by viewModel.notificationCountOption.collectAsStateWithLifecycle()
    val autoHideSmallPopupHours by viewModel.autoHideSmallPopupHours.collectAsStateWithLifecycle()
    val autoHideExpandedPopupSec by viewModel.autoHideExpandedPopupSec.collectAsStateWithLifecycle()
    val hideWhenTouchingOutside by viewModel.hideWhenTouchingOutside.collectAsStateWithLifecycle()
    val splitPosition by viewModel.splitPosition.collectAsStateWithLifecycle()
    val nfcSoundProfileTagId by viewModel.nfcSoundProfileTagId.collectAsStateWithLifecycle()
    val nfcQuickLaunchTagId by viewModel.nfcQuickLaunchTagId.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val soundEffectsEnabled by viewModel.soundEffectsEnabled.collectAsStateWithLifecycle()
    val activeRegistrationTask by viewModel.activeRegistrationTask.collectAsStateWithLifecycle()
    val isGlassEffectEnabled by viewModel.isGlassEffectEnabled.collectAsStateWithLifecycle()
    val glassBlurRadius by viewModel.glassBlurRadius.collectAsStateWithLifecycle()
    val glassEdgeTransparency by viewModel.glassEdgeTransparency.collectAsStateWithLifecycle()
    val glassEffectArea by viewModel.glassEffectArea.collectAsStateWithLifecycle()
    val glassBlackTint by viewModel.glassBlackTint.collectAsStateWithLifecycle()

    val isDarkEffective = when (themeMode) {
        "light" -> false
        "system" -> androidx.compose.foundation.isSystemInDarkTheme()
        else -> true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── TOP LOGO + TITLE BAR (FLOATING / TRANSPARENT) ────────────────────────
        Row(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Capsular logo",
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Capsular",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 28.sp
                )
                Text(
                    text = "Dynamic Punch-Hole Island",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            // Pixel UI Quick Theme Switcher Button (floating semi-transparent)
            IconButton(
                onClick = {
                    val nextTarget = if (isDarkEffective) "light" else "dark"
                    com.example.capsulebar.service.HapticSoundManager.playToggle(!isDarkEffective)
                    viewModel.updateThemeMode(nextTarget)
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Icon(
                    imageVector = if (isDarkEffective) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                    contentDescription = "Toggle Theme",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // ── PERMISSIONS ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            PermissionSection(
                isOverlayGranted = isOverlayGranted,
                isNotificationGranted = isNotificationGranted,
                isRecordAudioGranted = isRecordAudioGranted,
                isAccessibilityGranted = isAccessibilityGranted,
                isCalendarGranted = isCalendarGranted,
                isLocationGranted = isLocationGranted,
                onRequestOverlay = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                },
                onRequestNotification = {
                    val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    context.startActivity(intent)
                },
                onRequestRecordAudio = {
                    recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                },
                onRequestAccessibility = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                },
                onRequestCalendar = {
                    calendarPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
                },
                onRequestLocation = {
                    locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                }
            )
        }

        // ── SERVICE CONTROL SECTION ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            ServiceControlSection(
                isServiceRunning = isServiceRunning,
                isPermissionsGranted = isOverlayGranted,
                onToggleService = {
                    if (!isOverlayGranted) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    } else {
                        viewModel.toggleService()
                    }
                }
            )
        }

        // ── POSITION & SIZE (REMOTE CONTROL HUB) ──────────────────────────────────
        Box(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            PositionSection(
                xOffset = xOffset,
                yOffset = yOffset,
                widthDp = widthDp,
                heightDp = heightDp,
                edgeRoundingPercent = edgeRoundingPercent,
                maxPopupWidthPercent = maxPopupWidthPercent,
                expandedHeightScale = expandedHeightScale,
                cameraPosition = cameraPosition,
                cameraWidthDp = cameraWidthDp,
                isCalibrationMode = isCalibrationMode,
                isPositionLocked = isPositionLocked,
                isServiceRunning = isServiceRunning,
                splitPosition = splitPosition,
                showAsNotch = showAsNotch,
                showAlways = showAlways,
                onXChanged = { viewModel.updateXOffset(it) },
                onYChanged = { viewModel.updateYOffset(it) },
                onWidthChanged = { viewModel.updateWidthDp(it) },
                onHeightChanged = { viewModel.updateHeightDp(it) },
                onEdgeRoundingChanged = { viewModel.updateEdgeRoundingPercent(it) },
                onMaxPopupWidthChanged = { viewModel.updateMaxPopupWidthPercent(it) },
                onExpandedHeightScaleChanged = { viewModel.updateExpandedHeightScale(it) },
                onTestLiveExpand = { viewModel.testLiveExpand() },
                onCameraPositionChanged = { viewModel.updateCameraPosition(it) },
                onCameraWidthChanged = { viewModel.updateCameraWidthDp(it) },
                onToggleCalibration = { viewModel.toggleCalibrationMode(it) },
                onTogglePositionLocked = { viewModel.togglePositionLocked(it) },
                onSplitPositionChanged = { viewModel.updateSplitPosition(it) },
                onShowAsNotchToggle = { viewModel.toggleShowAsNotch(it) },
                onShowAlwaysToggle = { viewModel.toggleShowAlways(it) },
                onReset = {
                    viewModel.updateXOffset(0)
                    viewModel.updateYOffset(15)
                    viewModel.updateWidthDp(110)
                    viewModel.updateHeightDp(36)
                    viewModel.updateCameraWidthDp(28)
                    viewModel.updateEdgeRoundingPercent(60)
                    viewModel.updateMaxPopupWidthPercent(95)
                    viewModel.updateExpandedHeightScale(100)
                }
            )
        }

        // ── APPEARANCE ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            AppearanceSection(
                themeMode = themeMode,
                hapticsEnabled = hapticsEnabled,
                soundEffectsEnabled = soundEffectsEnabled,
                showAsNotch = showAsNotch,
                addBackground = addBackground,
                showImages = showImages,
                quickAnimations = quickAnimations,
                premiumAnimations = premiumAnimations,
                reverseOrder = reverseOrder,
                maxTextLines = maxTextLines,
                defaultColor = defaultColor,
                autoColor = autoColor,
                useAppColors = useAppColors,
                showMusicVisualizer = showMusicVisualizer,
                useAndroidMusicControls = useAndroidMusicControls,
                iconOption = iconOption,
                bluetoothImagePath = bluetoothImagePath,
                onThemeModeChanged = { viewModel.updateThemeMode(it) },
                onHapticsToggle = { viewModel.toggleHaptics(it) },
                onSoundEffectsToggle = { viewModel.toggleSoundEffects(it) },
                onShowAsNotchToggle = { viewModel.toggleShowAsNotch(it) },
                onAddBackgroundToggle = { viewModel.toggleAddBackground(it) },
                onShowImagesToggle = { viewModel.toggleShowImages(it) },
                onQuickAnimationsToggle = { viewModel.toggleQuickAnimations(it) },
                onPremiumAnimationsToggle = { viewModel.togglePremiumAnimations(it) },
                onReverseOrderToggle = { viewModel.toggleReverseOrder(it) },
                onMaxTextLinesChanged = { viewModel.updateMaxTextLines(it) },
                onDefaultColorChanged = { viewModel.updateDefaultColor(it) },
                onAutoColorToggle = { viewModel.toggleAutoColor(it) },
                onUseAppColorsToggle = { viewModel.toggleUseAppColors(it) },
                onShowMusicVisualizerToggle = { viewModel.toggleShowMusicVisualizer(it) },
                onUseAndroidMusicControlsToggle = { viewModel.toggleUseAndroidMusicControls(it) },
                onIconOptionChanged = { viewModel.updateIconOption(it) },
                onUploadBluetoothImage = { path -> viewModel.updateBluetoothImage(path) },
                cameraPosition = cameraPosition,
                onTestLiveExpand = { viewModel.testLiveExpand() },
                isGlassEffectEnabled = isGlassEffectEnabled,
                glassBlurRadius = glassBlurRadius,
                glassEdgeTransparency = glassEdgeTransparency,
                glassEffectArea = glassEffectArea,
                glassBlackTint = glassBlackTint,
                onToggleGlassEffect = { viewModel.toggleGlassEffect(it) },
                onGlassBlurRadiusChanged = { viewModel.updateGlassBlurRadius(it) },
                onGlassEdgeTransparencyChanged = { viewModel.updateGlassEdgeTransparency(it) },
                onGlassEffectAreaChanged = { viewModel.updateGlassEffectArea(it) },
                onGlassBlackTintChanged = { viewModel.updateGlassBlackTint(it) }
            )
        }

        // ── GENERAL ─────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            GeneralSection(
                allowTwoPopups = allowTwoPopups,
                autoExpand = autoExpand,
                sendReplies = sendReplies,
                hideInForeground = hideInForeground,
                showInLandscape = showInLandscape,
                showAlways = showAlways,
                quickAccessApps = quickAccessApps,
                showOnLockscreen = showOnLockscreen,
                hideOnNotificationPanel = hideOnNotificationPanel,
                notificationCountOption = notificationCountOption,
                autoHideSmallPopupHours = autoHideSmallPopupHours,
                autoHideExpandedPopupSec = autoHideExpandedPopupSec,
                hideWhenTouchingOutside = hideWhenTouchingOutside,
                onAllowTwoPopupsToggle = { viewModel.toggleAllowTwoPopups(it) },
                onAutoExpandToggle = { viewModel.toggleAutoExpand(it) },
                onSendRepliesToggle = { viewModel.toggleSendReplies(it) },
                onHideInForegroundToggle = { viewModel.toggleHideInForeground(it) },
                onShowInLandscapeToggle = { viewModel.toggleShowInLandscape(it) },
                onShowAlwaysToggle = { viewModel.toggleShowAlways(it) },
                onQuickAccessAppsToggle = { viewModel.toggleQuickAccessApps(it) },
                onShowOnLockscreenToggle = { viewModel.toggleShowOnLockscreen(it) },
                onHideOnNotificationPanelToggle = { viewModel.toggleHideOnNotificationPanel(it) },
                onNotificationCountOptionChanged = { viewModel.updateNotificationCountOption(it) },
                onAutoHideSmallPopupHoursChanged = { viewModel.updateAutoHideSmallPopupHours(it) },
                onAutoHideExpandedPopupSecChanged = { viewModel.updateAutoHideExpandedPopupSec(it) },
                onHideWhenTouchingOutsideToggle = { viewModel.toggleHideWhenTouchingOutside(it) }
            )
        }

        // ── NFC TASKS ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            NfcTasksSection(
                nfcSoundProfileTagId = nfcSoundProfileTagId,
                nfcQuickLaunchTagId = nfcQuickLaunchTagId,
                onRunNfcCheck = { viewModel.triggerMockNfc() },
                onBindSoundProfile = { viewModel.startNfcRegistration("sound_profile") },
                onBindQuickLaunch = { viewModel.startNfcRegistration("quick_launch") },
                onClearSoundProfile = { viewModel.clearNfcTag("sound_profile") },
                onClearQuickLaunch = { viewModel.clearNfcTag("quick_launch") },
                onSimulateSoundProfile = {
                    CapsuleStateManager.processNfcTag(context, nfcSoundProfileTagId.ifEmpty { "04:A1:C3:E5" }, "ISO 14443-4 NDEF")
                },
                onSimulateQuickLaunch = {
                    CapsuleStateManager.processNfcTag(context, nfcQuickLaunchTagId.ifEmpty { "04:B2:D4:F6" }, "ISO 14443-4 NDEF")
                }
            )
        }

        // ── MOCK CAPSULE (SIMULATOR) ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            EventSimulatorSection(
                onMockBattery = { viewModel.triggerMockBatteryCharging() },
                onMockLowBattery = { viewModel.triggerMockLowBattery() },
                onMockFaceID = { viewModel.triggerMockFaceID(true) },
                onMockFaceIDFail = { viewModel.triggerMockFaceID(false) },
                onMockMobilePay = { viewModel.triggerMockMobilePay() },
                onMockQuickShare = { viewModel.triggerMockQuickShare() },
                onMockBluetooth = { viewModel.triggerMockBluetoothHeadset("Pixel Buds Pro 2", 92) },
                onMockFocusMode = { viewModel.triggerMockFocusMode("Work") },
                onMockOngoingCall = { viewModel.triggerMockOngoingCall("Ayush", false) },
                onMockIncomingCall = { viewModel.triggerMockOngoingCall("Mom", true) },
                onMockScreenRecord = { viewModel.triggerMockScreenRecording() },
                onMockVoiceMemo = { viewModel.triggerMockVoiceMemo() },
                onMockHotspot = { viewModel.triggerMockHotspot(3) },
                onMockRideSharing = { viewModel.triggerMockRideSharing("Cab Service", "Driver arriving in 3m", 0.75f) },
                onMockFoodDelivery = { viewModel.triggerMockFoodDelivery("Food Delivery", "Delivery partner is near", 0.9f) },
                onMockMusic = { viewModel.triggerMockMusicPlayback() },
                onMockTimer = { viewModel.triggerMockTimer() },
                onMockStopwatch = { viewModel.triggerMockStopwatch() },
                onMockAlarm = { viewModel.triggerMockAlarm() },
                onMockNav = { viewModel.triggerMockNavigation() },
                onMockProgress = { viewModel.triggerMockProgress() },
                onMockNotification = { viewModel.triggerMockNotification() },
                onMockWifiToggle = { viewModel.triggerMockSystemToggle("Wi-Fi", true) },
                onMockFlashlightToggle = { viewModel.triggerMockSystemToggle("Flashlight", true) },
                onMockDndToggle = { viewModel.triggerMockSystemToggle("DND", true) },
                onMockSilentToggle = { viewModel.triggerMockSystemToggle("Silent", true) },
                onMockLowPowerToggle = { viewModel.triggerMockSystemToggle("Low Power", true) },
                onMockNfc = { viewModel.triggerMockNfc() },
                onClear = { viewModel.clearEvents() }
            )
        }

        if (activeRegistrationTask != null) {
            val taskName = when (activeRegistrationTask) {
                "sound_profile", "wrist_watch" -> "Sound Profile Toggle"
                "quick_launch", "chetak" -> "Quick Launch Action"
                else -> "NFC Action"
            }
            AlertDialog(
                onDismissRequest = { viewModel.cancelNfcRegistration() },
                title = { Text("Bind NFC Tag") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Nfc,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp).padding(bottom = 16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Approach your physical NFC tag to the back of the phone to bind it to:\n$taskName\n\n(Or tap 'Simulate Tag Scan' below to bind immediately)",
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val mockId = if (activeRegistrationTask == "sound_profile" || activeRegistrationTask == "wrist_watch") "04:A1:C3:E5" else "04:B2:D4:F6"
                            CapsuleStateManager.processNfcTag(context, mockId, "ISO 14443-4 NDEF")
                        }
                    ) {
                        Text("Simulate Tag Scan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelNfcRegistration() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// ── PERMISSION SECTION ────────────────────────────────────────────────────────
@Composable
fun PermissionSection(
    isOverlayGranted: Boolean,
    isNotificationGranted: Boolean,
    isRecordAudioGranted: Boolean,
    isAccessibilityGranted: Boolean,
    isCalendarGranted: Boolean,
    isLocationGranted: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestRecordAudio: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestCalendar: () -> Unit,
    onRequestLocation: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.extraLarge,                // M3 ExtraLarge = 28dp
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "System Permissions",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Draw over other apps row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isOverlayGranted) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                                contentDescription = null,
                                tint = if (isOverlayGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Draw Over Other Apps", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (isOverlayGranted) "Granted" else "Requires Authorization",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        if (!isOverlayGranted) {
                            Button(
                                onClick = onRequestOverlay,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = MaterialTheme.shapes.extraLarge
                            ) {
                                Text("Grant", fontSize = 12.sp)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                    // Notification access row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isNotificationGranted) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                                contentDescription = null,
                                tint = if (isNotificationGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Notification Listener Access", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (isNotificationGranted) "Granted" else "Requires Authorization",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        if (!isNotificationGranted) {
                            Button(
                                onClick = onRequestNotification,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = MaterialTheme.shapes.extraLarge
                            ) {
                                Text("Grant", fontSize = 12.sp)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                    // System Accessibility Service
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isAccessibilityGranted) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                                contentDescription = null,
                                tint = if (isAccessibilityGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("System Accessibility Service", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (isAccessibilityGranted) "Active" else "Requires Setup",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        if (!isAccessibilityGranted) {
                            Button(
                                onClick = onRequestAccessibility,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = MaterialTheme.shapes.extraLarge
                            ) {
                                Text("Setup", fontSize = 12.sp)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                    // Calendar access row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isCalendarGranted) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                                contentDescription = null,
                                tint = if (isCalendarGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Calendar Access", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (isCalendarGranted) "Granted" else "Optional for events read",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        if (!isCalendarGranted) {
                            Button(
                                onClick = onRequestCalendar,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = MaterialTheme.shapes.extraLarge
                            ) {
                                Text("Grant", fontSize = 12.sp)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                    // Location access row (Weather)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isLocationGranted) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                                contentDescription = null,
                                tint = if (isLocationGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Location Access", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (isLocationGranted) "Granted" else "Optional for weather read",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        if (!isLocationGranted) {
                            Button(
                                onClick = onRequestLocation,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = MaterialTheme.shapes.extraLarge
                            ) {
                                Text("Grant", fontSize = 12.sp)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                    // Microphone access row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isRecordAudioGranted) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                                contentDescription = null,
                                tint = if (isRecordAudioGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Real Audio Visualizer (Microphone)", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (isRecordAudioGranted) "Granted" else "Optional for real-time waveform",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        if (!isRecordAudioGranted) {
                            Button(
                                onClick = onRequestRecordAudio,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = MaterialTheme.shapes.extraLarge  // M3 Full/Pill
                            ) {
                                Text("Grant", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceControlSection(
    isServiceRunning: Boolean,
    isPermissionsGranted: Boolean,
    onToggleService: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,                // M3 ExtraLarge = 28dp
        colors = CardDefaults.cardColors(
            // Active: use primaryContainer (system-tinted on M3/Material You)
            // Inactive: standard surfaceVariant
            containerColor = if (isServiceRunning)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Capsular Service",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = if (isServiceRunning) "Running in Foreground" else "Stopped",
                    color = if (isServiceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = isServiceRunning,
                onCheckedChange = {
                    com.example.capsulebar.service.HapticSoundManager.playToggle(it)
                    onToggleService()
                },
                enabled = true,
                thumbContent = if (isServiceRunning) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                } else {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedIconColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    uncheckedIconColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
        }
    }
}

@Composable
fun PositionSection(
    xOffset: Int,
    yOffset: Int,
    widthDp: Int,
    heightDp: Int,
    edgeRoundingPercent: Int,
    maxPopupWidthPercent: Int,
    expandedHeightScale: Int,
    cameraPosition: String,
    cameraWidthDp: Int,
    isCalibrationMode: Boolean,
    isPositionLocked: Boolean,
    isServiceRunning: Boolean,
    splitPosition: String,
    showAsNotch: Boolean,
    showAlways: Boolean,
    onXChanged: (Int) -> Unit,
    onYChanged: (Int) -> Unit,
    onWidthChanged: (Int) -> Unit,
    onHeightChanged: (Int) -> Unit,
    onEdgeRoundingChanged: (Int) -> Unit,
    onMaxPopupWidthChanged: (Int) -> Unit,
    onExpandedHeightScaleChanged: (Int) -> Unit,
    onTestLiveExpand: () -> Unit,
    onCameraPositionChanged: (String) -> Unit,
    onCameraWidthChanged: (Int) -> Unit,
    onToggleCalibration: (Boolean) -> Unit,
    onTogglePositionLocked: (Boolean) -> Unit,
    onSplitPositionChanged: (String) -> Unit,
    onShowAsNotchToggle: (Boolean) -> Unit,
    onShowAlwaysToggle: (Boolean) -> Unit,
    onReset: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var stepSize by remember { mutableIntStateOf(5) }
    var showFineSliders by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── HEADER & LIVE OUTLINE TOGGLE ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Capsule Position Remote",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (isPositionLocked) Color(0xFF00E676).copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isPositionLocked) "FIXED" else "EDITING",
                                color = if (isPositionLocked) Color(0xFF00C853) else MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        if (isPositionLocked)
                            "Capsule position locked — all elements stay anchored to this area"
                        else
                            "Use the remote D-pad to align, then tap FIX to lock in place",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }

                if (isServiceRunning) {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleCalibration(!isCalibrationMode)
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isCalibrationMode) MaterialTheme.colorScheme.errorContainer
                                             else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (isCalibrationMode) MaterialTheme.colorScheme.onErrorContainer
                                           else MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = if (isCalibrationMode) Icons.Rounded.VisibilityOff else Icons.Rounded.CenterFocusStrong,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isCalibrationMode) "Hide Guide" else "Live Guide",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!isServiceRunning) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Turn on Capsular service above to see the live capsule outline move while using the remote.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ── REMOTE CONTROL CONSOLE ────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (isPositionLocked)
                        Color(0xFF00E676).copy(alpha = 0.55f)
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. DIGITAL TELEMETRY SCREEN (Shows Live Coordinates & Unified Anchor Status)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "X: ${if (xOffset >= 0) "+$xOffset" else "$xOffset"}px   •   Y: ${yOffset}px",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Size: ${widthDp}×${heightDp}dp  •  Hole: ${cameraWidthDp}dp ($cameraPosition)  •  Split: $splitPosition",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = if (isPositionLocked) Icons.Rounded.Lock else Icons.Rounded.OpenWith,
                            contentDescription = null,
                            tint = if (isPositionLocked) Color(0xFF00C853) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 2. STEP SPEED SELECTOR (1px Fine / 5px Normal / 15px Fast)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Step Speed",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(1 to "1px Fine", 5 to "5px Step", 15 to "15px Fast").forEach { (step, label) ->
                                val selected = stepSize == step
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary
                                             else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                        )
                                        .clickable {
                                            stepSize = step
                                            com.example.capsulebar.service.HapticSoundManager.playTick()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // 3. MAIN REMOTE BODY: [WIDTH ROCKER]  +  [CIRCULAR D-PAD & LOCK]  +  [HEIGHT ROCKER]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Rocker: Capsule Width
                        RemoteRockerColumn(
                            label = "WIDTH",
                            valueText = "${widthDp}dp",
                            enabled = !isPositionLocked,
                            onIncrease = {
                                val delta = if (stepSize == 1) 1 else 4
                                onWidthChanged((widthDp + delta).coerceIn(70, 250))
                                com.example.capsulebar.service.HapticSoundManager.playTick()
                            },
                            onDecrease = {
                                val delta = if (stepSize == 1) 1 else 4
                                onWidthChanged((widthDp - delta).coerceIn(70, 250))
                                com.example.capsulebar.service.HapticSoundManager.playTick()
                            }
                        )

                        // Center Circular D-Pad with Center FIX / LOCK Button
                        Box(
                            modifier = Modifier
                                .size(168.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        )
                                    )
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isPositionLocked)
                                        Color(0xFF00E676).copy(alpha = 0.5f)
                                    else
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // UP Button (Y - step)
                            RemoteDpadButton(
                                icon = Icons.Rounded.KeyboardArrowUp,
                                contentDescription = "Move Up",
                                enabled = !isPositionLocked,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 6.dp),
                                onClick = {
                                    onYChanged((yOffset - stepSize).coerceIn(0, 200))
                                    com.example.capsulebar.service.HapticSoundManager.playTick()
                                }
                            )

                            // DOWN Button (Y + step)
                            RemoteDpadButton(
                                icon = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Move Down",
                                enabled = !isPositionLocked,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 6.dp),
                                onClick = {
                                    onYChanged((yOffset + stepSize).coerceIn(0, 200))
                                    com.example.capsulebar.service.HapticSoundManager.playTick()
                                }
                            )

                            // LEFT Button (X - step)
                            RemoteDpadButton(
                                icon = Icons.Rounded.KeyboardArrowLeft,
                                contentDescription = "Move Left",
                                enabled = !isPositionLocked,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 6.dp),
                                onClick = {
                                    onXChanged((xOffset - stepSize).coerceIn(-300, 300))
                                    com.example.capsulebar.service.HapticSoundManager.playTick()
                                }
                            )

                            // RIGHT Button (X + step)
                            RemoteDpadButton(
                                icon = Icons.Rounded.KeyboardArrowRight,
                                contentDescription = "Move Right",
                                enabled = !isPositionLocked,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 6.dp),
                                onClick = {
                                    onXChanged((xOffset + stepSize).coerceIn(-300, 300))
                                    com.example.capsulebar.service.HapticSoundManager.playTick()
                                }
                            )

                            // CENTER OK / FIX POSITION BUTTON
                            Surface(
                                shape = CircleShape,
                                color = if (isPositionLocked) Color(0xFF00C853) else MaterialTheme.colorScheme.primary,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        val nextLocked = !isPositionLocked
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        com.example.capsulebar.service.HapticSoundManager.playToggle(nextLocked)
                                        onTogglePositionLocked(nextLocked)
                                    }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = if (isPositionLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                        contentDescription = if (isPositionLocked) "Unlock Position" else "Fix Position",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (isPositionLocked) "LOCKED" else "FIX POS",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        // Right Rocker: Capsule Height
                        RemoteRockerColumn(
                            label = "HEIGHT",
                            valueText = "${heightDp}dp",
                            enabled = !isPositionLocked,
                            onIncrease = {
                                val delta = if (stepSize == 1) 1 else 2
                                onHeightChanged((heightDp + delta).coerceIn(25, 60))
                                com.example.capsulebar.service.HapticSoundManager.playTick()
                            },
                            onDecrease = {
                                val delta = if (stepSize == 1) 1 else 2
                                onHeightChanged((heightDp - delta).coerceIn(25, 60))
                                com.example.capsulebar.service.HapticSoundManager.playTick()
                            }
                        )
                    }

                    // 4. CAMERA CUTOUT HOLE WIDTH ROCKER (In the same remote console)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Camera Hole Cutout Width",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Keeps left & right capsule icons clear of the lens",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    onCameraWidthChanged((cameraWidthDp - 2).coerceIn(15, 80))
                                    com.example.capsulebar.service.HapticSoundManager.playTick()
                                },
                                enabled = !isPositionLocked,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease Cutout", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "${cameraWidthDp}dp",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.widthIn(min = 42.dp)
                            )
                            FilledTonalIconButton(
                                onClick = {
                                    onCameraWidthChanged((cameraWidthDp + 2).coerceIn(15, 80))
                                    com.example.capsulebar.service.HapticSoundManager.playTick()
                                },
                                enabled = !isPositionLocked,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Increase Cutout", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // 5. REMOTE QUICK ACTION BAR: Auto-Detect | Center X | Test Bounce | Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val activity = context as? android.app.Activity
                                if (activity != null) {
                                    val result = com.example.capsulebar.util.DisplayCutoutHelper.detectCutout(activity)
                                    if (result != null) {
                                        onCameraPositionChanged(result.position)
                                        onXChanged(result.xOffsetPx)
                                        onYChanged(result.yOffsetPx)
                                        onCameraWidthChanged(result.cameraWidthDp)
                                        com.example.capsulebar.service.HapticManager.vibrateDouble()
                                        com.example.capsulebar.service.HapticSoundManager.playExpand()
                                    } else {
                                        com.example.capsulebar.service.HapticSoundManager.playTick()
                                    }
                                }
                            },
                            enabled = !isPositionLocked,
                            modifier = Modifier
                                .weight(1.15f)
                                .height(36.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Auto-Detect", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                onXChanged(0)
                                com.example.capsulebar.service.HapticSoundManager.playTick()
                            },
                            enabled = !isPositionLocked,
                            modifier = Modifier
                                .weight(0.95f)
                                .height(36.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Rounded.AlignHorizontalCenter, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Center X", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                com.example.capsulebar.service.HapticSoundManager.playCapsuleAppear()
                                CapsuleStateManager.postEvent(
                                    CapsuleEvent.Notification(
                                        id = "test_bounce_${System.currentTimeMillis()}",
                                        packageName = "com.example.capsulebar",
                                        appName = "Capsule Bar",
                                        title = "Position Verified",
                                        text = "All capsule elements anchored here",
                                        priority = 990,
                                        durationMs = 3800
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(0.95f)
                                .height(36.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Test", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onReset()
                            },
                            enabled = !isPositionLocked,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Reset Position", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // ── UNIFIED CAPSULE AREA CONFIGURATION (Camera + Split Side + Notch Mode) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Camera Cutout Anchor
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        "Camera Anchor",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Left", "Center", "Right").forEach { pos ->
                            val isSelected = cameraPosition == pos
                            val bg = if (isSelected) MaterialTheme.colorScheme.primary
                                     else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .background(bg)
                                    .clickable(enabled = !isPositionLocked) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onCameraPositionChanged(pos)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pos,
                                    color = textColor.copy(alpha = if (isPositionLocked && !isSelected) 0.45f else 1f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Side Split Capsule Anchor
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Split Bubble Side",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Left", "Right").forEach { side ->
                            val isSelected = splitPosition == side
                            val bg = if (isSelected) MaterialTheme.colorScheme.secondary
                                     else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            val textColor = if (isSelected) MaterialTheme.colorScheme.onSecondary
                                            else MaterialTheme.colorScheme.onSurface
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .background(bg)
                                    .clickable(enabled = !isPositionLocked) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSplitPositionChanged(side)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = side,
                                    color = textColor.copy(alpha = if (isPositionLocked && !isSelected) 0.45f else 1f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Quick Capsule Area Behavior Toggles (Notch flush top & Always visible in fixed area)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showAsNotch,
                    onClick = {
                        if (!isPositionLocked) {
                            com.example.capsulebar.service.HapticSoundManager.playToggle(!showAsNotch)
                            onShowAsNotchToggle(!showAsNotch)
                        }
                    },
                    enabled = !isPositionLocked,
                    label = { Text("Flush Top Notch", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.VerticalAlignTop,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = showAlways,
                    onClick = {
                        com.example.capsulebar.service.HapticSoundManager.playToggle(!showAlways)
                        onShowAlwaysToggle(!showAlways)
                    },
                    label = { Text("Always Show Pill", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // ── EXPANDED BIG CAPSULE SIZE (BADA CAPSULE CALIBRATION & LIVE PREVIEW) ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Row with Title and Live Screen Test Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.OpenInFull,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Big Capsule Size (Expanded)",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Adjust card size when notch is tapped",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Test on real screen button
                        FilledTonalButton(
                            onClick = {
                                com.example.capsulebar.service.HapticSoundManager.playClick()
                                onTestLiveExpand()
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text("Test Live", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Live Telemetry Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E676))
                                )
                                Text(
                                    text = "LIVE SIZE",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = "Width: ${maxPopupWidthPercent}% • Height Scale: ${expandedHeightScale}%",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // LIVE MINI-SCREEN PREVIEW BOX
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0C0D14))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Punch Hole Mock
                        val cameraAlignment = when (cameraPosition) {
                            "LEFT" -> Alignment.TopStart
                            "RIGHT" -> Alignment.TopEnd
                            else -> Alignment.TopCenter
                        }
                        val cameraPad = when (cameraPosition) {
                            "LEFT" -> Modifier.padding(start = 20.dp, top = 5.dp)
                            "RIGHT" -> Modifier.padding(end = 20.dp, top = 5.dp)
                            else -> Modifier.padding(top = 5.dp)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(cameraPad),
                            contentAlignment = cameraAlignment
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                                    .border(1.dp, Color(0xFF333333), CircleShape)
                            )
                        }

                        // Scaled Big Capsule Representation
                        val previewWidthFraction = (maxPopupWidthPercent / 100f).coerceIn(0.6f, 0.98f)
                        val previewHeightDp = (65f * (expandedHeightScale / 100f)).coerceIn(45f, 105f).dp

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(previewWidthFraction)
                                .height(previewHeightDp)
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1E1E28).copy(alpha = 0.92f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.MusicNote,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Now Playing",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "Dynamic Expanded Island",
                                                color = Color.LightGray,
                                                fontSize = 7.5.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    // Wave Bars
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf(6, 12, 9, 14).forEach { barHeight ->
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(barHeight.dp)
                                                    .clip(RoundedCornerShape(1.dp))
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.SkipPrevious, null, tint = Color.LightGray, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(14.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Pause, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(11.dp))
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Icon(Icons.Rounded.SkipNext, null, tint = Color.LightGray, modifier = Modifier.size(13.dp))
                                }
                            }
                        }

                        Text(
                            text = "Live Scale Preview",
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 3.dp)
                        )
                    }

                    // 1. EXPANDED WIDTH ADJUSTMENT
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Expanded Width (Chaudai)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${maxPopupWidthPercent}% screen",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    val next = (maxPopupWidthPercent - 5).coerceIn(60, 100)
                                    com.example.capsulebar.service.HapticSoundManager.playTick()
                                    onMaxPopupWidthChanged(next)
                                },
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease Width", modifier = Modifier.size(16.dp))
                            }

                            Slider(
                                value = maxPopupWidthPercent.toFloat(),
                                onValueChange = { onMaxPopupWidthChanged(it.toInt()) },
                                valueRange = 60f..100f,
                                modifier = Modifier.weight(1f)
                            )

                            FilledTonalIconButton(
                                onClick = {
                                    val next = (maxPopupWidthPercent + 5).coerceIn(60, 100)
                                    com.example.capsulebar.service.HapticSoundManager.playTick()
                                    onMaxPopupWidthChanged(next)
                                },
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Increase Width", modifier = Modifier.size(16.dp))
                            }
                        }

                        // Quick presets for Width
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(75 to "Compact 75%", 94 to "Standard 94%", 100 to "Edge 100%").forEach { (targetWidth, label) ->
                                val isSel = maxPopupWidthPercent == targetWidth
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        com.example.capsulebar.service.HapticSoundManager.playTick()
                                        onMaxPopupWidthChanged(targetWidth)
                                    },
                                    label = { Text(label, fontSize = 10.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 2. EXPANDED HEIGHT SCALE ADJUSTMENT
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Expanded Height (Lambai / Unchai)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${expandedHeightScale}% scale",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    val next = (expandedHeightScale - 5).coerceIn(70, 140)
                                    com.example.capsulebar.service.HapticSoundManager.playTick()
                                    onExpandedHeightScaleChanged(next)
                                },
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease Height", modifier = Modifier.size(16.dp))
                            }

                            Slider(
                                value = expandedHeightScale.toFloat(),
                                onValueChange = { onExpandedHeightScaleChanged(it.toInt()) },
                                valueRange = 70f..140f,
                                modifier = Modifier.weight(1f)
                            )

                            FilledTonalIconButton(
                                onClick = {
                                    val next = (expandedHeightScale + 5).coerceIn(70, 140)
                                    com.example.capsulebar.service.HapticSoundManager.playTick()
                                    onExpandedHeightScaleChanged(next)
                                },
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Increase Height", modifier = Modifier.size(16.dp))
                            }
                        }

                        // Quick presets for Height
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(85 to "Compact 85%", 100 to "Standard 100%", 120 to "Spacious 120%").forEach { (targetScale, label) ->
                                val isSel = expandedHeightScale == targetScale
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        com.example.capsulebar.service.HapticSoundManager.playTick()
                                        onExpandedHeightScaleChanged(targetScale)
                                    },
                                    label = { Text(label, fontSize = 10.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // ── COLLAPSIBLE FINE SLIDERS & POPUP GEOMETRY (In the same unified section) ──
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        showFineSliders = !showFineSliders
                        com.example.capsulebar.service.HapticSoundManager.playTick()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Fine Sliders & Shape Geometry",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = if (showFineSliders) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = showFineSliders,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    SliderOptionRow(
                        title = "Position horizontal (X)",
                        valueText = "${xOffset}px",
                        value = xOffset.toFloat(),
                        onValueChange = { if (!isPositionLocked) onXChanged(it.toInt()) },
                        valueRange = -300f..300f,
                        decreaseIcon = Icons.Rounded.ChevronLeft,
                        increaseIcon = Icons.Rounded.ChevronRight
                    )

                    SliderOptionRow(
                        title = "Position vertical (Y)",
                        valueText = "${yOffset}px",
                        value = yOffset.toFloat(),
                        onValueChange = { if (!isPositionLocked) onYChanged(it.toInt()) },
                        valueRange = 0f..200f,
                        decreaseIcon = Icons.Rounded.KeyboardArrowUp,
                        increaseIcon = Icons.Rounded.KeyboardArrowDown
                    )

                    SliderOptionRow(
                        title = "Capsule Width",
                        valueText = "${widthDp}dp",
                        value = widthDp.toFloat(),
                        onValueChange = { if (!isPositionLocked) onWidthChanged(it.toInt()) },
                        valueRange = 70f..250f,
                        decreaseIcon = Icons.Rounded.Remove,
                        increaseIcon = Icons.Rounded.Add
                    )

                    SliderOptionRow(
                        title = "Capsule Height",
                        valueText = "${heightDp}dp",
                        value = heightDp.toFloat(),
                        onValueChange = { if (!isPositionLocked) onHeightChanged(it.toInt()) },
                        valueRange = 25f..60f,
                        decreaseIcon = Icons.Rounded.KeyboardArrowUp,
                        increaseIcon = Icons.Rounded.KeyboardArrowDown
                    )

                    SliderOptionRow(
                        title = "Camera Cutout Width",
                        valueText = "${cameraWidthDp}dp",
                        value = cameraWidthDp.toFloat(),
                        onValueChange = { if (!isPositionLocked) onCameraWidthChanged(it.toInt()) },
                        valueRange = 15f..80f,
                        decreaseIcon = Icons.Rounded.Remove,
                        increaseIcon = Icons.Rounded.Add
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                    SliderOptionRow(
                        title = "Capsule Edge Rounding",
                        valueText = "${edgeRoundingPercent}%",
                        value = edgeRoundingPercent.toFloat(),
                        onValueChange = { if (!isPositionLocked) onEdgeRoundingChanged(it.toInt()) },
                        valueRange = 0f..100f,
                        decreaseIcon = Icons.Rounded.Remove,
                        increaseIcon = Icons.Rounded.Add
                    )

                    SliderOptionRow(
                        title = "Expanded Card Width",
                        valueText = "${maxPopupWidthPercent}%",
                        value = maxPopupWidthPercent.toFloat(),
                        onValueChange = { if (!isPositionLocked) onMaxPopupWidthChanged(it.toInt()) },
                        valueRange = 60f..100f,
                        decreaseIcon = Icons.Rounded.Remove,
                        increaseIcon = Icons.Rounded.Add
                    )

                    SliderOptionRow(
                        title = "Expanded Card Height Scale",
                        valueText = "${expandedHeightScale}%",
                        value = expandedHeightScale.toFloat(),
                        onValueChange = { if (!isPositionLocked) onExpandedHeightScaleChanged(it.toInt()) },
                        valueRange = 70f..140f,
                        decreaseIcon = Icons.Rounded.Remove,
                        increaseIcon = Icons.Rounded.Add
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoteDpadButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(44.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun RemoteRockerColumn(
    label: String,
    valueText: String,
    enabled: Boolean,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .width(68.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(vertical = 10.dp, horizontal = 6.dp)
    ) {
        FilledTonalIconButton(
            onClick = onIncrease,
            enabled = enabled,
            modifier = Modifier.size(38.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Increase $label", modifier = Modifier.size(18.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = valueText,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }
        FilledTonalIconButton(
            onClick = onDecrease,
            enabled = enabled,
            modifier = Modifier.size(38.dp)
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = "Decrease $label", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun AppearanceSection(
    themeMode: String,
    hapticsEnabled: Boolean,
    soundEffectsEnabled: Boolean,
    showAsNotch: Boolean,
    addBackground: Boolean,
    showImages: Boolean,
    quickAnimations: Boolean,
    premiumAnimations: Boolean,
    reverseOrder: Boolean,
    maxTextLines: Int,
    defaultColor: Int,
    autoColor: Boolean,
    useAppColors: Boolean,
    showMusicVisualizer: Boolean,
    useAndroidMusicControls: Boolean,
    iconOption: Int,
    bluetoothImagePath: String?,
    onThemeModeChanged: (String) -> Unit,
    onHapticsToggle: (Boolean) -> Unit,
    onSoundEffectsToggle: (Boolean) -> Unit,
    onShowAsNotchToggle: (Boolean) -> Unit,
    onAddBackgroundToggle: (Boolean) -> Unit,
    onShowImagesToggle: (Boolean) -> Unit,
    onQuickAnimationsToggle: (Boolean) -> Unit,
    onPremiumAnimationsToggle: (Boolean) -> Unit,
    onReverseOrderToggle: (Boolean) -> Unit,
    onMaxTextLinesChanged: (Int) -> Unit,
    onDefaultColorChanged: (Int) -> Unit,
    onAutoColorToggle: (Boolean) -> Unit,
    onUseAppColorsToggle: (Boolean) -> Unit,
    onShowMusicVisualizerToggle: (Boolean) -> Unit,
    onUseAndroidMusicControlsToggle: (Boolean) -> Unit,
    onIconOptionChanged: (Int) -> Unit,
    onUploadBluetoothImage: (String?) -> Unit,
    cameraPosition: String,
    onTestLiveExpand: () -> Unit,
    isGlassEffectEnabled: Boolean,
    glassBlurRadius: Int,
    glassEdgeTransparency: Int,
    glassEffectArea: Int,
    glassBlackTint: Int,
    onToggleGlassEffect: (Boolean) -> Unit,
    onGlassBlurRadiusChanged: (Int) -> Unit,
    onGlassEdgeTransparencyChanged: (Int) -> Unit,
    onGlassEffectAreaChanged: (Int) -> Unit,
    onGlassBlackTintChanged: (Int) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> 
            if (uri != null) {
                val path = copyUriToInternalStorage(context, uri)
                onUploadBluetoothImage(path)
            }
        }
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,             // M3 ExtraLarge = 28dp,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Appearance & Feedback",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            // Theme Mode Selector (Pixel UI Segmented Button)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Application Theme",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Pure Black AMOLED dark mode or crisp Pure White light mode",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val themes = listOf(
                        Triple("dark", "Dark", Icons.Rounded.DarkMode),
                        Triple("light", "Light", Icons.Rounded.LightMode),
                        Triple("system", "Auto", Icons.Rounded.BrightnessAuto)
                    )
                    themes.forEach { (mode, label, icon) ->
                        val isSelected = themeMode == mode
                        val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .clickable {
                                    com.example.capsulebar.service.HapticSoundManager.playClick()
                                    onThemeModeChanged(mode)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = contentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = label,
                                    color = contentColor,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

            // Sensory Feedback (Haptics & Audio)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Sensory Feedback",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                ToggleOptionRow(
                    title = "Haptic Vibrations",
                    subtitle = "Tactile clicks, ticks, and expand vibrations synced across the app",
                    checked = hapticsEnabled,
                    onCheckedChange = {
                        onHapticsToggle(it)
                    }
                )
                ToggleOptionRow(
                    title = "Sound Effects",
                    subtitle = "Subtle, crisp system audio feedback for clicks, toggles and capsules",
                    checked = soundEffectsEnabled,
                    onCheckedChange = {
                        onSoundEffectsToggle(it)
                    }
                )
                FilledTonalButton(
                    onClick = {
                        com.example.capsulebar.service.HapticSoundManager.playSuccess()
                    },
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.TouchApp, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Test Haptics & Sound Feedback", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

            // Popup
            Text("Popup", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            ToggleOptionRow("Show as notch", "Show the popup as a notch attached to the frame, instead of a floating pill", showAsNotch, onShowAsNotchToggle)
            ToggleOptionRow("Add background", "Show action buttons with a background", addBackground, onAddBackgroundToggle)
            ToggleOptionRow("Show images", "Show images from notifications in the popup", showImages, onShowImagesToggle)

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

            // Animations
            Text("Animations", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            ToggleOptionRow("Quick animations", "Faster expand and collapse animations", quickAnimations, onQuickAnimationsToggle)
            ToggleOptionRow("Premium animations", "Enable smooth transitions and animations when notifications get added or removed, like the original Dynamic Island on iPhone", premiumAnimations, onPremiumAnimationsToggle)

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

            // Messages
            Text("Messages", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            ToggleOptionRow("Reverse order", "Try to show oldest message on top", reverseOrder, onReverseOrderToggle)
            
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Maximum text lines", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    Text("$maxTextLines", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = maxTextLines.toFloat(),
                    onValueChange = { onMaxTextLinesChanged(it.toInt()) },
                    valueRange = 1f..30f
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

            // Colors
            Text("Colors", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Default color", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    Text("Choose your favorite color", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                
                // Color selection dots
                val colorOptions = listOf(
                    0xFF000000.toInt() to Color.Black,
                    0xFF1C1B1F.toInt() to Color(0xFF1C1B1F),
                    0xFF6750A4.toInt() to Color(0xFF6750A4),
                    0xFF038747.toInt() to Color(0xFF038747),
                    0xFFB3261E.toInt() to Color(0xFFB3261E)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    colorOptions.forEach { (cInt, cVal) ->
                        val isSelected = defaultColor == cInt
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(cVal)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { onDefaultColorChanged(cInt) }
                        )
                    }
                }
            }
            ToggleOptionRow("Auto color", "Disabled", autoColor, onAutoColorToggle)
            ToggleOptionRow("App colors", "Select a custom color for each application", useAppColors, onUseAppColorsToggle)

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

            // Glass Effect & Notch Blur
            Text("Glass Effect & Notch Blur", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            ToggleOptionRow("Glass Effect", "Frosted glass edges with solid notch disguise", isGlassEffectEnabled, onToggleGlassEffect)

            AnimatedVisibility(
                visible = isGlassEffectEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // ── REALISTIC LIVE NOTCH & GLASS CAPSULE SIMULATION (REPLACES DUMMY BAR) ──
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Top Row: Title + Live Test Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00E676))
                                    )
                                    Text(
                                        text = "LIVE NOTCH GLASS PREVIEW",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        com.example.capsulebar.service.HapticSoundManager.playClick()
                                        onTestLiveExpand()
                                    },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("Test on Screen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Dynamic Telemetry Badge
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Notch: 100% Solid Black",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Edge: ${glassEdgeTransparency}% Glass • Blur: ${glassBlurRadius}dp • Tint: ${glassBlackTint}%",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // ── FULL PHONE DISPLAY FRAME WITH VIBRANT WALLPAPER BEHIND GLASS ──
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                            ) {
                                // 1. Multi-color Wallpaper Layer (Vibrant wallpaper allows user to clearly evaluate transparency & blur)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .drawBehind {
                                            drawRect(
                                                brush = Brush.linearGradient(
                                                    listOf(
                                                        Color(0xFF19113B),
                                                        Color(0xFF241B54),
                                                        Color(0xFF0F3A4A),
                                                        Color(0xFF064E3B)
                                                    )
                                                )
                                            )
                                            drawCircle(
                                                brush = Brush.radialGradient(
                                                    listOf(Color(0xFFFF2A6D).copy(alpha = 0.75f), Color.Transparent),
                                                    radius = size.width * 0.44f,
                                                    center = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.85f)
                                                )
                                            )
                                            drawCircle(
                                                brush = Brush.radialGradient(
                                                    listOf(Color(0xFF05D9E8).copy(alpha = 0.70f), Color.Transparent),
                                                    radius = size.width * 0.48f,
                                                    center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.35f)
                                                )
                                            )
                                            drawCircle(
                                                brush = Brush.radialGradient(
                                                    listOf(Color(0xFFFFB800).copy(alpha = 0.55f), Color.Transparent),
                                                    radius = size.width * 0.36f,
                                                    center = androidx.compose.ui.geometry.Offset(size.width * 0.55f, size.height * 0.92f)
                                                )
                                            )
                                        }
                                )

                                // 2. Phone Status Bar Layer (Time, Icons)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "9:41",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.Wifi, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(13.dp))
                                        Icon(Icons.Rounded.SignalCellular4Bar, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(13.dp))
                                        Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
                                    }
                                }

                                // 3. Camera Notch Hardware Cutout
                                val notchHorizAlign = when (cameraPosition) {
                                    "LEFT" -> Alignment.TopStart
                                    "RIGHT" -> Alignment.TopEnd
                                    else -> Alignment.TopCenter
                                }
                                val notchPad = when (cameraPosition) {
                                    "LEFT" -> Modifier.padding(start = 28.dp, top = 8.dp)
                                    "RIGHT" -> Modifier.padding(end = 28.dp, top = 8.dp)
                                    else -> Modifier.padding(top = 8.dp)
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(notchPad),
                                    contentAlignment = notchHorizAlign
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(11.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black)
                                            .border(0.8.dp, Color(0xFF2E2E3A), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(3.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1E293B))
                                        )
                                    }
                                }

                                // 4. THE EXPANDED GLASS CAPSULE (Emerging seamlessly from Notch)
                                val previewBlackTintFactor = (glassBlackTint / 100f).coerceIn(0.15f, 1.0f)
                                val previewTransparency = (glassEdgeTransparency / 100f).coerceIn(0.10f, 0.95f)
                                val previewAreaRatio = (glassEffectArea / 100f).coerceIn(0.15f, 0.90f)

                                val previewEdgeAlpha = ((1.0f - previewTransparency) * previewBlackTintFactor).coerceIn(0.08f, 0.90f)
                                val previewMidAlpha = ((1.0f - (previewTransparency * 0.45f)) * (0.85f + 0.15f * previewBlackTintFactor)).coerceIn(previewEdgeAlpha, 0.98f)
                                val previewCenterAlpha = (0.94f + 0.06f * previewBlackTintFactor).coerceIn(0.88f, 1.0f)

                                val notchRelativeX = when (cameraPosition) {
                                    "LEFT" -> 0.16f
                                    "RIGHT" -> 0.84f
                                    else -> 0.50f
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .height(112.dp)
                                        .align(Alignment.TopCenter)
                                        .padding(top = 18.dp)
                                        .clip(RoundedCornerShape(26.dp))
                                        .drawBehind {
                                            val notchCenterX = size.width * notchRelativeX
                                            val notchCenterY = 0f
                                            val maxHypot = kotlin.math.hypot(size.width, size.height)
                                            val gradRadius = (maxHypot * (1.25f - previewAreaRatio * 0.55f)).coerceAtLeast(30f)

                                            val stop1 = (0.26f * (1.1f - previewAreaRatio)).coerceIn(0.06f, 0.40f)
                                            val stop2 = (stop1 + (1.0f - stop1) * 0.50f).coerceIn(stop1 + 0.12f, 0.85f)

                                            // 1. Core Glass Gradient: Solid Pitch Black around Notch, fading into Frosted Translucent Glass at Edges
                                            drawRoundRect(
                                                brush = Brush.radialGradient(
                                                    colorStops = arrayOf(
                                                        0.0f to Color.Black.copy(alpha = previewCenterAlpha),
                                                        stop1 to Color(0xFF0A0A0F).copy(alpha = (previewCenterAlpha * 0.97f)),
                                                        stop2 to Color(0xFF14141E).copy(alpha = previewMidAlpha),
                                                        1.0f to Color(0xFF1A1A26).copy(alpha = previewEdgeAlpha)
                                                    ),
                                                    center = androidx.compose.ui.geometry.Offset(notchCenterX, notchCenterY),
                                                    radius = gradRadius
                                                ),
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(26.dp.toPx(), 26.dp.toPx())
                                            )

                                            // 2. Specular Top Frosted Light Highlight
                                            drawRoundRect(
                                                brush = Brush.verticalGradient(
                                                    listOf(
                                                        Color.White.copy(alpha = (0.28f * (1f - previewEdgeAlpha * 0.4f)).coerceIn(0.12f, 0.38f)),
                                                        Color.White.copy(alpha = 0.05f),
                                                        Color.Transparent
                                                    )
                                                ),
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(26.dp.toPx(), 26.dp.toPx())
                                            )

                                            // 3. Specular Dual-Tone Glass Rim
                                            drawRoundRect(
                                                brush = Brush.verticalGradient(
                                                    listOf(
                                                        Color.White.copy(alpha = 0.48f),
                                                        Color.White.copy(alpha = 0.16f),
                                                        Color.White.copy(alpha = 0.08f)
                                                    )
                                                ),
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(26.dp.toPx(), 26.dp.toPx()),
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
                                            )
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Header Row inside capsule
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            Brush.linearGradient(
                                                                listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                                                            )
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.MusicNote,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = "Glass Island",
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = "Solid notch • ${glassEdgeTransparency}% transparent edge",
                                                        color = Color.LightGray.copy(alpha = 0.9f),
                                                        fontSize = 9.5.sp,
                                                        maxLines = 1
                                                    )
                                                }
                                            }

                                            // Waveform
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                listOf(8, 16, 12, 20, 10).forEach { barH ->
                                                    Box(
                                                        modifier = Modifier
                                                            .width(2.5.dp)
                                                            .height(barH.dp)
                                                            .clip(RoundedCornerShape(1.dp))
                                                            .background(Color(0xFF8B5CF6))
                                                    )
                                                }
                                            }
                                        }

                                        // Bottom playback row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.SkipPrevious, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(18.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.22f))
                                                    .border(0.8.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Rounded.Pause, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                            Spacer(Modifier.width(18.dp))
                                            Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                // Bottom helper tag in display frame
                                Text(
                                    text = "Notch is 100% disguised in black • Background wallpaper shows through glass edges",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 4.dp)
                                )
                            }
                        }
                    }

                    // 1. Glass Edge Transparency Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SliderOptionRow(
                            title = "Glass Edge Transparency",
                            valueText = "${glassEdgeTransparency}%",
                            value = glassEdgeTransparency.toFloat(),
                            onValueChange = { onGlassEdgeTransparencyChanged(it.toInt()) },
                            valueRange = 10f..95f,
                            decreaseIcon = Icons.Rounded.Remove,
                            increaseIcon = Icons.Rounded.Add,
                            step = 1f
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(30 to "Subtle 30%", 65 to "Frosted 65%", 85 to "High Glass 85%").forEach { (targetVal, label) ->
                                val isSel = glassEdgeTransparency == targetVal
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        com.example.capsulebar.service.HapticSoundManager.playTick()
                                        onGlassEdgeTransparencyChanged(targetVal)
                                    },
                                    label = { Text(label, fontSize = 10.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 2. Black Tone / Darkness Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SliderOptionRow(
                            title = "Black Tint Darkness (Notch Disguise)",
                            valueText = "${glassBlackTint}%",
                            value = glassBlackTint.toFloat(),
                            onValueChange = { onGlassBlackTintChanged(it.toInt()) },
                            valueRange = 10f..100f,
                            decreaseIcon = Icons.Rounded.Remove,
                            increaseIcon = Icons.Rounded.Add,
                            step = 1f
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(50 to "Light 50%", 75 to "Balanced 75%", 95 to "Stealth 95%").forEach { (targetVal, label) ->
                                val isSel = glassBlackTint == targetVal
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        com.example.capsulebar.service.HapticSoundManager.playTick()
                                        onGlassBlackTintChanged(targetVal)
                                    },
                                    label = { Text(label, fontSize = 10.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 3. Blur Intensity Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SliderOptionRow(
                            title = "Blur Intensity (Android 12+)",
                            valueText = "${glassBlurRadius}dp",
                            value = glassBlurRadius.toFloat(),
                            onValueChange = { onGlassBlurRadiusChanged(it.toInt()) },
                            valueRange = 0f..50f,
                            decreaseIcon = Icons.Rounded.Remove,
                            increaseIcon = Icons.Rounded.Add,
                            step = 1f
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(10 to "10dp Subtle", 25 to "25dp Frosted", 40 to "40dp Deep Blur").forEach { (targetVal, label) ->
                                val isSel = glassBlurRadius == targetVal
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        com.example.capsulebar.service.HapticSoundManager.playTick()
                                        onGlassBlurRadiusChanged(targetVal)
                                    },
                                    label = { Text(label, fontSize = 10.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 4. Notch Gradient Spread Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SliderOptionRow(
                            title = "Notch Gradient Spread (Notch to Edge)",
                            valueText = "${glassEffectArea}%",
                            value = glassEffectArea.toFloat(),
                            onValueChange = { onGlassEffectAreaChanged(it.toInt()) },
                            valueRange = 10f..90f,
                            decreaseIcon = Icons.Rounded.Remove,
                            increaseIcon = Icons.Rounded.Add,
                            step = 1f
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(25 to "Tight 25%", 50 to "Natural 50%", 75 to "Wide 75%").forEach { (targetVal, label) ->
                                val isSel = glassEffectArea == targetVal
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        com.example.capsulebar.service.HapticSoundManager.playTick()
                                        onGlassEffectAreaChanged(targetVal)
                                    },
                                    label = { Text(label, fontSize = 10.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

            // Music
            Text("Music", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            ToggleOptionRow("Visualizer", "Show an animated music visualizer in the popup", showMusicVisualizer, onShowMusicVisualizerToggle)
            ToggleOptionRow("Android music controls", "Use android music controls which change depending on used app (play, seek, like, dismiss...)", useAndroidMusicControls, onUseAndroidMusicControlsToggle)

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

            // Icons
            Text("Icons", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            RadioOptionRow("Use images from contacts or music covers", iconOption == 0) { onIconOptionChanged(0) }
            RadioOptionRow("Use app icons", iconOption == 1) { onIconOptionChanged(1) }
            RadioOptionRow("Use small notification icons", iconOption == 2) { onIconOptionChanged(2) }

            // Custom uploader (Accessory Icon Manager) integrated under Icons
            Spacer(Modifier.height(4.dp))
            Text("Accessory Icon Manager", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (bluetoothImagePath != null) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(MaterialTheme.shapes.small)                // M3 Small = 8dp
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    ) {
                        val bitmap = remember(bluetoothImagePath) {
                            try {
                                android.graphics.BitmapFactory.decodeFile(bluetoothImagePath)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            androidx.compose.foundation.Image(
                                  bitmap = bitmap.asImageBitmap(),
                                  contentDescription = null,
                                  modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Button(
                        onClick = { onUploadBluetoothImage(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = MaterialTheme.shapes.extraLarge     // M3 Full/Pill
                    ) {
                        Text("Remove Icon", fontSize = 10.sp)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(MaterialTheme.shapes.small)                // M3 Small = 8dp
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Headphones, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    Button(
                        onClick = {
                            launcher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = MaterialTheme.shapes.extraLarge     // M3 Full/Pill
                    ) {
                        Text("Upload Headset Image", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ToggleOptionRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressClickEffect()
            .clickable {
                val next = !checked
                com.example.capsulebar.service.HapticSoundManager.playToggle(next)
                onCheckedChange(next)
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = { next ->
                com.example.capsulebar.service.HapticSoundManager.playToggle(next)
                onCheckedChange(next)
            },
            thumbContent = if (checked) {
                {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            } else {
                {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedIconColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedIconColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )
    }
}

@Composable
fun RadioOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressClickEffect()
            .clickable {
                com.example.capsulebar.service.HapticSoundManager.playClick()
                onClick()
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        RadioButton(
            selected = selected,
            onClick = {
                com.example.capsulebar.service.HapticSoundManager.playClick()
                onClick()
            },
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
    }
}

@Composable
fun SliderOptionRow(
    title: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    decreaseIcon: ImageVector,
    increaseIcon: ImageVector,
    step: Float = 1f
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(valueText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    com.example.capsulebar.service.HapticSoundManager.playClick()
                    onValueChange((value - step).coerceIn(valueRange.start, valueRange.endInclusive))
                },
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                    .size(32.dp)
            ) {
                Icon(decreaseIcon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
            Slider(
                value = value,
                onValueChange = { newVal ->
                    if (kotlin.math.abs(newVal - value) >= step * 0.85f) {
                        com.example.capsulebar.service.HapticSoundManager.playTick()
                    }
                    onValueChange(newVal)
                },
                valueRange = valueRange,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    com.example.capsulebar.service.HapticSoundManager.playClick()
                    onValueChange((value + step).coerceIn(valueRange.start, valueRange.endInclusive))
                },
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                    .size(32.dp)
            ) {
                Icon(increaseIcon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun GeneralSection(
    allowTwoPopups: Boolean,
    autoExpand: Boolean,
    sendReplies: Boolean,
    hideInForeground: Boolean,
    showInLandscape: Boolean,
    showAlways: Boolean,
    quickAccessApps: Boolean,
    showOnLockscreen: Boolean,
    hideOnNotificationPanel: Boolean,
    notificationCountOption: Int,
    autoHideSmallPopupHours: Int,
    autoHideExpandedPopupSec: Int,
    hideWhenTouchingOutside: Boolean,
    onAllowTwoPopupsToggle: (Boolean) -> Unit,
    onAutoExpandToggle: (Boolean) -> Unit,
    onSendRepliesToggle: (Boolean) -> Unit,
    onHideInForegroundToggle: (Boolean) -> Unit,
    onShowInLandscapeToggle: (Boolean) -> Unit,
    onShowAlwaysToggle: (Boolean) -> Unit,
    onQuickAccessAppsToggle: (Boolean) -> Unit,
    onShowOnLockscreenToggle: (Boolean) -> Unit,
    onHideOnNotificationPanelToggle: (Boolean) -> Unit,
    onNotificationCountOptionChanged: (Int) -> Unit,
    onAutoHideSmallPopupHoursChanged: (Int) -> Unit,
    onAutoHideExpandedPopupSecChanged: (Int) -> Unit,
    onHideWhenTouchingOutsideToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,             // M3 ExtraLarge = 28dp,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "General",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            // Popup
            Text("Popup", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            ToggleOptionRow("Allow two popups", "Show a second popup if multiple notifications arrive", allowTwoPopups, onAllowTwoPopupsToggle)
            ToggleOptionRow("Auto expand", "Automatically expand notifications from selected apps", autoExpand, onAutoExpandToggle)
            ToggleOptionRow("Send replies", "Allow to send short replies directly from the popup if available in the notification", sendReplies, onSendRepliesToggle)

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

            // Display settings
            Text("Display settings", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            ToggleOptionRow("Hide in foreground", "Hide the popup if the same app is in foreground (like on iPhone)", hideInForeground, onHideInForegroundToggle)
            ToggleOptionRow("Show in landscape", "Allow the popup when the phone is in landscape mode", showInLandscape, onShowInLandscapeToggle)
            ToggleOptionRow("Show always", "Always show a small version of the popup to simulate a camera cutout", showAlways, onShowAlwaysToggle)
            ToggleOptionRow("Quick Access Apps", "Quickly access your favorite apps. Just tap the popup to show the apps. Long press to hide the popup and take a screenshot", quickAccessApps, onQuickAccessAppsToggle)
            ToggleOptionRow("Show on lockscreen", "Show the popup on lockscreen", showOnLockscreen, onShowOnLockscreenToggle)
            ToggleOptionRow("Notification panel", "Hide the popup when the notification panel is visible", hideOnNotificationPanel, onHideOnNotificationPanelToggle)

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

            // Notification count
            Text("Notification count", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            RadioOptionRow("Show latest notification", notificationCountOption == 0) { onNotificationCountOptionChanged(0) }
            RadioOptionRow("Show all notifications", notificationCountOption == 1) { onNotificationCountOptionChanged(1) }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

            // Auto hide - Small popup
            Text("Auto hide - Small popup", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("After this time", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    Text("$autoHideSmallPopupHours hours", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = autoHideSmallPopupHours.toFloat(),
                    onValueChange = { onAutoHideSmallPopupHoursChanged(it.toInt()) },
                    valueRange = 1f..48f
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

            // Auto hide - Expanded popup
            Text("Auto hide - Expanded popup", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("After this time", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    Text("$autoHideExpandedPopupSec seconds", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = autoHideExpandedPopupSec.toFloat(),
                    onValueChange = { onAutoHideExpandedPopupSecChanged(it.toInt()) },
                    valueRange = 2f..30f
                )
            }
            ToggleOptionRow("When touching outside", "", hideWhenTouchingOutside, onHideWhenTouchingOutsideToggle)
        }
    }
}

@Composable
fun EventSimulatorSection(
    onMockBattery: () -> Unit,
    onMockLowBattery: () -> Unit,
    onMockFaceID: () -> Unit,
    onMockFaceIDFail: () -> Unit,
    onMockMobilePay: () -> Unit,
    onMockQuickShare: () -> Unit,
    onMockBluetooth: () -> Unit,
    onMockFocusMode: () -> Unit,
    onMockOngoingCall: () -> Unit,
    onMockIncomingCall: () -> Unit,
    onMockScreenRecord: () -> Unit,
    onMockVoiceMemo: () -> Unit,
    onMockHotspot: () -> Unit,
    onMockRideSharing: () -> Unit,
    onMockFoodDelivery: () -> Unit,
    onMockMusic: () -> Unit,
    onMockTimer: () -> Unit,
    onMockStopwatch: () -> Unit,
    onMockAlarm: () -> Unit,
    onMockNav: () -> Unit,
    onMockProgress: () -> Unit,
    onMockNotification: () -> Unit,
    onMockWifiToggle: () -> Unit,
    onMockFlashlightToggle: () -> Unit,
    onMockDndToggle: () -> Unit,
    onMockSilentToggle: () -> Unit,
    onMockLowPowerToggle: () -> Unit,
    onMockNfc: () -> Unit = {},
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,             // M3 ExtraLarge = 28dp,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row: title + hide/show toggle
            var simulatorExpanded by remember { mutableStateOf(true) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Mock Capsule",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "Test capsule events in real-time",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                // Hide / Show toggle — always visible
                IconButton(onClick = { simulatorExpanded = !simulatorExpanded }) {
                    Icon(
                        imageVector = if (simulatorExpanded) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (simulatorExpanded) "Hide simulator" else "Show simulator",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Collapsible simulator buttons
            AnimatedVisibility(
                visible = simulatorExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // ── Group 1: Calls & Live Activities ─────────────
                    SimulatorGroup("Calls & Live Activities") {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SimBtn(Icons.Rounded.Call, "Ongoing Call", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, onMockOngoingCall)
                            SimBtn(Icons.Rounded.PhoneCallback, "Incoming Call", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, onMockIncomingCall)
                            SimBtn(Icons.Rounded.MusicNote, "Music", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, onMockMusic)
                            SimBtn(Icons.Rounded.Navigation, "Navigation", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, onMockNav)
                        }
                    }

                    // ── Group 2: Timers & Recording ───────────────────
                    SimulatorGroup("Timers & Recording") {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SimBtn(Icons.Rounded.Timer, "Timer (5m)", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, onMockTimer)
                            SimBtn(Icons.Rounded.AvTimer, "Stopwatch", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, onMockStopwatch)
                            SimBtn(Icons.Rounded.Alarm, "Alarm", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, onMockAlarm)
                            SimBtn(Icons.Rounded.FiberManualRecord, "Screen Record", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, onMockScreenRecord)
                            SimBtn(Icons.Rounded.Mic, "Voice Record", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, onMockVoiceMemo)
                        }
                    }

                    // ── Group 3: System Status ────────────────────────
                    SimulatorGroup("System Status") {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SimBtn(Icons.Rounded.BatteryChargingFull, "Charging", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, onMockBattery)
                            SimBtn(Icons.Rounded.BatteryAlert, "Low Battery", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, onMockLowBattery)
                            SimBtn(Icons.Rounded.Headphones, "Bluetooth", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, onMockBluetooth)
                            SimBtn(Icons.Rounded.Router, "Hotspot", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, onMockHotspot)
                            SimBtn(Icons.Rounded.Nfc, "NFC Check", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, onMockNfc)
                        }
                    }

                    // ── Group 4: Quick Toggles ────────────────────────
                    SimulatorGroup("Quick Toggles") {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SimBtn(Icons.Rounded.Wifi, "Wi-Fi", MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), MaterialTheme.colorScheme.onSurface, onMockWifiToggle)
                            SimBtn(Icons.Rounded.FlashlightOn, "Flashlight", MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), MaterialTheme.colorScheme.onSurface, onMockFlashlightToggle)
                            SimBtn(Icons.Rounded.DoNotDisturbOn, "DND", MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), MaterialTheme.colorScheme.onSurface, onMockDndToggle)
                            SimBtn(Icons.Rounded.VolumeOff, "Silent", MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), MaterialTheme.colorScheme.onSurface, onMockSilentToggle)
                            SimBtn(Icons.Rounded.BatterySaver, "Low Power", MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), MaterialTheme.colorScheme.onSurface, onMockLowPowerToggle)
                        }
                    }

                    // ── Group 5: Notifications & Apps ─────────────────
                    SimulatorGroup("Notifications & Apps") {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SimBtn(Icons.Rounded.Message, "Notification", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, onMockNotification)
                            SimBtn(Icons.Rounded.Download, "App Download", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, onMockProgress)
                            SimBtn(Icons.Rounded.DirectionsCar, "Cab Service", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, onMockRideSharing)
                            SimBtn(Icons.Rounded.DeliveryDining, "Food Delivery", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, onMockFoodDelivery)
                        }
                    }

                    // ── Group 6: Authentication ───────────────────────
                    SimulatorGroup("Authentication") {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SimBtn(Icons.Rounded.Face, "Face ID Success", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, onMockFaceID)
                            SimBtn(Icons.Rounded.Face, "Face ID Fail", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, onMockFaceIDFail)
                            SimBtn(Icons.Rounded.CreditCard, "Mobile Pay", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, onMockMobilePay)
                            SimBtn(Icons.Rounded.WifiTethering, "Quick Share", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, onMockQuickShare)
                            SimBtn(Icons.Rounded.Bedtime, "Focus Mode", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, onMockFocusMode)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

            // Clear button â€” ALWAYS visible regardless of collapse state
            Button(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,   // M3 Full/Pill
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Icon(Icons.Rounded.Clear, null)
                Spacer(Modifier.width(6.dp))
                Text("Clear Active Events", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SimBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bgColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = {
            com.example.capsulebar.service.HapticSoundManager.playClick()
            onClick()
        },
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = bgColor, contentColor = textColor),
        shape = MaterialTheme.shapes.extraLarge,   // M3 Full/Pill
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}


@Composable
fun SimulatorGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

private fun copyUriToInternalStorage(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.filesDir, "bluetooth_headset.png")
        val outputStream = FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

fun Modifier.pressClickEffect(): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pressClickScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val down = awaitFirstDown(false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
    }
}

@Composable
fun NfcTasksSection(
    nfcSoundProfileTagId: String,
    nfcQuickLaunchTagId: String,
    onRunNfcCheck: () -> Unit,
    onBindSoundProfile: () -> Unit,
    onBindQuickLaunch: () -> Unit,
    onClearSoundProfile: () -> Unit,
    onClearQuickLaunch: () -> Unit,
    onSimulateSoundProfile: () -> Unit,
    onSimulateQuickLaunch: () -> Unit
) {
    val context = LocalContext.current
    val nfcAdapter = remember {
        try {
            android.nfc.NfcAdapter.getDefaultAdapter(context)
        } catch (_: Exception) {
            null
        }
    }
    val isNfcSupported = nfcAdapter != null
    val isNfcEnabled = nfcAdapter?.isEnabled == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Nfc,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        "NFC Tasks & Check",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                // NFC Hardware Status Badge
                Surface(
                    color = when {
                        !isNfcSupported -> MaterialTheme.colorScheme.surfaceContainerHighest
                        isNfcEnabled -> Color(0xFF2E7D32).copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when {
                            !isNfcSupported -> "No Hardware"
                            isNfcEnabled -> "NFC Ready"
                            else -> "NFC Disabled"
                        },
                        color = when {
                            !isNfcSupported -> MaterialTheme.colorScheme.onSurfaceVariant
                            isNfcEnabled -> Color(0xFF81C784)
                            else -> MaterialTheme.colorScheme.error
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // ── DEDICATED NFC CHECK CARD ─────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "NFC Check",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Simulates an NFC tag read to test live Capsule Bar animation & reaction",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                com.example.capsulebar.service.HapticSoundManager.playClick()
                                onRunNfcCheck()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Sensors, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Run NFC Check", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        if (isNfcSupported && !isNfcEnabled) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(android.provider.Settings.ACTION_NFC_SETTINGS))
                                    } catch (_: Exception) {}
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Enable NFC", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

            Text(
                "Automated Tag Actions",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            // Task 1: Sound Profile Toggle
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Sound Profile Toggle",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    "Scans tag to toggle sound: Ring ↔ Vibrate",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (nfcSoundProfileTagId.isNotEmpty()) {
                        Text(
                            "Bound tag: $nfcSoundProfileTagId",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = onClearSoundProfile) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Clear tag ID", tint = MaterialTheme.colorScheme.error)
                            }
                            Button(onClick = onSimulateSoundProfile) {
                                Text("Test Action", fontSize = 12.sp)
                            }
                        }
                    } else {
                        Text(
                            "Not bound",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onBindSoundProfile) {
                                Text("Bind Tag", fontSize = 12.sp)
                            }
                            Button(
                                onClick = onSimulateSoundProfile,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Test Action", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))

            // Task 2: Quick Launch Action
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Quick Launch Action",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    "Scans tag to turn on Bluetooth & launch Music Player",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (nfcQuickLaunchTagId.isNotEmpty()) {
                        Text(
                            "Bound tag: $nfcQuickLaunchTagId",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = onClearQuickLaunch) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Clear tag ID", tint = MaterialTheme.colorScheme.error)
                            }
                            Button(onClick = onSimulateQuickLaunch) {
                                Text("Test Action", fontSize = 12.sp)
                            }
                        }
                    } else {
                        Text(
                            "Not bound",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onBindQuickLaunch) {
                                Text("Bind Tag", fontSize = 12.sp)
                            }
                            Button(
                                onClick = onSimulateQuickLaunch,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Test Action", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}




