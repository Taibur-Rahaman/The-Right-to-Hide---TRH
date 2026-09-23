package com.rth.kycliveness.source

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.rth.kycliveness.logging.SandboxLogger

/**
 * Static image test source. Emits a single frame for inspection, but the
 * motion-based liveness engine must reject it as insufficient for verification.
 */
class TestImageSourceImpl(
    private val context: Context,
    private val onEvent: (SourceEvent) -> Unit,
) : TestMediaSource {

    override val type: InputSourceType = InputSourceType.TEST_IMAGE
    override val player get() = null

    private var media: TestMediaInfo? = null

    override fun setMedia(info: TestMediaInfo) {
        if (info.sizeBytes <= 0L) {
            onEvent(SourceEvent.Error("Empty file"))
            return
        }
        if (info.sizeBytes > MAX_BYTES) {
            onEvent(SourceEvent.Error("File is too large (max 20 MB)"))
            return
        }
        val mime = info.mimeType.orEmpty()
        if (mime.isNotEmpty() && !mime.startsWith("image/")) {
            onEvent(SourceEvent.Error("Unsupported image format"))
            return
        }
        media = info
        try {
            context.contentResolver.openInputStream(info.uri)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap == null) {
                    onEvent(SourceEvent.Error("Unable to load this test image."))
                    return
                }
                onEvent(SourceEvent.Frame(bitmap, 0L))
                onEvent(
                    SourceEvent.Error(
                        "Static image cannot be used for this liveness test.",
                    ),
                )
                onEvent(SourceEvent.Status("TEST SOURCE image loaded (not valid for motion liveness)"))
            } ?: onEvent(SourceEvent.Error("Unable to load this test image."))
        } catch (e: Exception) {
            SandboxLogger.videoError(e.message ?: "image load failed")
            onEvent(SourceEvent.Error("Unable to load this test image."))
        }
    }

    override fun clearMedia() {
        media = null
        onEvent(SourceEvent.Status("Test image removed"))
    }

    override fun currentMedia(): TestMediaInfo? = media
    override fun play() = Unit
    override fun pause() = Unit
    override fun restart() {
        media?.let { setMedia(it) }
    }

    override fun start() = Unit
    override fun stop() = Unit
    override fun release() {
        media = null
    }

    companion object {
        private const val MAX_BYTES = 20L * 1024L * 1024L
    }
}
