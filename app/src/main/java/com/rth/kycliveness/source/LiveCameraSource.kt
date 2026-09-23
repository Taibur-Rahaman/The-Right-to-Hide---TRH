package com.rth.kycliveness.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.rth.kycliveness.logging.SandboxLogger
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real device camera source via CameraX Preview + ImageAnalysis.
 * Frames are emitted into the shared sandbox liveness pipeline.
 */
class LiveCameraSourceImpl(
    private val context: Context,
    private val onEvent: (SourceEvent) -> Unit,
) : LiveCameraSource {

    override val type: InputSourceType = InputSourceType.LIVE_CAMERA

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var previewView: PreviewView? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT
    private val running = AtomicBoolean(false)
    private var lastFrameEmitMs = 0L

    override fun bindPreview(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        this.lifecycleOwner = lifecycleOwner
        this.previewView = previewView
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                try {
                    cameraProvider = future.get()
                    if (running.get()) {
                        bindUseCases()
                    }
                } catch (e: Exception) {
                    SandboxLogger.videoError("Camera provider failed: ${e.message}")
                    onEvent(SourceEvent.Error("Camera initialization failure: ${e.message}"))
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    override fun start() {
        running.set(true)
        if (cameraProvider != null) {
            bindUseCases()
        }
    }

    override fun stop() {
        running.set(false)
        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {
        }
        camera = null
    }

    override fun release() {
        stop()
        analysisExecutor.shutdown()
        cameraProvider = null
        previewView = null
        lifecycleOwner = null
    }

    override fun listCameras(): List<CameraDeviceInfo> {
        val provider = cameraProvider ?: return emptyList()
        val devices = mutableListOf<CameraDeviceInfo>()
        if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
            devices += CameraDeviceInfo("front", "Front Camera", CameraSelector.LENS_FACING_FRONT)
        }
        if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
            devices += CameraDeviceInfo("back", "Back Camera", CameraSelector.LENS_FACING_BACK)
        }
        return devices
    }

    override fun switchCamera(): CameraDeviceInfo? {
        val devices = listCameras()
        if (devices.size < 2) {
            onEvent(SourceEvent.Status("Only one camera available"))
            return activeCamera()
        }
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        if (running.get()) bindUseCases()
        return activeCamera()
    }

    override fun activeCamera(): CameraDeviceInfo? =
        listCameras().firstOrNull { it.lensFacing == lensFacing }

    private fun bindUseCases() {
        val owner = lifecycleOwner ?: return
        val view = previewView ?: return
        val provider = cameraProvider ?: return
        try {
            provider.unbindAll()
            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = view.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analyzer ->
                    analyzer.setAnalyzer(analysisExecutor) { image ->
                        processImage(image)
                    }
                }
            camera = provider.bindToLifecycle(owner, selector, preview, analysis)
            onEvent(SourceEvent.Status("Live camera active: ${activeCamera()?.label}"))
        } catch (e: Exception) {
            SandboxLogger.videoError("Camera bind failed: ${e.message}")
            onEvent(SourceEvent.Error("Unable to start camera: ${e.message}"))
        }
    }

    private fun processImage(image: ImageProxy) {
        try {
            if (!running.get()) return
            val now = System.currentTimeMillis()
            // Throttle for ML Kit (~5 fps)
            if (now - lastFrameEmitMs < 200) return
            lastFrameEmitMs = now
            val bitmap = imageProxyToBitmap(image) ?: return
            onEvent(SourceEvent.Frame(bitmap, now))
        } finally {
            image.close()
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            val yuv = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuv.compressToJpeg(Rect(0, 0, image.width, image.height), 70, out)
            val bytes = out.toByteArray()
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return null
            val rotation = image.imageInfo.rotationDegrees
            if (rotation == 0) bitmap else {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    .also { if (it != bitmap) bitmap.recycle() }
            }
        } catch (_: Exception) {
            null
        }
    }
}
