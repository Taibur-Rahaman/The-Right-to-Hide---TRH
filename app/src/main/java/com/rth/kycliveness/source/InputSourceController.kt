package com.rth.kycliveness.source

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.Player

/**
 * Owns the active [CameraSource] and switches between live camera / test media
 * without changing the downstream frame → liveness pipeline.
 */
class InputSourceController(
    private val context: Context,
    private val onEvent: (SourceEvent) -> Unit,
) {
    private var live: LiveCameraSourceImpl? = null
    private var testVideo: TestVideoSourceImpl? = null
    private var testImage: TestImageSourceImpl? = null
    private var active: CameraSource? = null
    private var type: InputSourceType = InputSourceType.LIVE_CAMERA

    fun currentType(): InputSourceType = type

    fun activeSource(): CameraSource? = active

    fun liveCamera(): LiveCameraSource? = live

    fun testMedia(): TestMediaSource? = when (type) {
        InputSourceType.TEST_VIDEO -> testVideo
        InputSourceType.TEST_IMAGE -> testImage
        else -> null
    }

    fun exoPlayer(): Player? = testVideo?.player

    fun select(type: InputSourceType) {
        stopActive()
        this.type = type
        active = when (type) {
            InputSourceType.LIVE_CAMERA -> {
                val src = live ?: LiveCameraSourceImpl(context, onEvent).also { live = it }
                src
            }
            InputSourceType.TEST_VIDEO -> {
                val src = testVideo ?: TestVideoSourceImpl(context, onEvent).also { testVideo = it }
                src
            }
            InputSourceType.TEST_IMAGE -> {
                val src = testImage ?: TestImageSourceImpl(context, onEvent).also { testImage = it }
                src
            }
        }
    }

    fun bindLivePreview(owner: LifecycleOwner, previewView: PreviewView) {
        ensureLive()
        live?.bindPreview(owner, previewView)
    }

    fun startActive() {
        if (active == null) select(type)
        active?.start()
    }

    fun stopActive() {
        active?.stop()
    }

    fun releaseAll() {
        live?.release()
        testVideo?.release()
        testImage?.release()
        live = null
        testVideo = null
        testImage = null
        active = null
    }

    private fun ensureLive() {
        if (live == null) {
            live = LiveCameraSourceImpl(context, onEvent)
        }
    }
}
