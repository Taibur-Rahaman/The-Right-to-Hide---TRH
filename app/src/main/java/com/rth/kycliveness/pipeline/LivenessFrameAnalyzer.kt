package com.rth.kycliveness.pipeline

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.rth.kycliveness.data.TestAction
import java.util.concurrent.atomic.AtomicBoolean

data class FaceSignals(
    val faceDetected: Boolean,
    val smilingProbability: Float? = null,
    val leftEyeOpenProbability: Float? = null,
    val rightEyeOpenProbability: Float? = null,
    val headEulerX: Float? = null,
    val headEulerY: Float? = null,
    val headEulerZ: Float? = null,
    val timestampMs: Long = 0L,
)

/**
 * Shared face analyzer used by both live camera frames and test-video frames.
 * Does not invent verification success — only reports facial signals.
 */
class LivenessFrameAnalyzer(
    private val onSignals: (FaceSignals) -> Unit,
    private val onError: (String) -> Unit,
) {
    private val busy = AtomicBoolean(false)
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .enableTracking()
            .build(),
    )

    fun analyze(bitmap: Bitmap, timestampMs: Long) {
        if (!busy.compareAndSet(false, true)) {
            bitmap.recycle()
            return
        }
        val image = InputImage.fromBitmap(bitmap, 0)
        detector.process(image)
            .addOnSuccessListener { faces ->
                val signals = faces.firstOrNull()?.toSignals(timestampMs)
                    ?: FaceSignals(faceDetected = false, timestampMs = timestampMs)
                onSignals(signals)
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Face detection failed")
            }
            .addOnCompleteListener {
                busy.set(false)
                if (!bitmap.isRecycled) bitmap.recycle()
            }
    }

    fun close() {
        detector.close()
    }

    private fun Face.toSignals(timestampMs: Long) = FaceSignals(
        faceDetected = true,
        smilingProbability = smilingProbability,
        leftEyeOpenProbability = leftEyeOpenProbability,
        rightEyeOpenProbability = rightEyeOpenProbability,
        headEulerX = headEulerAngleX,
        headEulerY = headEulerAngleY,
        headEulerZ = headEulerAngleZ,
        timestampMs = timestampMs,
    )
}

/**
 * Maps facial signals over time to a required [TestAction].
 * Conservative heuristics — wrong/noisy input must not auto-pass.
 */
class ActionMatcher {
    private var eyeClosedStreak = 0
    private var eyeOpenAfterBlink = false
    private var blinkCount = 0
    private var holdFrames = 0
    private var lastMatched: TestAction? = null
    private var lastMatchAt = 0L

    fun reset() {
        eyeClosedStreak = 0
        eyeOpenAfterBlink = false
        blinkCount = 0
        holdFrames = 0
        lastMatched = null
        lastMatchAt = 0L
    }

    /**
     * @return matched action when confidence threshold met, else null
     */
    fun evaluate(required: TestAction, signals: FaceSignals): TestAction? {
        if (!signals.faceDetected) {
            holdFrames = 0
            return null
        }
        // Debounce repeated matches
        if (lastMatched == required && signals.timestampMs - lastMatchAt < 1500) {
            return null
        }

        val matched = when (required) {
            TestAction.BLINK_ONCE -> detectBlink(signals, need = 1)
            TestAction.BLINK_TWICE -> detectBlink(signals, need = 2)
            TestAction.CLOSE_BOTH_EYES -> bothEyesClosed(signals)
            TestAction.OPEN_BOTH_EYES, TestAction.HOLD_EYES_OPEN -> bothEyesOpen(signals)
            TestAction.SMILE -> (signals.smilingProbability ?: 0f) > 0.65f
            TestAction.NEUTRAL_FACE -> (signals.smilingProbability ?: 1f) < 0.25f &&
                bothEyesOpen(signals)
            TestAction.LOOK_STRAIGHT, TestAction.FACE_CENTER, TestAction.CENTER_FACE,
            TestAction.HOLD_HEAD_STILL, TestAction.HOLD_POSITION,
            -> headCentered(signals) && hold(8)

            TestAction.LOOK_UP -> (signals.headEulerX ?: 0f) < -12f && hold(5)
            TestAction.LOOK_DOWN -> (signals.headEulerX ?: 0f) > 12f && hold(5)
            TestAction.HEAD_LEFT, TestAction.TURN_SLIGHTLY_LEFT ->
                (signals.headEulerY ?: 0f) > 12f && hold(5)

            TestAction.HEAD_RIGHT, TestAction.TURN_SLIGHTLY_RIGHT ->
                (signals.headEulerY ?: 0f) < -12f && hold(5)

            TestAction.NOD -> detectNod(signals)
            TestAction.SHAKE_HEAD -> detectShake(signals)
            TestAction.OPEN_MOUTH, TestAction.CLOSE_MOUTH, TestAction.RAISE_EYEBROWS,
            TestAction.MOVE_CLOSER, TestAction.MOVE_FARTHER,
            -> facePresentHold(signals)

            else -> false
        }

        return if (matched) {
            lastMatched = required
            lastMatchAt = signals.timestampMs
            required
        } else {
            null
        }
    }

    private fun bothEyesOpen(s: FaceSignals): Boolean {
        val l = s.leftEyeOpenProbability ?: return false
        val r = s.rightEyeOpenProbability ?: return false
        return l > 0.6f && r > 0.6f
    }

    private fun bothEyesClosed(s: FaceSignals): Boolean {
        val l = s.leftEyeOpenProbability ?: return false
        val r = s.rightEyeOpenProbability ?: return false
        return l < 0.3f && r < 0.3f && hold(5)
    }

    private fun headCentered(s: FaceSignals): Boolean {
        val x = kotlin.math.abs(s.headEulerX ?: 99f)
        val y = kotlin.math.abs(s.headEulerY ?: 99f)
        return x < 10f && y < 10f
    }

    private fun facePresentHold(s: FaceSignals): Boolean =
        s.faceDetected && hold(10)

    private fun hold(needed: Int): Boolean {
        holdFrames++
        return if (holdFrames >= needed) {
            holdFrames = 0
            true
        } else {
            false
        }
    }

    private fun detectBlink(s: FaceSignals, need: Int): Boolean {
        val closed = bothEyesClosedLoose(s)
        val open = bothEyesOpen(s)
        if (closed) {
            eyeClosedStreak++
        } else if (eyeClosedStreak >= 1 && open) {
            blinkCount++
            eyeClosedStreak = 0
            eyeOpenAfterBlink = true
        } else if (open) {
            eyeClosedStreak = 0
        }
        if (blinkCount >= need) {
            blinkCount = 0
            eyeOpenAfterBlink = false
            return true
        }
        return false
    }

    private fun bothEyesClosedLoose(s: FaceSignals): Boolean {
        val l = s.leftEyeOpenProbability ?: return false
        val r = s.rightEyeOpenProbability ?: return false
        return l < 0.35f && r < 0.35f
    }

    private var nodPhase = 0
    private var shakePhase = 0

    private fun detectNod(s: FaceSignals): Boolean {
        val x = s.headEulerX ?: return false
        when (nodPhase) {
            0 -> if (x < -10f) nodPhase = 1
            1 -> if (x > 10f) nodPhase = 2
            2 -> if (kotlin.math.abs(x) < 8f) {
                nodPhase = 0
                return true
            }
        }
        return false
    }

    private fun detectShake(s: FaceSignals): Boolean {
        val y = s.headEulerY ?: return false
        when (shakePhase) {
            0 -> if (y > 12f) shakePhase = 1
            1 -> if (y < -12f) shakePhase = 2
            2 -> if (kotlin.math.abs(y) < 8f) {
                shakePhase = 0
                return true
            }
        }
        return false
    }
}
