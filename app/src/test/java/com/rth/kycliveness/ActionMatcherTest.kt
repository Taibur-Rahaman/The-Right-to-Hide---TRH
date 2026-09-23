package com.rth.kycliveness

import com.rth.kycliveness.data.TestAction
import com.rth.kycliveness.pipeline.ActionMatcher
import com.rth.kycliveness.pipeline.FaceSignals
import com.rth.kycliveness.source.InputSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionMatcherTest {
    @Test
    fun smileRequiresHighProbability() {
        val matcher = ActionMatcher()
        val low = FaceSignals(faceDetected = true, smilingProbability = 0.2f, timestampMs = 1)
        assertNull(matcher.evaluate(TestAction.SMILE, low))
        val high = FaceSignals(faceDetected = true, smilingProbability = 0.9f, timestampMs = 2)
        assertEquals(TestAction.SMILE, matcher.evaluate(TestAction.SMILE, high))
    }

    @Test
    fun noFaceNeverMatches() {
        val matcher = ActionMatcher()
        val signals = FaceSignals(faceDetected = false, smilingProbability = 1f, timestampMs = 1)
        assertNull(matcher.evaluate(TestAction.SMILE, signals))
    }
}

class InputSourceTypeTest {
    @Test
    fun sourceTypesExist() {
        assertTrue(InputSourceType.entries.contains(InputSourceType.LIVE_CAMERA))
        assertTrue(InputSourceType.entries.contains(InputSourceType.TEST_VIDEO))
        assertTrue(InputSourceType.entries.contains(InputSourceType.TEST_IMAGE))
        assertNotNull(InputSourceType.LIVE_CAMERA)
    }
}
