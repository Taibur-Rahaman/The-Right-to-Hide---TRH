package com.rth.kycliveness.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.rth.kycliveness.data.ClipCatalog
import com.rth.kycliveness.data.KycTestProfile
import com.rth.kycliveness.data.ProfileCatalog
import com.rth.kycliveness.data.TestAction
import com.rth.kycliveness.logging.SandboxLogger
import com.rth.kycliveness.player.LivenessPlayer
import com.rth.kycliveness.player.PlayerEvent
import com.rth.kycliveness.state.KycTestStateMachine
import com.rth.kycliveness.state.TestPhase
import com.rth.kycliveness.state.TestResult
import com.rth.kycliveness.state.TestSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SandboxUiState(
    val selectedProfile: KycTestProfile = ProfileCatalog.defaultProfile(),
    val showMoreProfiles: Boolean = false,
    val showMoreActions: Boolean = false,
    val snapshot: TestSnapshot = TestSnapshot(),
    val requiredAction: TestAction? = null,
    val clipUnavailable: Boolean = false,
    val videoMessage: String? = null,
    val isPlaying: Boolean = false,
    val testStarted: Boolean = false,
)

class SandboxViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val stateMachine = KycTestStateMachine()
    private var timeoutJob: Job? = null
    private var tickerJob: Job? = null

    private val _uiState = MutableStateFlow(SandboxUiState())
    val uiState: StateFlow<SandboxUiState> = _uiState.asStateFlow()

    private var player: LivenessPlayer? = null

    val exoPlayer: Player?
        get() = player?.player

    fun attachPlayer() {
        if (player != null) return
        player = LivenessPlayer(getApplication()) { event ->
            when (event) {
                is PlayerEvent.Completed -> onVideoCompleted(event.action)
                is PlayerEvent.Error -> onVideoError(event)
                is PlayerEvent.StateChanged -> _uiState.update { it.copy(isPlaying = event.isPlaying) }
            }
        }
    }

    fun selectProfile(profile: KycTestProfile) {
        if (_uiState.value.snapshot.phase != TestPhase.IDLE &&
            _uiState.value.snapshot.result == TestResult.NONE &&
            _uiState.value.testStarted
        ) {
            // Allow profile change only when idle or finished; reset first for clarity.
            reset()
        }
        _uiState.update {
            it.copy(
                selectedProfile = profile,
                showMoreProfiles = false,
            )
        }
    }

    fun toggleMoreProfiles(show: Boolean) {
        _uiState.update { it.copy(showMoreProfiles = show) }
    }

    fun toggleMoreActions(show: Boolean) {
        _uiState.update { it.copy(showMoreActions = show) }
    }

    fun startTest() {
        attachPlayer()
        player?.stop()
        val profile = _uiState.value.selectedProfile
        val snap = stateMachine.start(profile)
        SandboxLogger.testStarted(profile.id)
        publish(snap, clipUnavailable = false, videoMessage = null)
        _uiState.update { it.copy(testStarted = true) }
        startTicker()
    }

    fun onActionSelected(action: TestAction) {
        val before = stateMachine.snapshot()
        if (before.phase != TestPhase.WAITING_FOR_ACTION &&
            before.phase != TestPhase.RETRY
        ) {
            return
        }
        val snap = stateMachine.beginAction(action)
        if (snap.phase == TestPhase.ACTION_PLAYING) {
            SandboxLogger.actionStarted(action.displayLabel())
            publish(snap)
            playRequiredClip()
            startActionTimeout()
        } else {
            if (snap.phase == TestPhase.WAITING_FOR_ACTION || snap.phase == TestPhase.RETRY) {
                SandboxLogger.retry(
                    stateMachine.requiredAction()?.displayLabel().orEmpty(),
                    snap.retriesUsed,
                )
            }
            if (snap.result == TestResult.FAILED) {
                SandboxLogger.actionFailed(action.displayLabel(), snap.lastError.orEmpty())
                SandboxLogger.testFailed()
                stopTimeout()
            } else {
                SandboxLogger.actionFailed(
                    stateMachine.requiredAction()?.displayLabel().orEmpty(),
                    snap.lastError.orEmpty(),
                )
            }
            publish(snap)
        }
    }

    fun play() = player?.play()
    fun pause() = player?.pause()
    fun replay() = player?.replay()
    fun stopVideo() = player?.stop()

    fun reset() {
        stopTimeout()
        stopTicker()
        player?.stop()
        val snap = stateMachine.reset()
        SandboxLogger.reset()
        _uiState.update {
            SandboxUiState(
                selectedProfile = it.selectedProfile,
                snapshot = snap,
                requiredAction = null,
                clipUnavailable = false,
                videoMessage = null,
                isPlaying = false,
                testStarted = false,
            )
        }
    }

    private fun playRequiredClip() {
        val action = stateMachine.requiredAction() ?: return
        val resolution = ClipCatalog.resolve(getApplication(), action)
        if (!resolution.isAvailable) {
            SandboxLogger.videoError("Missing clip ${resolution.clipName}.mp4")
            _uiState.update {
                it.copy(
                    clipUnavailable = true,
                    videoMessage = "Test clip unavailable",
                )
            }
            // Placeholder handling: no binary clips ship with the sandbox.
            // Surface the message, do not crash, and complete the simulated action.
            viewModelScope.launch {
                delay(350)
                if (stateMachine.snapshot().phase == TestPhase.ACTION_PLAYING) {
                    stopTimeout()
                    SandboxLogger.actionCompleted(action.displayLabel())
                    val snap = stateMachine.onActionCompleted()
                    if (snap.result == TestResult.PASSED) SandboxLogger.testPassed()
                    publish(snap, clipUnavailable = true, videoMessage = "Test clip unavailable")
                }
            }
            return
        }
        _uiState.update {
            it.copy(clipUnavailable = false, videoMessage = null)
        }
        player?.playClip(resolution)
    }

    private fun onVideoCompleted(action: TestAction) {
        val required = stateMachine.requiredAction()
        if (required != action) return
        if (stateMachine.snapshot().phase != TestPhase.ACTION_PLAYING) return
        stopTimeout()
        SandboxLogger.actionCompleted(action.displayLabel())
        val snap = stateMachine.onActionCompleted()
        when (snap.result) {
            TestResult.PASSED -> SandboxLogger.testPassed()
            TestResult.FAILED -> SandboxLogger.testFailed()
            TestResult.NONE -> Unit
        }
        publish(snap)
    }

    private fun onVideoError(event: PlayerEvent.Error) {
        SandboxLogger.videoError(event.message)
        _uiState.update {
            it.copy(
                clipUnavailable = event.missing,
                videoMessage = if (event.missing) "Test clip unavailable" else event.message,
            )
        }
        handlePlaybackFailure(event.message)
    }

    private fun handlePlaybackFailure(reason: String) {
        if (stateMachine.snapshot().phase != TestPhase.ACTION_PLAYING) return
        val actionLabel = stateMachine.requiredAction()?.displayLabel().orEmpty()
        SandboxLogger.actionFailed(actionLabel, reason)
        val snap = stateMachine.onActionFailed(reason)
        if (snap.phase == TestPhase.ACTION_PLAYING) {
            SandboxLogger.retry(actionLabel, snap.retriesUsed)
            publish(snap)
            playRequiredClip()
            startActionTimeout()
        } else if (snap.result == TestResult.FAILED) {
            SandboxLogger.testFailed()
            stopTimeout()
            publish(snap)
        } else {
            publish(snap)
        }
    }

    private fun startActionTimeout() {
        stopTimeout()
        val timeoutMs = _uiState.value.selectedProfile.timeoutSeconds * 1000L
        timeoutJob = viewModelScope.launch {
            delay(timeoutMs)
            if (stateMachine.snapshot().phase == TestPhase.ACTION_PLAYING) {
                handlePlaybackFailure("Action timed out")
            }
        }
    }

    private fun stopTimeout() {
        timeoutJob?.cancel()
        timeoutJob = null
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(250)
                val snap = stateMachine.tickElapsed()
                if (snap.phase == TestPhase.IDLE ||
                    snap.phase == TestPhase.PASSED ||
                    snap.phase == TestPhase.FAILED
                ) {
                    publish(snap)
                    break
                }
                publish(snap)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun publish(
        snap: TestSnapshot,
        clipUnavailable: Boolean = _uiState.value.clipUnavailable,
        videoMessage: String? = _uiState.value.videoMessage,
    ) {
        _uiState.update {
            it.copy(
                snapshot = snap,
                requiredAction = stateMachine.requiredAction(),
                clipUnavailable = clipUnavailable,
                videoMessage = videoMessage,
                testStarted = snap.phase != TestPhase.IDLE,
            )
        }
    }

    override fun onCleared() {
        stopTimeout()
        stopTicker()
        player?.release()
        player = null
        super.onCleared()
    }
}
