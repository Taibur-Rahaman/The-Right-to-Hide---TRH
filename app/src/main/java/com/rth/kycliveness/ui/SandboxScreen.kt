package com.rth.kycliveness.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import com.rth.kycliveness.data.ProfileCatalog
import com.rth.kycliveness.data.TestAction
import com.rth.kycliveness.source.InputSourceType
import com.rth.kycliveness.state.TestResult
import java.util.Locale
import java.util.concurrent.TimeUnit

private val primaryActions = listOf(
    TestAction.BLINK_ONCE,
    TestAction.HEAD_LEFT,
    TestAction.HEAD_RIGHT,
    TestAction.NOD,
    TestAction.OPEN_MOUTH,
    TestAction.SMILE,
)

private val advancedActions = TestAction.entries.filterNot { it in primaryActions }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SandboxScreen(
    viewModel: SandboxViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onCameraPermissionResult(granted)
    }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val name = uri.lastPathSegment ?: "selected-video"
        val mime = context.contentResolver.getType(uri)
        val size = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
        viewModel.setTestMedia(uri, name, mime, size)
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val name = uri.lastPathSegment ?: "selected-image"
        val mime = context.contentResolver.getType(uri)
        val size = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
        viewModel.setTestMedia(uri, name, mime, size)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onCameraPermissionResult(granted)
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (state.showMoreProfiles && state.developerMode) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleMoreProfiles(false) },
            title = { Text("More profiles") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Sandbox profiles only. Not real KYC vendor requirements.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ProfileCatalog.moreProfiles.forEach { profile ->
                            FilterChip(
                                selected = state.selectedProfile.id == profile.id,
                                onClick = { viewModel.selectProfile(profile) },
                                label = { Text(profile.displayName) },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.toggleMoreProfiles(false) }) {
                    Text("Close")
                }
            },
        )
    }

    if (state.showMoreActions && state.developerMode) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleMoreActions(false) },
            title = { Text("More actions (manual inject)") },
            text = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    advancedActions.forEach { action ->
                        OutlinedButton(onClick = {
                            viewModel.onManualAction(action)
                            viewModel.toggleMoreActions(false)
                        }) {
                            Text(action.displayLabel())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.toggleMoreActions(false) }) {
                    Text("Close")
                }
            },
        )
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "KYC Verification",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            CameraPreviewPane(
                state = state,
                viewModel = viewModel,
                lifecycleOwner = lifecycleOwner,
            )

            Text(
                text = state.requiredAction?.displayLabel() ?: "Position your face in the guide",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            val step = state.snapshot.progressCompleted +
                if (state.testStarted && state.snapshot.result == TestResult.NONE) 1 else 0
            Text(
                text = if (state.snapshot.progressTotal > 0) {
                    "Step ${step.coerceAtMost(state.snapshot.progressTotal)} of ${state.snapshot.progressTotal}"
                } else {
                    "Ready"
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "Verification: ${state.verificationLabel}",
                style = MaterialTheme.typography.bodyLarge,
                color = when (state.snapshot.result) {
                    TestResult.PASSED -> MaterialTheme.colorScheme.primary
                    TestResult.FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { viewModel.startTest() },
                    modifier = Modifier.weight(1f),
                ) { Text("Start verification") }
                OutlinedButton(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.weight(1f),
                ) { Text("Reset") }
            }

            ResultSummary(state)

            HorizontalDivider()

            DeveloperModePanel(
                state = state,
                viewModel = viewModel,
                onPickVideo = { videoPicker.launch(arrayOf("video/*")) },
                onPickImage = { imagePicker.launch(arrayOf("image/*")) },
                onRequestCameraPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
            )
        }
    }
}

@Composable
private fun CameraPreviewPane(
    state: SandboxUiState,
    viewModel: SandboxViewModel,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .background(Color(0xFF101820), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when (state.inputSource) {
            InputSourceType.LIVE_CAMERA -> {
                val ctx = LocalContext.current
                val previewView = remember { PreviewView(ctx) }
                DisposableEffect(lifecycleOwner, state.cameraPermissionGranted) {
                    if (state.cameraPermissionGranted) {
                        viewModel.bindCameraPreview(lifecycleOwner, previewView)
                    }
                    onDispose { }
                }
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            InputSourceType.TEST_VIDEO -> {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            useController = false
                        }
                    },
                    update = { it.player = viewModel.exoPlayer },
                    modifier = Modifier.fillMaxSize(),
                )
                if (state.testMediaUri == null) {
                    Text(
                        "Select a test video in Developer Mode",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            InputSourceType.TEST_IMAGE -> {
                Text(
                    state.sourceError ?: "TEST SOURCE: Image",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        // Face guide overlay
        Box(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .aspectRatio(0.75f)
                .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape),
        )

        if (state.developerMode && state.inputSource != InputSourceType.LIVE_CAMERA) {
            Text(
                text = "TEST SOURCE",
                color = Color.Yellow,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (state.faceDetected) "Face detected" else "No face",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
            state.sourceError?.let {
                Text(it, color = Color(0xFFFFAB91), textAlign = TextAlign.Center)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeveloperModePanel(
    state: SandboxUiState,
    viewModel: SandboxViewModel,
    onPickVideo: () -> Unit,
    onPickImage: () -> Unit,
    onRequestCameraPermission: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Developer Mode", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = state.developerMode,
                onCheckedChange = { viewModel.setDeveloperMode(it) },
            )
        }

        if (!state.developerMode) {
            Text(
                "Enable Developer Mode to choose Live Camera / Test Video / Test Image " +
                    "and access sandbox tooling. Normal mode uses live camera only.",
                style = MaterialTheme.typography.bodySmall,
            )
            return
        }

        Text(
            "Sandbox-only. Does not inject video into other apps or bypass production KYC.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )

        Text("Input Source", fontWeight = FontWeight.SemiBold)
        SourceRadio(
            selected = state.inputSource == InputSourceType.LIVE_CAMERA,
            label = "Live Camera",
            onClick = { viewModel.selectInputSource(InputSourceType.LIVE_CAMERA) },
        )
        SourceRadio(
            selected = state.inputSource == InputSourceType.TEST_VIDEO,
            label = "Test Video",
            onClick = { viewModel.selectInputSource(InputSourceType.TEST_VIDEO) },
        )
        SourceRadio(
            selected = state.inputSource == InputSourceType.TEST_IMAGE,
            label = "Test Image",
            onClick = { viewModel.selectInputSource(InputSourceType.TEST_IMAGE) },
        )

        when (state.inputSource) {
            InputSourceType.LIVE_CAMERA -> {
                Text("Camera: ${state.activeCameraLabel ?: "—"}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onRequestCameraPermission) {
                        Text("Request permission")
                    }
                    OutlinedButton(onClick = { viewModel.switchCamera() }) {
                        Text("Switch Camera")
                    }
                }
            }
            InputSourceType.TEST_VIDEO -> {
                Text("Selected video: ${state.testMediaName ?: "(none)"}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onPickVideo) { Text(if (state.testMediaUri == null) "Choose" else "Replace") }
                    OutlinedButton(onClick = { viewModel.clearTestMedia() }) { Text("Remove") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.playTestMedia() }) { Text("Play") }
                    OutlinedButton(onClick = { viewModel.pauseTestMedia() }) { Text("Pause") }
                    OutlinedButton(onClick = { viewModel.restartTestMedia() }) { Text("Restart") }
                }
                Text(
                    "Progress: ${formatMs(state.playbackPositionMs)} / ${formatMs(state.playbackDurationMs)}",
                )
            }
            InputSourceType.TEST_IMAGE -> {
                Text("Selected image: ${state.testMediaName ?: "(none)"}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onPickImage) { Text(if (state.testMediaUri == null) "Choose" else "Replace") }
                    OutlinedButton(onClick = { viewModel.clearTestMedia() }) { Text("Remove") }
                }
                Text(
                    "Static image cannot be used for this liveness test.",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        HorizontalDivider()
        Text("Sandbox profiles", fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProfileCatalog.primaryProfiles.forEach { profile ->
                FilterChip(
                    selected = state.selectedProfile.id == profile.id,
                    onClick = { viewModel.selectProfile(profile) },
                    label = { Text(profile.displayName) },
                )
            }
        }
        OutlinedButton(
            onClick = { viewModel.toggleMoreProfiles(true) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("MORE PROFILES") }

        Text("Manual action inject (does not bypass face pipeline)", fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            primaryActions.forEach { action ->
                Button(onClick = { viewModel.onManualAction(action) }) {
                    Text(shortLabel(action))
                }
            }
        }
        OutlinedButton(
            onClick = { viewModel.toggleMoreActions(true) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("MORE ACTIONS") }

        state.sourceStatus?.let {
            Text("Status: $it", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SourceRadio(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun ResultSummary(state: SandboxUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Test Result", fontWeight = FontWeight.SemiBold)
        Text("Input: ${state.inputSource.name}")
        Text("Face: ${if (state.faceDetected) "Yes" else "No"}")
        Text("Completed: ${state.snapshot.completedActions.size}")
        state.snapshot.completedActions.forEach { Text("✓ $it") }
        state.snapshot.failedActions.forEach { Text("✕ $it") }
        Text("Retries: ${state.snapshot.retriesUsed} / ${state.snapshot.maxRetries}")
        Text("Elapsed: ${formatElapsed(state.snapshot.elapsedMillis)}")
        val resultText = when (state.snapshot.result) {
            TestResult.PASSED -> "TEST PASSED"
            TestResult.FAILED -> "TEST FAILED"
            TestResult.NONE -> "—"
        }
        Text("RESULT  $resultText", fontWeight = FontWeight.Bold)
    }
}

private fun shortLabel(action: TestAction): String = when (action) {
    TestAction.BLINK_ONCE -> "Blink"
    TestAction.HEAD_LEFT -> "Left"
    TestAction.HEAD_RIGHT -> "Right"
    TestAction.NOD -> "Nod"
    TestAction.OPEN_MOUTH -> "Mouth"
    TestAction.SMILE -> "Smile"
    else -> action.displayLabel()
}

private fun formatElapsed(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun formatMs(millis: Long): String {
    if (millis <= 0L) return "00:00"
    val totalSec = millis / 1000
    return String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60)
}
