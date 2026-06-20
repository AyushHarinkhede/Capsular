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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.capsulebar.R
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
    val bluetoothImagePath by viewModel.bluetoothImagePath.collectAsStateWithLifecycle()

    val compactDurationSec by viewModel.compactDurationSec.collectAsStateWithLifecycle()
    val expandedDurationSec by viewModel.expandedDurationSec.collectAsStateWithLifecycle()
    val dismissDelaySec by viewModel.dismissDelaySec.collectAsStateWithLifecycle()

    // Screen Option State Collections
    val maxPopupWidthPercent by viewModel.maxPopupWidthPercent.collectAsStateWithLifecycle()
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
    val nfcWristWatchTagId by viewModel.nfcWristWatchTagId.collectAsStateWithLifecycle()
    val nfcChetakTagId by viewModel.nfcChetakTagId.collectAsStateWithLifecycle()
    val activeRegistrationTask by viewModel.activeRegistrationTask.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── TOP LOGO + TITLE BAR ──────────────────────────────────────────────────
        // M3 ExtraLarge shape (28dp) — the iconic Pixel widget / Material You shape.
        // Background uses the system dynamic primary color (wallpaper-extracted on API 31+).
        Row(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(
                    Brush.linearGradient(
                        0.0f to MaterialTheme.colorScheme.primary,
                        0.65f to MaterialTheme.colorScheme.primary,
                        1.0f to MaterialTheme.colorScheme.tertiary.copy(alpha = 0.80f)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Capsular logo",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Capsular",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 26.sp
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

        // ── POSITION & SIZE ──────────────────────────────────────────────────────
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
                cameraPosition = cameraPosition,
                cameraWidthDp = cameraWidthDp,
                isCalibrationMode = isCalibrationMode,
                isServiceRunning = isServiceRunning,
                splitPosition = splitPosition,
                onXChanged = { viewModel.updateXOffset(it) },
                onYChanged = { viewModel.updateYOffset(it) },
                onWidthChanged = { viewModel.updateWidthDp(it) },
                onHeightChanged = { viewModel.updateHeightDp(it) },
                onEdgeRoundingChanged = { viewModel.updateEdgeRoundingPercent(it) },
                onMaxPopupWidthChanged = { viewModel.updateMaxPopupWidthPercent(it) },
                onCameraPositionChanged = { viewModel.updateCameraPosition(it) },
                onCameraWidthChanged = { viewModel.updateCameraWidthDp(it) },
                onToggleCalibration = { viewModel.toggleCalibrationMode(it) },
                onSplitPositionChanged = { viewModel.updateSplitPosition(it) },
                onReset = {
                    viewModel.updateXOffset(0)
                    viewModel.updateYOffset(15)
                    viewModel.updateWidthDp(110)
                    viewModel.updateHeightDp(36)
                    viewModel.updateEdgeRoundingPercent(60)
                    viewModel.updateMaxPopupWidthPercent(100)
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
                onUploadBluetoothImage = { path -> viewModel.updateBluetoothImage(path) }
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
                nfcWristWatchTagId = nfcWristWatchTagId,
                nfcChetakTagId = nfcChetakTagId,
                onBindWristWatch = { viewModel.startNfcRegistration("wrist_watch") },
                onBindChetak = { viewModel.startNfcRegistration("chetak") },
                onClearWristWatch = { viewModel.clearNfcTag("wrist_watch") },
                onClearChetak = { viewModel.clearNfcTag("chetak") },
                onSimulateWristWatch = { CapsuleStateManager.processNfcTag(context, "MOCK_WRIST_WATCH_TAG") },
                onSimulateChetak = { CapsuleStateManager.processNfcTag(context, "MOCK_CHETAK_TAG") }
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
                onClear = { viewModel.clearEvents() }
            )
        }

        if (activeRegistrationTask != null) {
            val taskName = if (activeRegistrationTask == "wrist_watch") "Wrist Watch Task" else "Chetak Task"
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
                            "Approach your NFC tag to the back of the phone to bind it to: $taskName.\n\n(Or click 'Test Scan' below to simulate a scan)",
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {},
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
                    onToggleService()
                },
                enabled = true,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
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
    cameraPosition: String,
    cameraWidthDp: Int,
    isCalibrationMode: Boolean,
    isServiceRunning: Boolean,
    splitPosition: String,
    onXChanged: (Int) -> Unit,
    onYChanged: (Int) -> Unit,
    onWidthChanged: (Int) -> Unit,
    onHeightChanged: (Int) -> Unit,
    onEdgeRoundingChanged: (Int) -> Unit,
    onMaxPopupWidthChanged: (Int) -> Unit,
    onCameraPositionChanged: (String) -> Unit,
    onCameraWidthChanged: (Int) -> Unit,
    onToggleCalibration: (Boolean) -> Unit,
    onSplitPositionChanged: (String) -> Unit,
    onReset: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,             // M3 ExtraLarge = 28dp,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Position",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "Change size and position",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                
                if (isServiceRunning) {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleCalibration(!isCalibrationMode)
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isCalibrationMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isCalibrationMode) Icons.Rounded.Close else Icons.Rounded.CenterFocusStrong,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isCalibrationMode) "Hide Outline" else "Show Outline",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!isServiceRunning) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Start overlay service to enable Show Outline alignment directly on screen.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Camera Cutout Position
            Column {
                Text("Camera Cutout Position", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val positions = listOf("Left", "Center", "Right")
                    positions.forEach { pos ->
                        val isSelected = cameraPosition == pos
                        val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                // M3 Full/Pill shape for segmented control buttons
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(bg)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onCameraPositionChanged(pos)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pos,
                                color = textColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Camera Cutout Width
            SliderOptionRow(
                title = "Camera Cutout Width",
                valueText = "${cameraWidthDp}dp",
                value = cameraWidthDp.toFloat(),
                onValueChange = { onCameraWidthChanged(it.toInt()) },
                valueRange = 15f..80f,
                decreaseIcon = Icons.Rounded.Remove,
                increaseIcon = Icons.Rounded.Add
            )

            // Split Capsule Position
            Column {
                Text(
                    "Side Capsule Position",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Text(
                    "Which side the split circle appears on",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val splitOptions = listOf("Left", "Right")
                    splitOptions.forEach { side ->
                        val isSelected = splitPosition == side
                        val bg = if (isSelected) MaterialTheme.colorScheme.secondary
                                 else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        val textColor = if (isSelected) MaterialTheme.colorScheme.onSecondary
                                        else MaterialTheme.colorScheme.onSurface
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(MaterialTheme.shapes.extraLarge)    // M3 Full/Pill
                                .background(bg)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSplitPositionChanged(side)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (side == "Left") Icons.Rounded.KeyboardArrowLeft
                                                  else Icons.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "$side Side",
                                    color = textColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Position horizontal
            SliderOptionRow(
                title = "Position horizontal",
                valueText = "${xOffset}px",
                value = xOffset.toFloat(),
                onValueChange = { onXChanged(it.toInt()) },
                valueRange = -300f..300f,
                decreaseIcon = Icons.Rounded.ChevronLeft,
                increaseIcon = Icons.Rounded.ChevronRight
            )

            // Position vertical
            SliderOptionRow(
                title = "Position vertical",
                valueText = "${yOffset}px",
                value = yOffset.toFloat(),
                onValueChange = { onYChanged(it.toInt()) },
                valueRange = 0f..200f,
                decreaseIcon = Icons.Rounded.KeyboardArrowUp,
                increaseIcon = Icons.Rounded.KeyboardArrowDown
            )

            // Size horizontal
            SliderOptionRow(
                title = "Size horizontal",
                valueText = "${widthDp}dp",
                value = widthDp.toFloat(),
                onValueChange = { onWidthChanged(it.toInt()) },
                valueRange = 70f..250f,
                decreaseIcon = Icons.Rounded.Remove,
                increaseIcon = Icons.Rounded.Add
            )

            // Size vertical
            SliderOptionRow(
                title = "Size vertical",
                valueText = "${heightDp}dp",
                value = heightDp.toFloat(),
                onValueChange = { onHeightChanged(it.toInt()) },
                valueRange = 25f..60f,
                decreaseIcon = Icons.Rounded.KeyboardArrowUp,
                increaseIcon = Icons.Rounded.KeyboardArrowDown
            )

            // Reset
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onReset()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Reset", fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

            // General Subtitle
            Text(
                "General",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            // Edge rounding
            SliderOptionRow(
                title = "Edge rounding",
                valueText = "${edgeRoundingPercent}%",
                value = edgeRoundingPercent.toFloat(),
                onValueChange = { onEdgeRoundingChanged(it.toInt()) },
                valueRange = 0f..100f,
                decreaseIcon = Icons.Rounded.Remove,
                increaseIcon = Icons.Rounded.Add
            )

            // Maximum popup width
            SliderOptionRow(
                title = "Maximum popup width",
                valueText = "${maxPopupWidthPercent}%",
                value = maxPopupWidthPercent.toFloat(),
                onValueChange = { onMaxPopupWidthChanged(it.toInt()) },
                valueRange = 50f..100f,
                decreaseIcon = Icons.Rounded.Remove,
                increaseIcon = Icons.Rounded.Add
            )
        }
    }
}

@Composable
fun AppearanceSection(
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
    onUploadBluetoothImage: (String?) -> Unit
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
                "Appearance",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

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
            .clickable { onCheckedChange(!checked) }
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
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
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
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
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
                onClick = { onValueChange((value - step).coerceIn(valueRange.start, valueRange.endInclusive)) },
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                    .size(32.dp)
            ) {
                Icon(decreaseIcon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onValueChange((value + step).coerceIn(valueRange.start, valueRange.endInclusive)) },
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
        onClick = onClick,
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
    nfcWristWatchTagId: String,
    nfcChetakTagId: String,
    onBindWristWatch: () -> Unit,
    onBindChetak: () -> Unit,
    onClearWristWatch: () -> Unit,
    onClearChetak: () -> Unit,
    onSimulateWristWatch: () -> Unit,
    onSimulateChetak: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "NFC Tasks",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            // Wrist Watch Task
            Column {
                Text(
                    "Wrist Watch Task (Sound Profile Toggle)",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    "Scans to toggle sound profile: Ring <-> Vibrate",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (nfcWristWatchTagId.isNotEmpty()) {
                        Text(
                            "Bound tag: $nfcWristWatchTagId",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = onClearWristWatch) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Clear tag ID", tint = MaterialTheme.colorScheme.error)
                            }
                            Button(onClick = onSimulateWristWatch) {
                                Text("Test Scan", fontSize = 12.sp)
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
                            Button(onClick = onBindWristWatch) {
                                Text("Bind Tag", fontSize = 12.sp)
                            }
                            Button(
                                onClick = onSimulateWristWatch,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Test Scan", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

            // Chetak Task
            Column {
                Text(
                    "Chetak Task (Bluetooth + Launch App)",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    "Scans to turn on Bluetooth and launch Chetak App/Oto Music (after 2s delay)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (nfcChetakTagId.isNotEmpty()) {
                        Text(
                            "Bound tag: $nfcChetakTagId",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = onClearChetak) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Clear tag ID", tint = MaterialTheme.colorScheme.error)
                            }
                            Button(onClick = onSimulateChetak) {
                                Text("Test Scan", fontSize = 12.sp)
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
                            Button(onClick = onBindChetak) {
                                Text("Bind Tag", fontSize = 12.sp)
                            }
                            Button(
                                onClick = onSimulateChetak,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Test Scan", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}




