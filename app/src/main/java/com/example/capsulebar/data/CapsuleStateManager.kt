package com.example.capsulebar.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CapsuleUiState(
    val mainEvent: CapsuleEvent? = null,
    val splitEvent: CapsuleEvent? = null,
    val displayMode: DisplayMode = DisplayMode.COLLAPSED,
    val isHidden: Boolean = false
)

enum class DisplayMode {
    COLLAPSED,
    EXPANDED,
    SPLIT,
    HIDDEN
}

object CapsuleStateManager {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _uiState = MutableStateFlow(CapsuleUiState())
    val uiState: StateFlow<CapsuleUiState> = _uiState.asStateFlow()

    private val _isRealVisualizerActive = MutableStateFlow(false)
    val isRealVisualizerActive: StateFlow<Boolean> = _isRealVisualizerActive.asStateFlow()

    private val _visualizerAmplitudes = MutableStateFlow(listOf(0.3f, 0.3f, 0.3f, 0.3f))
    val visualizerAmplitudes: StateFlow<List<Float>> = _visualizerAmplitudes.asStateFlow()

    private val _isNotificationPanelVisible = MutableStateFlow(false)
    val isNotificationPanelVisible: StateFlow<Boolean> = _isNotificationPanelVisible.asStateFlow()

    private val _isInputFocused = MutableStateFlow(false)
    val isInputFocused: StateFlow<Boolean> = _isInputFocused.asStateFlow()

    private val _themeMode = MutableStateFlow("dark")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(true)
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _soundEffectsEnabled = MutableStateFlow(true)
    val soundEffectsEnabled: StateFlow<Boolean> = _soundEffectsEnabled.asStateFlow()

    private var prefs: android.content.SharedPreferences? = null
    private var isManuallyHidden = false
    private var preferredMainEventId: String? = null
    private var collapseJob: Job? = null
    private var hideJob: Job? = null
    var currentForegroundPackage: String? = null

    fun setInputFocused(focused: Boolean) {
        if (_isInputFocused.value != focused) {
            _isInputFocused.value = focused
            if (focused) {
                keepExpandedAlive()
            }
        }
    }

    fun updateForegroundPackage(packageName: String) {
        if (currentForegroundPackage != packageName) {
            currentForegroundPackage = packageName
            recalculateState()
        }
    }

    fun initialize(context: android.content.Context) {
        prefs = context.applicationContext.getSharedPreferences("capsule_settings", android.content.Context.MODE_PRIVATE)
        _themeMode.value = prefs?.getString("theme_mode", "dark") ?: "dark"
        _hapticsEnabled.value = prefs?.getBoolean("haptics_enabled", true) ?: true
        _soundEffectsEnabled.value = prefs?.getBoolean("sound_effects_enabled", true) ?: true

        // Also initialize sound & haptic managers
        com.example.capsulebar.service.HapticManager.init(context)
        com.example.capsulebar.service.SoundManager.init(context)

        prefs?.registerOnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "theme_mode" -> _themeMode.value = prefs?.getString("theme_mode", "dark") ?: "dark"
                "haptics_enabled" -> _hapticsEnabled.value = prefs?.getBoolean("haptics_enabled", true) ?: true
                "sound_effects_enabled" -> _soundEffectsEnabled.value = prefs?.getBoolean("sound_effects_enabled", true) ?: true
                "show_always", "dismiss_delay_sec", "hide_on_notification_panel",
                "allow_two_popups", "reverse_order", "hide_in_foreground",
                "auto_hide_expanded_popup_sec", "auto_hide_small_popup_hours" -> recalculateState()
            }
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs?.edit()?.putString("theme_mode", mode)?.apply()
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _hapticsEnabled.value = enabled
        prefs?.edit()?.putBoolean("haptics_enabled", enabled)?.apply()
    }

    fun setSoundEffectsEnabled(enabled: Boolean) {
        _soundEffectsEnabled.value = enabled
        prefs?.edit()?.putBoolean("sound_effects_enabled", enabled)?.apply()
    }

    private fun getCompactDurationMs(): Long {
        return (prefs?.getInt("compact_duration_sec", 5) ?: 5) * 1000L
    }

    private fun getExpandedDurationMs(): Long {
        val sec = prefs?.getInt("auto_hide_expanded_popup_sec", prefs?.getInt("expanded_duration_sec", 8) ?: 8) ?: 8
        return sec * 1000L
    }

    private fun getDismissDelayMs(): Long {
        return (prefs?.getInt("dismiss_delay_sec", 3) ?: 3) * 1000L
    }

    fun setRealVisualizerActive(active: Boolean) {
        _isRealVisualizerActive.value = active
    }

    fun updateAmplitudes(amplitudes: List<Float>) {
        _visualizerAmplitudes.value = amplitudes
    }

    var onMediaActionListener: ((String) -> Unit)? = null

    fun sendMediaAction(action: String) {
        keepExpandedAlive()
        applyOptimisticMusicUpdate(action)
        onMediaActionListener?.invoke(action)
    }

    @Synchronized
    private fun applyOptimisticMusicUpdate(action: String) {
        val currentMusic = (activeEvents["music"] as? CapsuleEvent.Music)
            ?: (_uiState.value.mainEvent as? CapsuleEvent.Music)
            ?: return

        val updated = when {
            action == "play" -> currentMusic.copy(isPlaying = true)
            action == "pause" -> currentMusic.copy(isPlaying = false)
            action == "rewind" || action == "reverse" -> {
                val target = (currentMusic.position - 10_000L).coerceAtLeast(0L)
                currentMusic.copy(position = target)
            }
            action == "fast_forward" || action == "forward" -> {
                val cap = if (currentMusic.duration > 0L) currentMusic.duration else (currentMusic.position + 10_000L)
                val target = (currentMusic.position + 10_000L).coerceAtMost(cap)
                currentMusic.copy(position = target)
            }
            action.startsWith("seek:") -> {
                val pos = action.removePrefix("seek:").toLongOrNull() ?: currentMusic.position
                currentMusic.copy(position = pos.coerceAtLeast(0L))
            }
            action == "toggle_like" || action == "like" || action == "unlike" -> {
                val nextLiked = !currentMusic.isLiked
                currentMusic.copy(
                    isLiked = nextLiked,
                    isDisliked = if (nextLiked) false else currentMusic.isDisliked
                )
            }
            action == "toggle_dislike" || action == "dislike" -> {
                val nextDisliked = !currentMusic.isDisliked
                currentMusic.copy(
                    isDisliked = nextDisliked,
                    isLiked = if (nextDisliked) false else currentMusic.isLiked
                )
            }
            action == "toggle_shuffle" || action == "shuffle" -> {
                currentMusic.copy(isShuffleEnabled = !currentMusic.isShuffleEnabled)
            }
            action == "toggle_repeat" || action == "repeat" -> {
                currentMusic.copy(isRepeatEnabled = !currentMusic.isRepeatEnabled)
            }
            action == "cycle_speed" -> {
                val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f, 0.75f)
                val idx = speeds.indexOfFirst { kotlin.math.abs(it - currentMusic.playbackSpeed) < 0.05f }
                val nextSpeed = speeds[(if (idx >= 0) idx + 1 else 1) % speeds.size]
                currentMusic.copy(playbackSpeed = nextSpeed)
            }
            else -> null
        }

        if (updated != null) {
            activeEvents[updated.id] = updated
            recalculateState()
        }
    }

    private val activeEvents = mutableMapOf<String, CapsuleEvent>()
    private val autoDismissJobs = mutableMapOf<String, Job>()
    private var clockTickerJob: Job? = null
    private var lastLapStopwatchSec: Long = 0L

    @Synchronized
    fun postEvent(event: CapsuleEvent) {
        autoDismissJobs[event.id]?.cancel()

        val nowMs = System.currentTimeMillis()
        val normalizedEvent = when (event) {
            is CapsuleEvent.Timer -> {
                val prev = activeEvents[event.id] as? CapsuleEvent.Timer
                val resolvedTotal = when {
                    event.totalSeconds > event.remainingSeconds -> event.totalSeconds
                    prev != null && prev.totalSeconds >= event.remainingSeconds -> prev.totalSeconds
                    else -> event.remainingSeconds.coerceAtLeast(1L)
                }
                val resolvedEndMs = if (event.isRunning && event.remainingSeconds > 0L) {
                    nowMs + event.remainingSeconds * 1000L
                } else {
                    0L
                }
                event.copy(
                    totalSeconds = resolvedTotal,
                    targetEndElapsedMs = resolvedEndMs
                )
            }
            is CapsuleEvent.Stopwatch -> {
                val prev = activeEvents[event.id] as? CapsuleEvent.Stopwatch
                val preservedLaps = if (event.laps.isNotEmpty()) {
                    event.laps
                } else if (event.elapsedSeconds == 0L && !event.isRunning) {
                    lastLapStopwatchSec = 0L
                    emptyList()
                } else {
                    prev?.laps ?: emptyList()
                }
                val resolvedBaseMs = if (event.isRunning) {
                    nowMs - (event.elapsedSeconds * 1000L)
                } else {
                    0L
                }
                event.copy(
                    laps = preservedLaps,
                    baseStartElapsedMs = resolvedBaseMs
                )
            }
            else -> event
        }

        activeEvents[normalizedEvent.id] = normalizedEvent

        val duration = if (normalizedEvent.durationMs > 0) {
            normalizedEvent.durationMs
        } else {
            0L
        }

        if (duration > 0) {
            autoDismissJobs[normalizedEvent.id] = scope.launch {
                delay(duration)
                removeEvent(normalizedEvent.id)
            }
        }

        ensureClockTickerState()
        recalculateState()
    }

    @Synchronized
    private fun ensureClockTickerState() {
        val hasRunningClock = activeEvents.values.any {
            (it is CapsuleEvent.Timer && it.isRunning && it.remainingSeconds > 0L) ||
            (it is CapsuleEvent.Stopwatch && it.isRunning)
        }
        if (hasRunningClock && (clockTickerJob == null || clockTickerJob?.isActive != true)) {
            clockTickerJob = scope.launch {
                while (isActive) {
                    delay(1000L)
                    tickRunningClocks()
                }
            }
        } else if (!hasRunningClock) {
            clockTickerJob?.cancel()
            clockTickerJob = null
        }
    }

    @Synchronized
    private fun tickRunningClocks() {
        val nowMs = System.currentTimeMillis()
        var changed = false
        val entries = activeEvents.entries.toList()
        for ((key, ev) in entries) {
            when (ev) {
                is CapsuleEvent.Timer -> {
                    if (ev.isRunning && ev.remainingSeconds > 0L) {
                        val nextRem = if (ev.targetEndElapsedMs > 0L) {
                            ((ev.targetEndElapsedMs - nowMs + 499L) / 1000L).coerceAtLeast(0L)
                        } else {
                            (ev.remainingSeconds - 1L).coerceAtLeast(0L)
                        }
                        if (nextRem == 0L) {
                            activeEvents[key] = ev.copy(
                                remainingSeconds = 0L,
                                isRunning = false,
                                isFiring = true,
                                targetEndElapsedMs = 0L,
                                priority = 940
                            )
                            try {
                                com.example.capsulebar.service.HapticSoundManager.playExpand()
                            } catch (_: Exception) {}
                        } else {
                            activeEvents[key] = ev.copy(remainingSeconds = nextRem)
                        }
                        changed = true
                    }
                }
                is CapsuleEvent.Stopwatch -> {
                    if (ev.isRunning) {
                        val nextElapsed = if (ev.baseStartElapsedMs > 0L) {
                            ((nowMs - ev.baseStartElapsedMs) / 1000L).coerceAtLeast(0L)
                        } else {
                            ev.elapsedSeconds + 1L
                        }
                        activeEvents[key] = ev.copy(elapsedSeconds = nextElapsed)
                        changed = true
                    }
                }
                else -> {}
            }
        }
        if (changed) {
            ensureClockTickerState()
            recalculateState()
        }
    }

    fun sendTimerAction(context: android.content.Context?, eventId: String, action: String) {
        keepExpandedAlive()
        val current = (activeEvents[eventId] as? CapsuleEvent.Timer)
            ?: (activeEvents.values.firstOrNull { it is CapsuleEvent.Timer } as? CapsuleEvent.Timer)
            ?: return
        val id = current.id
        val nowMs = System.currentTimeMillis()

        when {
            action == "toggle" || action == "pause" || action == "start" || action == "resume" -> {
                val nextRunning = when (action) {
                    "pause" -> false
                    "start", "resume" -> true
                    else -> !current.isRunning
                }
                val rem = if (current.remainingSeconds <= 0L && nextRunning) current.totalSeconds.coerceAtLeast(60L) else current.remainingSeconds
                val triggered = com.example.capsulebar.service.CapsuleNotificationListener.triggerNotificationAction(
                    id,
                    if (nextRunning) "start" else "pause"
                )
                postEvent(
                    current.copy(
                        remainingSeconds = rem,
                        isRunning = nextRunning,
                        isFiring = false,
                        isSystemSynced = current.isSystemSynced || triggered,
                        targetEndElapsedMs = if (nextRunning) nowMs + rem * 1000L else 0L,
                        priority = 350
                    )
                )
            }
            action.startsWith("+") -> {
                val addSec = action.removePrefix("+").removeSuffix("s").removeSuffix("m").let { raw ->
                    if (action.endsWith("m")) (raw.toLongOrNull() ?: 1L) * 60L
                    else raw.toLongOrNull() ?: 60L
                }
                val nextRem = current.remainingSeconds + addSec
                val nextTotal = (current.totalSeconds + addSec).coerceAtLeast(nextRem)
                val nextRunning = if (current.isFiring) true else current.isRunning
                val triggered = if (addSec == 60L) {
                    com.example.capsulebar.service.CapsuleNotificationListener.triggerNotificationAction(id, "+1")
                } else false
                postEvent(
                    current.copy(
                        remainingSeconds = nextRem,
                        totalSeconds = nextTotal,
                        isRunning = nextRunning,
                        isFiring = false,
                        isSystemSynced = current.isSystemSynced || triggered,
                        targetEndElapsedMs = if (nextRunning) nowMs + nextRem * 1000L else 0L,
                        priority = 350
                    )
                )
            }
            action.startsWith("preset:") -> {
                val presetSec = action.removePrefix("preset:").toLongOrNull() ?: 60L
                val synced = if (context != null) {
                    com.example.capsulebar.service.CapsuleNotificationListener.startSystemClockTimer(
                        context = context,
                        seconds = presetSec.toInt(),
                        label = current.label.ifBlank { "Timer" }
                    )
                } else false
                postEvent(
                    current.copy(
                        remainingSeconds = presetSec,
                        totalSeconds = presetSec,
                        isRunning = true,
                        isFiring = false,
                        isSystemSynced = current.isSystemSynced || synced,
                        targetEndElapsedMs = nowMs + presetSec * 1000L,
                        priority = 350
                    )
                )
            }
            action == "sync_system" -> {
                if (context != null && current.remainingSeconds > 0L) {
                    val synced = com.example.capsulebar.service.CapsuleNotificationListener.startSystemClockTimer(
                        context = context,
                        seconds = current.remainingSeconds.toInt(),
                        label = current.label.ifBlank { "Timer" }
                    )
                    postEvent(current.copy(isSystemSynced = synced || current.isSystemSynced, isRunning = true))
                }
            }
            action == "reset" -> {
                com.example.capsulebar.service.CapsuleNotificationListener.triggerNotificationAction(id, "reset")
                val resetSec = current.totalSeconds.coerceAtLeast(60L)
                postEvent(
                    current.copy(
                        remainingSeconds = resetSec,
                        isRunning = false,
                        isFiring = false,
                        targetEndElapsedMs = 0L,
                        priority = 350
                    )
                )
            }
            action == "stop" || action == "dismiss" -> {
                com.example.capsulebar.service.CapsuleNotificationListener.triggerNotificationAction(id, "stop")
                com.example.capsulebar.service.CapsuleNotificationListener.dismissNotification(id)
                removeEvent(id)
            }
            action == "open_clock" -> {
                if (context != null) {
                    com.example.capsulebar.service.CapsuleNotificationListener.openClockApp(
                        context = context,
                        eventId = id,
                        packageName = current.packageName,
                        isTimer = true
                    )
                    collapseToCompact()
                }
            }
        }
    }

    fun sendStopwatchAction(context: android.content.Context?, eventId: String, action: String) {
        keepExpandedAlive()
        val current = (activeEvents[eventId] as? CapsuleEvent.Stopwatch)
            ?: (activeEvents.values.firstOrNull { it is CapsuleEvent.Stopwatch } as? CapsuleEvent.Stopwatch)
            ?: return
        val id = current.id
        val nowMs = System.currentTimeMillis()

        when (action) {
            "toggle", "pause", "start", "resume" -> {
                val nextRunning = when (action) {
                    "pause" -> false
                    "start", "resume" -> true
                    else -> !current.isRunning
                }
                val triggered = com.example.capsulebar.service.CapsuleNotificationListener.triggerNotificationAction(
                    id,
                    if (nextRunning) "start" else "pause"
                )
                postEvent(
                    current.copy(
                        isRunning = nextRunning,
                        isSystemSynced = current.isSystemSynced || triggered,
                        baseStartElapsedMs = if (nextRunning) nowMs - (current.elapsedSeconds * 1000L) else 0L
                    )
                )
            }
            "lap" -> {
                com.example.capsulebar.service.CapsuleNotificationListener.triggerNotificationAction(id, "lap")
                val totalSec = current.elapsedSeconds
                val splitDelta = (totalSec - lastLapStopwatchSec).coerceAtLeast(0L)
                lastLapStopwatchSec = totalSec
                val lapNum = current.laps.size + 1
                val totalFmt = "%02d:%02d".format((totalSec % 3600) / 60, totalSec % 60)
                val deltaFmt = "+%02d:%02d".format((splitDelta % 3600) / 60, splitDelta % 60)
                val newLapEntry = "L$lapNum  $deltaFmt  •  $totalFmt"
                val updatedLaps = (listOf(newLapEntry) + current.laps).take(6)
                postEvent(current.copy(laps = updatedLaps))
            }
            "reset" -> {
                com.example.capsulebar.service.CapsuleNotificationListener.triggerNotificationAction(id, "reset")
                lastLapStopwatchSec = 0L
                postEvent(
                    current.copy(
                        elapsedSeconds = 0L,
                        isRunning = false,
                        laps = emptyList(),
                        baseStartElapsedMs = 0L
                    )
                )
            }
            "stop", "dismiss" -> {
                com.example.capsulebar.service.CapsuleNotificationListener.triggerNotificationAction(id, "reset")
                com.example.capsulebar.service.CapsuleNotificationListener.dismissNotification(id)
                lastLapStopwatchSec = 0L
                removeEvent(id)
            }
            "open_clock" -> {
                if (context != null) {
                    com.example.capsulebar.service.CapsuleNotificationListener.openClockApp(
                        context = context,
                        eventId = id,
                        packageName = current.packageName,
                        isTimer = false
                    )
                    collapseToCompact()
                }
            }
        }
    }

    @Synchronized
    fun removeEvent(id: String) {
        autoDismissJobs[id]?.cancel()
        autoDismissJobs.remove(id)
        activeEvents.remove(id)
        if (preferredMainEventId == id) {
            preferredMainEventId = null
        }
        ensureClockTickerState()
        recalculateState()
    }

    @Synchronized
    fun clearAllEvents() {
        autoDismissJobs.values.forEach { it.cancel() }
        autoDismissJobs.clear()
        activeEvents.clear()
        preferredMainEventId = null
        ensureClockTickerState()
        recalculateState()
    }

    @Synchronized
    fun isEventActive(id: String): Boolean {
        return activeEvents.containsKey(id)
    }

    /**
     * Resets the auto-collapse countdown when the user is actively interacting
     * with controls inside the expanded capsule.
     */
    fun keepExpandedAlive() {
        if (_uiState.value.displayMode != DisplayMode.EXPANDED) return
        collapseJob?.cancel()
        val duration = getExpandedDurationMs()
        if (duration > 0 && !_isInputFocused.value) {
            collapseJob = scope.launch {
                delay(duration)
                val current = _uiState.value
                if (current.displayMode == DisplayMode.EXPANDED && !_isInputFocused.value) {
                    val newMode = if (current.splitEvent != null) DisplayMode.SPLIT else DisplayMode.COLLAPSED
                    setDisplayMode(newMode)
                }
            }
        }
    }

    /**
     * Swaps the secondary split event into the main capsule and expands/focuses it.
     */
    @Synchronized
    fun swapMainAndSplit(expandAfterSwap: Boolean = true) {
        val current = _uiState.value
        val split = current.splitEvent ?: return
        preferredMainEventId = split.id
        recalculateState()
        if (expandAfterSwap) {
            setDisplayMode(DisplayMode.EXPANDED)
        }
    }

    fun setDisplayMode(mode: DisplayMode) {
        val prevMode = _uiState.value.displayMode
        collapseJob?.cancel()
        if (mode != DisplayMode.EXPANDED) {
            _isInputFocused.value = false
        }
        _uiState.value = _uiState.value.copy(displayMode = mode, isHidden = mode == DisplayMode.HIDDEN)

        if (prevMode == DisplayMode.EXPANDED && (mode == DisplayMode.COLLAPSED || mode == DisplayMode.SPLIT || mode == DisplayMode.HIDDEN)) {
            com.example.capsulebar.service.HapticSoundManager.playCollapse()
            // DO NOT kill active events just because user collapsed the expanded popup.
            // If the event originally had an auto-dismiss duration (transient notification), resume its remaining time.
            val mainEv = _uiState.value.mainEvent
            if (mainEv != null && mainEv.durationMs > 0) {
                autoDismissJobs[mainEv.id]?.cancel()
                autoDismissJobs[mainEv.id] = scope.launch {
                    delay(mainEv.durationMs)
                    removeEvent(mainEv.id)
                }
            }
        }
        
        if (mode == DisplayMode.EXPANDED) {
            val mainId = _uiState.value.mainEvent?.id
            if (mainId != null) {
                autoDismissJobs[mainId]?.cancel()
            }
            keepExpandedAlive()
        }
    }

    fun toggleExpanded() {
        val current = _uiState.value
        val newMode = if (current.displayMode == DisplayMode.EXPANDED) {
            if (current.splitEvent != null) DisplayMode.SPLIT else DisplayMode.COLLAPSED
        } else {
            DisplayMode.EXPANDED
        }
        setDisplayMode(newMode)
    }

    /**
     * Collapse the expanded island back to compact/split state.
     * Called on swipe-up gesture from the expanded card — mirrors Dynamic Island
     * dismiss behavior (pill stays visible, just shrinks back).
     */
    fun collapseToCompact() {
        val current = _uiState.value
        if (current.displayMode == DisplayMode.EXPANDED) {
            val newMode = if (current.splitEvent != null) DisplayMode.SPLIT else DisplayMode.COLLAPSED
            setDisplayMode(newMode)
        }
    }

    fun hideTemporarily() {
        isManuallyHidden = true
        _isInputFocused.value = false
        _uiState.value = _uiState.value.copy(isHidden = true, displayMode = DisplayMode.HIDDEN)
    }

    fun show() {
        isManuallyHidden = false
        _uiState.value = _uiState.value.copy(isHidden = false)
        recalculateState()
    }

    private fun recalculateState() {
        val hideInForeground = prefs?.getBoolean("hide_in_foreground", true) ?: true
        val foregroundPkg = currentForegroundPackage
        val isCurrentlyExpanded = _uiState.value.displayMode == DisplayMode.EXPANDED
        val eventsToConsider = if (hideInForeground && !foregroundPkg.isNullOrEmpty() && !isCurrentlyExpanded) {
            activeEvents.values.filter { event ->
                val pkg = when (event) {
                    is CapsuleEvent.Notification -> event.packageName
                    is CapsuleEvent.Music -> event.packageName
                    is CapsuleEvent.Call -> if (event.id.startsWith("call_")) event.id.removePrefix("call_") else ""
                    else -> ""
                }
                pkg.isEmpty() || pkg != foregroundPkg
            }
        } else {
            activeEvents.values.toList()
        }

        val reverseOrder = prefs?.getBoolean("reverse_order", false) ?: false
        val baseSortedList = if (reverseOrder) {
            eventsToConsider.sortedWith(compareBy<CapsuleEvent> { it.priority }.thenByDescending { it.id })
        } else {
            eventsToConsider.sortedWith(compareByDescending<CapsuleEvent> { it.priority }.thenBy { it.id })
        }

        val preferredId = preferredMainEventId
        val sortedList = if (preferredId != null && baseSortedList.any { it.id == preferredId }) {
            val preferredEvent = baseSortedList.first { it.id == preferredId }
            listOf(preferredEvent) + baseSortedList.filter { it.id != preferredId }
        } else {
            baseSortedList
        }
        
        val isPanelVisible = _isNotificationPanelVisible.value
        val hideOnPanel = prefs?.getBoolean("hide_on_notification_panel", true) ?: true
        val forceHide = isPanelVisible && hideOnPanel

        if (sortedList.isEmpty()) {
            hideJob?.cancel()
            _isInputFocused.value = false
            val showAlwaysVal = prefs?.getBoolean("show_always", false) ?: false
            if (showAlwaysVal && !isManuallyHidden && !forceHide) {
                _uiState.value = CapsuleUiState(
                    mainEvent = null,
                    splitEvent = null,
                    displayMode = if (isCurrentlyExpanded) DisplayMode.EXPANDED else DisplayMode.COLLAPSED,
                    isHidden = false
                )
            } else {
                val delayMs = getDismissDelayMs()
                if (delayMs > 0 && !forceHide) {
                    hideJob = scope.launch {
                        delay(delayMs)
                        _uiState.value = CapsuleUiState(
                            mainEvent = null,
                            splitEvent = null,
                            displayMode = DisplayMode.HIDDEN,
                            isHidden = true
                        )
                    }
                } else {
                    _uiState.value = CapsuleUiState(
                        mainEvent = null,
                        splitEvent = null,
                        displayMode = DisplayMode.HIDDEN,
                        isHidden = true
                    )
                }
            }
            return
        }

        hideJob?.cancel()

        val allowTwoPopups = prefs?.getBoolean("allow_two_popups", true) ?: true
        val main = sortedList[0]
        val split = if (allowTwoPopups && sortedList.size > 1) sortedList[1] else null

        val currentMode = _uiState.value.displayMode
        val targetMode = when {
            currentMode == DisplayMode.EXPANDED -> DisplayMode.EXPANDED
            split != null -> DisplayMode.SPLIT
            else -> DisplayMode.COLLAPSED
        }

        val shouldShow = if (isManuallyHidden || forceHide) {
            if (forceHide) true else main.priority < 70
        } else {
            false
        }

        _uiState.value = CapsuleUiState(
            mainEvent = main,
            splitEvent = split,
            displayMode = targetMode,
            isHidden = shouldShow
        )
    }

    fun setIsNotificationPanelVisible(visible: Boolean) {
        if (_isNotificationPanelVisible.value != visible) {
            _isNotificationPanelVisible.value = visible
            recalculateState()
        }
    }

    var activeNfcRegistrationTask: String? = null

    fun formatNfcTagId(raw: String): String {
        val clean = raw.replace(":", "").trim().uppercase()
        return if (clean.length >= 2) clean.chunked(2).joinToString(":") else clean
    }

    fun postNfcReaction(
        tagId: String,
        title: String,
        subtitle: String,
        techType: String = "NFC Tag",
        actionName: String = "",
        actionStatus: String = "",
        isBound: Boolean = false
    ) {
        val event = CapsuleEvent.Nfc(
            tagId = tagId,
            title = title,
            subtitle = subtitle,
            techType = techType,
            actionName = actionName,
            actionStatus = actionStatus,
            isBound = isBound,
            priority = 930,
            durationMs = 4500L
        )
        postEvent(event)
        com.example.capsulebar.service.HapticSoundManager.playExpand()
        // Open/expand capsule so user immediately sees dynamic reaction
        setDisplayMode(DisplayMode.EXPANDED)
    }

    fun processNfcTag(
        context: android.content.Context,
        tagId: String,
        fromBackground: Boolean = false,
        onFinish: (() -> Unit)? = null
    ) {
        processNfcTag(context, tagId, "NFC Tag", fromBackground, onFinish)
    }

    fun processNfcTag(
        context: android.content.Context,
        tagId: String,
        techType: String,
        fromBackground: Boolean = false,
        onFinish: (() -> Unit)? = null
    ) {
        val settings = CapsuleSettings(context)
        val formattedId = formatNfcTagId(tagId)
        val task = activeNfcRegistrationTask

        if (task != null) {
            val taskLabel = when (task) {
                "sound_profile", "wrist_watch" -> {
                    settings.nfcSoundProfileTagId = formattedId
                    "Sound Profile Toggle"
                }
                "quick_launch", "chetak" -> {
                    settings.nfcQuickLaunchTagId = formattedId
                    "Quick Launch Action"
                }
                else -> {
                    settings.nfcSoundProfileTagId = formattedId
                    "NFC Action"
                }
            }
            activeNfcRegistrationTask = null
            vibrateBriefly(context)
            postNfcReaction(
                tagId = formattedId,
                title = "NFC Tag Bound",
                subtitle = "Assigned to $taskLabel",
                techType = techType,
                actionName = taskLabel,
                actionStatus = "Tag bound to $taskLabel",
                isBound = true
            )
            android.widget.Toast.makeText(context, "$taskLabel bound to tag: $formattedId", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            val boundSoundTag = formatNfcTagId(settings.nfcSoundProfileTagId)
            val boundLaunchTag = formatNfcTagId(settings.nfcQuickLaunchTagId)

            val rawClean = tagId.replace(":", "").uppercase()
            val isSoundMatch = boundSoundTag.isNotEmpty() && (formattedId == boundSoundTag || rawClean == boundSoundTag.replace(":", "").uppercase())
            val isLaunchMatch = boundLaunchTag.isNotEmpty() && (formattedId == boundLaunchTag || rawClean == boundLaunchTag.replace(":", "").uppercase())

            when {
                isSoundMatch -> {
                    val newMode = executeSoundProfileTask(context)
                    postNfcReaction(
                        tagId = formattedId,
                        title = "NFC • Sound Mode",
                        subtitle = "Switched to $newMode",
                        techType = techType,
                        actionName = "Sound: $newMode",
                        actionStatus = "Ringer mode set to $newMode",
                        isBound = true
                    )
                    if (fromBackground) {
                        onFinish?.invoke()
                    }
                }
                isLaunchMatch -> {
                    executeQuickLaunchTask(context)
                    postNfcReaction(
                        tagId = formattedId,
                        title = "NFC • Quick Launch",
                        subtitle = "Bluetooth enabled & player opening",
                        techType = techType,
                        actionName = "Quick Launch",
                        actionStatus = "Bluetooth ON • Player starting",
                        isBound = true
                    )
                    if (fromBackground) {
                        onFinish?.invoke()
                    }
                }
                else -> {
                    vibrateBriefly(context)
                    postNfcReaction(
                        tagId = formattedId,
                        title = "NFC Tag Detected",
                        subtitle = "UID: $formattedId",
                        techType = techType,
                        actionName = "NFC Check Verified",
                        actionStatus = "Tag scanned successfully • Unbound",
                        isBound = false
                    )
                    android.widget.Toast.makeText(context, "NFC Tag Scanned: $formattedId (Unbound)", android.widget.Toast.LENGTH_SHORT).show()
                    if (fromBackground) {
                        onFinish?.invoke()
                    }
                }
            }
        }
    }

    private fun executeSoundProfileTask(context: android.content.Context): String {
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        val currentMode = audioManager.ringerMode
        val (newMode, profileName) = if (currentMode == android.media.AudioManager.RINGER_MODE_VIBRATE) {
            Pair(android.media.AudioManager.RINGER_MODE_NORMAL, "Ring")
        } else {
            Pair(android.media.AudioManager.RINGER_MODE_VIBRATE, "Vibrate")
        }
        audioManager.ringerMode = newMode
        vibrateBriefly(context)
        return profileName
    }

    private fun executeQuickLaunchTask(context: android.content.Context) {
        try {
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
                bluetoothAdapter.enable()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        vibrateBriefly(context)

        scope.launch {
            delay(1000)
            val pm = context.packageManager
            val packagesToTry = listOf(
                "com.google.android.apps.youtube.music",
                "com.spotify.music",
                "com.piyush.oto",
                "com.amazon.mp3",
                "com.apple.android.music",
                "com.jio.media.jiobeats",
                "com.android.music",
                "com.google.android.music"
            )
            for (pkg in packagesToTry) {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return@launch
                }
            }
            try {
                val musicIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                    addCategory(android.content.Intent.CATEGORY_APP_MUSIC)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(musicIntent)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun executeWristWatchTask(context: android.content.Context) = executeSoundProfileTask(context)
    fun executeChetakTask(context: android.content.Context) = executeQuickLaunchTask(context)

    private fun vibrateBriefly(context: android.content.Context) {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(80, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(80)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
