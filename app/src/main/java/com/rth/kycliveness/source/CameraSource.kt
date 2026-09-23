package com.rth.kycliveness.source

import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.Player

/**
 * Abstraction over sandbox media input.
 * Live camera and developer test media both feed the same preview + frame pipeline.
 *
 * This is NOT a system-wide virtual camera and cannot inject into third-party apps.
 */
enum class InputSourceType {
    LIVE_CAMERA,
    TEST_VIDEO,
    TEST_IMAGE,
}

data class CameraDeviceInfo(
    val id: String,
    val label: String,
    val lensFacing: Int,
)

data class TestMediaInfo(
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val durationMs: Long? = null,
)

sealed class SourceEvent {
    data class Error(val message: String) : SourceEvent()
    data class Frame(val bitmap: Bitmap, val timestampMs: Long) : SourceEvent()
    data class Status(val message: String) : SourceEvent()
    data class Playback(
        val isPlaying: Boolean,
        val positionMs: Long,
        val durationMs: Long,
    ) : SourceEvent()
}

interface CameraSource {
    val type: InputSourceType
    fun start()
    fun stop()
    fun release()
}

interface LiveCameraSource : CameraSource {
    fun bindPreview(lifecycleOwner: LifecycleOwner, previewView: PreviewView)
    fun listCameras(): List<CameraDeviceInfo>
    fun switchCamera(): CameraDeviceInfo?
    fun activeCamera(): CameraDeviceInfo?
}

interface TestMediaSource : CameraSource {
    val player: Player?
    fun setMedia(info: TestMediaInfo)
    fun clearMedia()
    fun play()
    fun pause()
    fun restart()
    fun currentMedia(): TestMediaInfo?
}
