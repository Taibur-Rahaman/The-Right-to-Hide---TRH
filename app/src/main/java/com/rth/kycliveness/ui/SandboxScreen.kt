package com.rth.kycliveness.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import com.rth.kycliveness.data.ProfileCatalog
import com.rth.kycliveness.data.TestAction
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

    DisposableEffect(Unit) {
        viewModel.attachPlayer()
        onDispose { }
    }

    if (state.showMoreProfiles) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleMoreProfiles(false) },
            title = { Text("More profiles") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Sandbox profiles only. These do not claim identical KYC requirements.",
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

    if (state.showMoreActions) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleMoreActions(false) },
            title = { Text("More actions") },
            text = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    advancedActions.forEach { action ->
                        OutlinedButton(onClick = {
                            viewModel.onActionSelected(action)
                            viewModel.toggleMoreActions(false)
                        }) {
                            Text(shortLabel(action))
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
                text = "KYC Liveness Sandbox",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Text("App Profile", style = MaterialTheme.typography.titleMedium)

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
            ) {
                Text("MORE")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { viewModel.startTest() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Start Test")
                }
                OutlinedButton(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("RESET")
                }
            }

            HorizontalDivider()

            VideoPreview(
                playerProvider = { viewModel.exoPlayer },
                unavailable = state.clipUnavailable || state.videoMessage != null,
                message = state.videoMessage ?: "VIDEO PREVIEW",
                isPlaying = state.isPlaying,
                onPlay = viewModel::play,
                onPause = viewModel::pause,
                onReplay = viewModel::replay,
                onStop = viewModel::stopVideo,
            )

            Text("Required Action", style = MaterialTheme.typography.titleMedium)
            Text(
                text = state.requiredAction?.displayLabel()?.uppercase(Locale.US) ?: "—",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                primaryActions.forEach { action ->
                    Button(onClick = { viewModel.onActionSelected(action) }) {
                        Text(shortLabel(action))
                    }
                }
            }

            OutlinedButton(
                onClick = { viewModel.toggleMoreActions(true) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("MORE ACTIONS")
            }

            Text(
                text = "Progress: ${state.snapshot.progressCompleted} / ${state.snapshot.progressTotal.coerceAtLeast(state.selectedProfile.actions.size)}",
                style = MaterialTheme.typography.bodyLarge,
            )

            ResultPanel(state = state)
        }
    }
}

@Composable
private fun VideoPreview(
    playerProvider: () -> androidx.media3.common.Player?,
    unavailable: Boolean,
    message: String,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onReplay: () -> Unit,
    onStop: () -> Unit,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (!unavailable) {
                AndroidView(
                    factory = {
                        PlayerView(context).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            useController = false
                        }
                    },
                    update = { view ->
                        view.player = playerProvider()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (unavailable || playerProvider() == null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onPlay, modifier = Modifier.weight(1f)) { Text("Play") }
            OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f)) { Text("Pause") }
            OutlinedButton(onClick = onReplay, modifier = Modifier.weight(1f)) { Text("Replay") }
            OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f)) { Text("Stop") }
        }
        Text(
            text = if (isPlaying) "Playing" else "Idle",
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun ResultPanel(state: SandboxUiState) {
    val snap = state.snapshot
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Test Result", style = MaterialTheme.typography.titleMedium)
        Text("Test Started: ${if (state.testStarted) "Yes" else "No"}")
        Text("Current Action: ${state.requiredAction?.displayLabel() ?: "—"}")
        Text("Completed Actions: ${snap.completedActions.size}")
        snap.completedActions.forEach { Text("✓ $it") }
        Text("Failed Actions: ${snap.failedActions.size}")
        snap.failedActions.forEach { Text("✕ $it") }
        Text("Retries: ${snap.retriesUsed} / ${snap.maxRetries}")
        Text("Elapsed Time: ${formatElapsed(snap.elapsedMillis)}")
        Spacer(modifier = Modifier.height(8.dp))
        val resultText = when (snap.result) {
            TestResult.PASSED -> "TEST PASSED"
            TestResult.FAILED -> "TEST FAILED"
            TestResult.NONE -> "—"
        }
        Text(
            text = "RESULT  $resultText",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = when (snap.result) {
                TestResult.PASSED -> MaterialTheme.colorScheme.primary
                TestResult.FAILED -> MaterialTheme.colorScheme.error
                TestResult.NONE -> MaterialTheme.colorScheme.onSurface
            },
        )
        snap.lastError?.let {
            Text("Last error: $it", style = MaterialTheme.typography.bodySmall)
        }
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
