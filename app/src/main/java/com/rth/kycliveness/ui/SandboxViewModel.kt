package com.rth.kycliveness.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.camera.view.PreviewView
import androidx.media3.common.Player
import com.rth.kycliveness.data.KycTestProfile
import com.rth.kycliveness.data.ProfileCatalog
import com.rth.kycliveness.data.TestAction
import com.rth.kycliveness.logging.SandboxLogger
import com.rth.kycliveness.pipeline.ActionMatcher
import com.rth.kycliveness.pipeline.FaceSignals
import com.rth.kycliveness.pipeline.LivenessFrameAnalyzer
import com.rth.kycliveness.source.CameraDeviceInfo
import com.rth.kycliveness.source.InputSourceController
import com.rth.kycliveness.source.InputSourceType
import com.rth.kycliveness.source.SourceEvent
import com.rth.kycliveness.source.TestMediaInfo
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
    val developerMode: Boolean = false,
    val inputSource: InputSourceType = InputSourceType.LIVE_CAMERA,
    val selectedProfile: KycTestProfile = ProfileCatalog.defaultProfile(),
    val showMoreProfiles: Boolean = false,
    val showMoreActions: Boolean = false,
    val snapshot: TestSnapshot = TestSnapshot(),
    val requiredAction: TestAction? = null,
    val testStarted: Boolean = false,
    val faceDetected: Boolean = false,
    val sourceStatus: String? = null,
    val sourceError: String? = null,
    val cameraPermissionGranted: Boolean = false,
    val availableCameras: List<CameraDeviceInfo> = emptyList(),
    val activeCameraLabel: String? = null,
    val testMediaName: String? = null,
    val testMediaUri: Uri? = null,
    val isPlaying: Boolean = false,
    val playbackPositionMs: Long = 0L,
    val playbackDurationMs: Long = 0L,
    val verificationLabel: String = "Idle",
)

class SandboxViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val stateMachine = KycTestStateMachine()
    private val actionMatcher = ActionMatcher()
    private var timeoutJob: Job? = null
    private var tickerJob: Job? = null

    private val _uiState = MutableStateFlow(SandboxUiState())
    val uiState: StateFlow<SandboxUiState> = _uiState.asStateFlow()

    private val frameAnalyzer = LivenessFrameAnalyzer(
        onSignals = { signals -> onFaceSignals(signals) },
        onError = { msg ->
            _uiState.update { it.copy(sourceError = msg) }
        },
    )

    private val sources = InputSourceController(application) { event ->
        when (event) {
            is SourceEvent.Frame -> frameAnalyzer.analyze(event.bitmap, event.timestampMs)
            is SourceEvent.Error -> _uiState.update {
                it.copy(sourceError = event.message, sourceStatus = event.message)
            }
            is SourceEvent.Status -> _uiState.update {
                it.copy(sourceStatus = event.message, sourceError = null)
            }
            is SourceEvent.Playback -> _uiState.update {
                it.copy(
                    isPlaying = event.isPlaying,
                    playbackPositionMs = event.positionMs,
                    playbackDurationMs = event.durationMs,
                )
            }
        }
    }

    val exoPlayer: Player?
        get() = sources.exoPlayer()

    init {
        sources.select(InputSourceType.LIVE_CAMERA)
    }

    fun setDeveloperMode(enabled: Boolean) {
        _uiState.update { it.copy(developerMode = enabled) }
        if (!enabled && _uiState.value.inputSource != InputSourceType.LIVE_CAMERA) {
            selectInputSource(InputSourceType.LIVE_CAMERA)
        }
    }

    fun selectInputSource(type: InputSourceType) {
        if (!_uiState.value.developerMode && type != InputSourceType.LIVE_CAMERA) {
            return
        }
        sources.stopActive()
        sources.select(type)
        actionMatcher.reset()
        _uiState.update {
            it.copy(
                inputSource = type,
                sourceError = null,
                sourceStatus = when (type) {
                    InputSourceType.LIVE_CAMERA -> "Live Camera"
                    InputSourceType.TEST_VIDEO -> "TEST SOURCE: Video"
                    InputSourceType.TEST_IMAGE -> "TEST SOURCE: Image"
                },
                testMediaName = sources.testMedia()?.currentMedia()?.displayName,
                testMediaUri = sources.testMedia()?.currentMedia()?.uri,
                isPlaying = false,
            )
        }
        if (type == InputSourceType.LIVE_CAMERA && _uiState.value.cameraPermissionGranted) {
            sources.startActive()
            refreshCameras()
        } else if (type != InputSourceType.LIVE_CAMERA) {
            sources.startActive()
        }
    }

    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(cameraPermissionGranted = granted) }
        if (granted && _uiState.value.inputSource == InputSourceType.LIVE_CAMERA) {
            sources.startActive()
            refreshCameras()
        } else if (!granted) {
            _uiState.update {
                it.copy(sourceError = "Camera permission denied")
            }
        }
    }

    fun bindCameraPreview(owner: LifecycleOwner, previewView: PreviewView) {
        sources.bindLivePreview(owner, previewView)
        if (_uiState.value.cameraPermissionGranted &&
            _uiState.value.inputSource == InputSourceType.LIVE_CAMERA
        ) {
            sources.startActive()
            refreshCameras()
        }
    }

    fun switchCamera() {
        if (_uiState.value.inputSource != InputSourceType.LIVE_CAMERA) return
        val active = sources.liveCamera()?.switchCamera()
        _uiState.update { it.copy(activeCameraLabel = active?.label) }
        refreshCameras()
    }

    fun setTestMedia(uri: Uri, displayName: String, mimeType: String?, sizeBytes: Long) {
        if (!_uiState.value.developerMode) return
        val type = _uiState.value.inputSource
        if (type != InputSourceType.TEST_VIDEO && type != InputSourceType.TEST_IMAGE) return
        val info = TestMediaInfo(
            uri = uri,
            displayName = displayName,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
        )
        sources.select(type)
        sources.testMedia()?.setMedia(info)
        sources.startActive()
        _uiState.update {
            it.copy(
                testMediaName = displayName,
                testMediaUri = uri,
                sourceError = null,
            )
        }
    }

    fun clearTestMedia() {
        sources.testMedia()?.clearMedia()
        _uiState.update {
            it.copy(
                testMediaName = null,
                testMediaUri = null,
                isPlaying = false,
                playbackPositionMs = 0L,
                playbackDurationMs = 0L,
            )
        }
    }

    fun playTestMedia() = sources.testMedia()?.play()
    fun pauseTestMedia() = sources.testMedia()?.pause()
    fun restartTestMedia() = sources.testMedia()?.restart()

    fun selectProfile(profile: KycTestProfile) {
        if (_uiState.value.testStarted &&
            _uiState.value.snapshot.result == TestResult.NONE &&
            _uiState.value.snapshot.phase != TestPhase.IDLE
        ) {
            reset()
        }
        _uiState.update { it.copy(selectedProfile = profile, showMoreProfiles = false) }
    }

    fun toggleMoreProfiles(show: Boolean) {
        _uiState.update { it.copy(showMoreProfiles = show) }
    }

    fun toggleMoreActions(show: Boolean) {
        _uiState.update { it.copy(showMoreActions = show) }
    }

    fun startTest() {
        if (_uiState.value.inputSource == InputSourceType.TEST_IMAGE) {
            _uiState.update {
                it.copy(sourceError = "Static image cannot be used for this liveness test.")
            }
            return
        }
        if (_uiState.value.inputSource == InputSourceType.TEST_VIDEO &&
            _uiState.value.testMediaUri == null
        ) {
            _uiState.update { it.copy(sourceError = "No test video selected") }
            return
        }
        if (_uiState.value.inputSource == InputSourceType.LIVE_CAMERA &&
            !_uiState.value.cameraPermissionGranted
        ) {
            _uiState.update { it.copy(sourceError = "Camera permission required") }
            return
        }

        actionMatcher.reset()
        sources.startActive()
        if (_uiState.value.inputSource == InputSourceType.TEST_VIDEO) {
            sources.testMedia()?.restart()
        }

        val profile = _uiState.value.selectedProfile
        val snap = stateMachine.start(profile)
        SandboxLogger.testStarted(profile.id)
        // Enter ACTION_PLAYING for the first required action so pipeline can complete it.
        val required = stateMachine.requiredAction()
        val playing = if (required != null) {
            stateMachine.beginAction(required)
        } else {
            snap
        }
        publish(playing)
        _uiState.update {
            it.copy(
                testStarted = true,
                verificationLabel = "Processing",
                sourceError = null,
            )
        }
        startTicker()
        startActionTimeout()
    }

    /**
     * Developer-only manual action inject for exercising the state machine.
     * Does not auto-pass merely by selecting a file.
     */
    fun onManualAction(action: TestAction) {
        if (!_uiState.value.developerMode) return
        val phase = stateMachine.snapshot().phase
        if (phase != TestPhase.WAITING_FOR_ACTION &&
            phase != TestPhase.RETRY &&
            phase != TestPhase.ACTION_PLAYING
        ) {
            return
        }
        val required = stateMachine.requiredAction() ?: return
        if (phase == TestPhase.WAITING_FOR_ACTION || phase == TestPhase.RETRY) {
            val snap = stateMachine.beginAction(action)
            if (snap.phase != TestPhase.ACTION_PLAYING) {
                publish(snap)
                if (snap.result == TestResult.FAILED) {
                    _uiState.update { it.copy(verificationLabel = "Failure") }
                }
                return
            }
        } else if (action != required) {
            val snap = stateMachine.onActionFailed("Wrong action selected")
            publish(snap)
            return
        }
        completeCurrentAction()
    }

    fun reset() {
        stopTimeout()
        stopTicker()
        actionMatcher.reset()
        sources.testMedia()?.pause()
        val snap = stateMachine.reset()
        SandboxLogger.reset()
        _uiState.update {
            it.copy(
                snapshot = snap,
                requiredAction = null,
                testStarted = false,
                faceDetected = false,
                verificationLabel = "Idle",
                sourceError = null,
            )
        }
    }

    private fun onFaceSignals(signals: FaceSignals) {
        _uiState.update { it.copy(faceDetected = signals.faceDetected) }
        val snap = stateMachine.snapshot()
        if (snap.phase != TestPhase.ACTION_PLAYING &&
            snap.phase != TestPhase.WAITING_FOR_ACTION &&
            snap.phase != TestPhase.RETRY
        ) {
            return
        }
        // Image source must never succeed via face hold alone for motion actions
        if (_uiState.value.inputSource == InputSourceType.TEST_IMAGE) {
            return
        }
        val required = stateMachine.requiredAction() ?: return
        // Ensure we are in ACTION_PLAYING for the required action
        if (snap.phase == TestPhase.WAITING_FOR_ACTION || snap.phase == TestPhase.RETRY) {
            stateMachine.beginAction(required)
            publish(stateMachine.snapshot())
        }
        val matched = actionMatcher.evaluate(required, signals) ?: return
        if (matched == required) {
            completeCurrentAction()
        }
    }

    private fun completeCurrentAction() {
        val action = stateMachine.requiredAction() ?: return
        if (stateMachine.snapshot().phase != TestPhase.ACTION_PLAYING) return
        stopTimeout()
        SandboxLogger.actionCompleted(action.displayLabel())
        val snap = stateMachine.onActionCompleted()
        when (snap.result) {
            TestResult.PASSED -> {
                SandboxLogger.testPassed()
                _uiState.update { it.copy(verificationLabel = "Success") }
                sources.testMedia()?.pause()
            }
            TestResult.FAILED -> {
                SandboxLogger.testFailed()
                _uiState.update { it.copy(verificationLabel = "Failure") }
            }
            TestResult.NONE -> {
                _uiState.update { it.copy(verificationLabel = "Processing") }
                // Advance to next required action
                val next = stateMachine.requiredAction()
                if (next != null && snap.phase == TestPhase.WAITING_FOR_ACTION) {
                    publish(stateMachine.beginAction(next))
                    startActionTimeout()
                } else {
                    publish(snap)
                }
                return
            }
        }
        publish(snap)
    }

    private fun refreshCameras() {
        val cams = sources.liveCamera()?.listCameras().orEmpty()
        val active = sources.liveCamera()?.activeCamera()?.label
        _uiState.update {
            it.copy(availableCameras = cams, activeCameraLabel = active)
        }
    }

    private fun startActionTimeout() {
        stopTimeout()
        val timeoutMs = _uiState.value.selectedProfile.timeoutSeconds * 1000L
        timeoutJob = viewModelScope.launch {
            delay(timeoutMs)
            if (stateMachine.snapshot().phase == TestPhase.ACTION_PLAYING) {
                val label = stateMachine.requiredAction()?.displayLabel().orEmpty()
                SandboxLogger.actionFailed(label, "Action timed out")
                val snap = stateMachine.onActionFailed("Action timed out")
                if (snap.phase == TestPhase.ACTION_PLAYING ||
                    snap.phase == TestPhase.WAITING_FOR_ACTION
                ) {
                    SandboxLogger.retry(label, snap.retriesUsed)
                    val required = stateMachine.requiredAction()
                    if (required != null && snap.phase == TestPhase.WAITING_FOR_ACTION) {
                        publish(stateMachine.beginAction(required))
                    } else {
                        publish(snap)
                    }
                    startActionTimeout()
                } else if (snap.result == TestResult.FAILED) {
                    SandboxLogger.testFailed()
                    _uiState.update { it.copy(verificationLabel = "Failure") }
                    publish(snap)
                } else {
                    publish(snap)
                }
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
                publish(snap)
                if (snap.phase == TestPhase.IDLE ||
                    snap.phase == TestPhase.PASSED ||
                    snap.phase == TestPhase.FAILED
                ) {
                    break
                }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun publish(snap: TestSnapshot) {
        _uiState.update {
            it.copy(
                snapshot = snap,
                requiredAction = stateMachine.requiredAction(),
                testStarted = snap.phase != TestPhase.IDLE,
            )
        }
    }

    override fun onCleared() {
        stopTimeout()
        stopTicker()
        frameAnalyzer.close()
        sources.releaseAll()
        super.onCleared()
    }
}
