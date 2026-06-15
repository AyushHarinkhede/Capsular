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

    private val _visualizerAmplitudes = MutableStateFlow(listOf(0.3f, 0.3f, 0.3f))
    val visualizerAmplitudes: StateFlow<List<Float>> = _visualizerAmplitudes.asStateFlow()

    private var prefs: android.content.SharedPreferences? = null
    private var isManuallyHidden = false
    private var collapseJob: Job? = null
    private var hideJob: Job? = null

    fun initialize(context: android.content.Context) {
        prefs = context.applicationContext.getSharedPreferences("capsule_settings", android.content.Context.MODE_PRIVATE)
        prefs?.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == "show_always" || key == "dismiss_delay_sec") {
                recalculateState()
            }
        }
    }

    private fun getCompactDurationMs(): Long {
        return (prefs?.getInt("compact_duration_sec", 5) ?: 5) * 1000L
    }

    private fun getExpandedDurationMs(): Long {
        return (prefs?.getInt("expanded_duration_sec", 8) ?: 8) * 1000L
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
        onMediaActionListener?.invoke(action)
    }

    private val activeEvents = mutableMapOf<String, CapsuleEvent>()
    private val autoDismissJobs = mutableMapOf<String, Job>()

    @Synchronized
    fun postEvent(event: CapsuleEvent) {
        autoDismissJobs[event.id]?.cancel()

        activeEvents[event.id] = event

        val duration = if (event.durationMs > 0) {
            getCompactDurationMs()
        } else {
            0L
        }

        if (duration > 0) {
            autoDismissJobs[event.id] = scope.launch {
                delay(duration)
                removeEvent(event.id)
            }
        }

        recalculateState()
    }

    @Synchronized
    fun removeEvent(id: String) {
        autoDismissJobs[id]?.cancel()
        autoDismissJobs.remove(id)
        activeEvents.remove(id)
        recalculateState()
    }

    @Synchronized
    fun clearAllEvents() {
        autoDismissJobs.values.forEach { it.cancel() }
        autoDismissJobs.clear()
        activeEvents.clear()
        recalculateState()
    }

    @Synchronized
    fun isEventActive(id: String): Boolean {
        return activeEvents.containsKey(id)
    }


    fun setDisplayMode(mode: DisplayMode) {
        collapseJob?.cancel()
        _uiState.value = _uiState.value.copy(displayMode = mode, isHidden = mode == DisplayMode.HIDDEN)
        
        if (mode == DisplayMode.EXPANDED) {
            val duration = getExpandedDurationMs()
            if (duration > 0) {
                collapseJob = scope.launch {
                    delay(duration)
                    val current = _uiState.value
                    if (current.displayMode == DisplayMode.EXPANDED) {
                        val newMode = if (current.splitEvent != null) DisplayMode.SPLIT else DisplayMode.COLLAPSED
                        _uiState.value = current.copy(displayMode = newMode)
                    }
                }
            }
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
        _uiState.value = _uiState.value.copy(isHidden = true, displayMode = DisplayMode.HIDDEN)
    }

    fun show() {
        isManuallyHidden = false
        _uiState.value = _uiState.value.copy(isHidden = false)
        recalculateState()
    }

    private fun recalculateState() {
        val sortedList = activeEvents.values.sortedByDescending { it.priority }
        
        if (sortedList.isEmpty()) {
            hideJob?.cancel()
            val showAlwaysVal = prefs?.getBoolean("show_always", false) ?: false
            if (showAlwaysVal && !isManuallyHidden) {
                _uiState.value = CapsuleUiState(
                    mainEvent = null,
                    splitEvent = null,
                    displayMode = DisplayMode.COLLAPSED,
                    isHidden = false
                )
            } else {
                val delayMs = getDismissDelayMs()
                if (delayMs > 0) {
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

        val main = sortedList[0]
        val split = if (sortedList.size > 1) sortedList[1] else null

        val currentMode = _uiState.value.displayMode
        val targetMode = when {
            currentMode == DisplayMode.EXPANDED -> DisplayMode.EXPANDED
            split != null -> DisplayMode.SPLIT
            else -> DisplayMode.COLLAPSED
        }

        val shouldShow = if (isManuallyHidden) {
            main.priority < 70 // Remain hidden unless priority is high
        } else {
            false // Show (not hidden)
        }

        _uiState.value = CapsuleUiState(
            mainEvent = main,
            splitEvent = split,
            displayMode = targetMode,
            isHidden = shouldShow
        )
    }
}
