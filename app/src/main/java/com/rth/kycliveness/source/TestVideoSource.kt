package com.rth.kycliveness.source

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.rth.kycliveness.logging.SandboxLogger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Developer test-video source. Plays a local URI via ExoPlayer and samples frames
 * into the same sandbox liveness pipeline used by the live camera.
 *
 * Scope: this app only — not a system virtual camera.
 */
class TestVideoSourceImpl(
    context: Context,
    private val onEvent: (SourceEvent) -> Unit,
) : TestMediaSource {

    override val type: InputSourceType = InputSourceType.TEST_VIDEO

    private val appContext = context.applicationContext
    private var exoPlayer: ExoPlayer? = ExoPlayer.Builder(appContext).build()
    private var media: TestMediaInfo? = null
    private val running = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var sampleRunnable: Runnable? = null

    override val player: Player?
        get() = exoPlayer

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val p = exoPlayer ?: return
            onEvent(
                SourceEvent.Playback(
                    isPlaying = isPlaying,
                    positionMs = p.currentPosition.coerceAtLeast(0L),
                    durationMs = p.duration.coerceAtLeast(0L),
                ),
            )
            if (isPlaying) startSampling() else stopSampling()
        }

        override fun onPlayerError(error: PlaybackException) {
            val message = error.message ?: "Unable to load this test video."
            SandboxLogger.videoError(message)
            onEvent(SourceEvent.Error(message))
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val p = exoPlayer ?: return
            onEvent(
                SourceEvent.Playback(
                    isPlaying = p.isPlaying,
                    positionMs = p.currentPosition.coerceAtLeast(0L),
                    durationMs = p.duration.coerceAtLeast(0L),
                ),
            )
        }
    }

    init {
        exoPlayer?.addListener(listener)
    }

    override fun setMedia(info: TestMediaInfo) {
        if (info.sizeBytes <= 0L) {
            onEvent(SourceEvent.Error("Empty file"))
            return
        }
        if (info.sizeBytes > MAX_BYTES) {
            onEvent(SourceEvent.Error("File is too large (max 100 MB)"))
            return
        }
        val mime = info.mimeType.orEmpty()
        if (mime.isNotEmpty() && !mime.startsWith("video/")) {
            onEvent(SourceEvent.Error("Unsupported video format"))
            return
        }
        media = info
        val player = exoPlayer ?: return
        try {
            player.setMediaItem(MediaItem.fromUri(info.uri))
            player.prepare()
            player.playWhenReady = false
            onEvent(SourceEvent.Status("TEST SOURCE ready: ${info.displayName}"))
        } catch (e: Exception) {
            onEvent(SourceEvent.Error("Unable to load this test video."))
            SandboxLogger.videoError(e.message ?: "load failed")
        }
    }

    override fun clearMedia() {
        stopSampling()
        media = null
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        onEvent(SourceEvent.Status("Test video removed"))
    }

    override fun currentMedia(): TestMediaInfo? = media

    override fun play() {
        if (media == null) {
            onEvent(SourceEvent.Error("No test video selected"))
            return
        }
        exoPlayer?.play()
    }

    override fun pause() {
        exoPlayer?.pause()
    }

    override fun restart() {
        val player = exoPlayer ?: return
        if (media == null) {
            onEvent(SourceEvent.Error("No test video selected"))
            return
        }
        player.seekTo(0)
        player.playWhenReady = true
    }

    override fun start() {
        running.set(true)
        if (exoPlayer?.isPlaying == true) startSampling()
    }

    override fun stop() {
        running.set(false)
        stopSampling()
        exoPlayer?.pause()
    }

    override fun release() {
        stop()
        exoPlayer?.removeListener(listener)
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun startSampling() {
        stopSampling()
        if (!running.get()) return
        val runnable = object : Runnable {
            override fun run() {
                if (!running.get()) return
                sampleFrame()
                mainHandler.postDelayed(this, SAMPLE_INTERVAL_MS)
            }
        }
        sampleRunnable = runnable
        mainHandler.post(runnable)
    }

    private fun stopSampling() {
        sampleRunnable?.let { mainHandler.removeCallbacks(it) }
        sampleRunnable = null
    }

    private fun sampleFrame() {
        val info = media ?: return
        val player = exoPlayer ?: return
        if (!player.isPlaying) return
        val position = player.currentPosition.coerceAtLeast(0L)
        val bitmap = retrieveFrame(info.uri, position) ?: return
        onEvent(SourceEvent.Frame(bitmap, position))
        onEvent(
            SourceEvent.Playback(
                isPlaying = true,
                positionMs = position,
                durationMs = player.duration.coerceAtLeast(0L),
            ),
        )
    }

    private fun retrieveFrame(uri: Uri, positionMs: Long): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(appContext, uri)
            retriever.getFrameAtTime(
                positionMs * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST,
            )
        } catch (e: Exception) {
            SandboxLogger.videoError("Frame sample failed: ${e.message}")
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        private const val SAMPLE_INTERVAL_MS = 250L
        private const val MAX_BYTES = 100L * 1024L * 1024L
    }
}
