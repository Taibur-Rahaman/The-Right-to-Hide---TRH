package com.rth.kycliveness.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.rth.kycliveness.data.ClipResolution
import com.rth.kycliveness.data.TestAction
import com.rth.kycliveness.logging.SandboxLogger

sealed class PlayerEvent {
    data class Completed(val action: TestAction) : PlayerEvent()
    data class Error(val action: TestAction?, val message: String, val missing: Boolean) : PlayerEvent()
    data class StateChanged(val isPlaying: Boolean) : PlayerEvent()
}

/**
 * Isolated Media3 ExoPlayer wrapper. UI must not talk to ExoPlayer directly.
 */
class LivenessPlayer(
    context: Context,
    private val onEvent: (PlayerEvent) -> Unit,
) {
    private val appContext = context.applicationContext
    private var exoPlayer: ExoPlayer? = ExoPlayer.Builder(appContext).build()
    private var currentAction: TestAction? = null
    private var currentClipName: String? = null
    private var lastRawResId: Int = 0

    val player: Player?
        get() = exoPlayer

    val isClipAvailable: Boolean
        get() = lastRawResId != 0

    val unavailableMessage: String?
        get() = if (lastRawResId == 0 && currentAction != null) {
            "Test clip unavailable"
        } else {
            null
        }

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                val action = currentAction
                if (action != null) {
                    onEvent(PlayerEvent.Completed(action))
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            onEvent(PlayerEvent.StateChanged(isPlaying))
        }

        override fun onPlayerError(error: PlaybackException) {
            val action = currentAction
            val message = error.message ?: "Corrupted or unreadable video"
            SandboxLogger.videoError(message)
            onEvent(PlayerEvent.Error(action, message, missing = false))
        }
    }

    init {
        exoPlayer?.addListener(listener)
    }

    fun playClip(resolution: ClipResolution) {
        currentAction = resolution.action
        currentClipName = resolution.clipName
        lastRawResId = resolution.rawResId
        if (!resolution.isAvailable) {
            SandboxLogger.videoError("Missing clip ${resolution.clipName}.mp4")
            onEvent(
                PlayerEvent.Error(
                    action = resolution.action,
                    message = "Test clip unavailable",
                    missing = true,
                ),
            )
            return
        }
        val player = exoPlayer ?: return
        val uri = Uri.parse("android.resource://${appContext.packageName}/${resolution.rawResId}")
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = true
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun replay() {
        val player = exoPlayer ?: return
        if (lastRawResId == 0) {
            onEvent(
                PlayerEvent.Error(
                    action = currentAction,
                    message = "Test clip unavailable",
                    missing = true,
                ),
            )
            return
        }
        player.seekTo(0)
        player.playWhenReady = true
    }

    fun stop() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        currentAction = null
        currentClipName = null
        lastRawResId = 0
    }

    fun release() {
        exoPlayer?.removeListener(listener)
        exoPlayer?.release()
        exoPlayer = null
    }
}
