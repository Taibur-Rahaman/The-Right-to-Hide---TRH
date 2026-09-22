package com.rth.kycliveness

import com.rth.kycliveness.data.ClipCatalog
import com.rth.kycliveness.data.ClipResolution
import com.rth.kycliveness.data.TestAction
import com.rth.kycliveness.player.PlayerEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Media contract tests without shipping binary mp4 files.
 */
class ClipCatalogTest {
    @Test
    fun validClipMappingExistsForPrimaryActions() {
        assertEquals("blink", ClipCatalog.clipNameFor(TestAction.BLINK_ONCE))
        assertEquals("blink_twice", ClipCatalog.clipNameFor(TestAction.BLINK_TWICE))
        assertEquals("left", ClipCatalog.clipNameFor(TestAction.HEAD_LEFT))
        assertEquals("right", ClipCatalog.clipNameFor(TestAction.HEAD_RIGHT))
        assertEquals("nod", ClipCatalog.clipNameFor(TestAction.NOD))
        assertEquals("mouth", ClipCatalog.clipNameFor(TestAction.OPEN_MOUTH))
        assertEquals("smile", ClipCatalog.clipNameFor(TestAction.SMILE))
        assertEquals("look_up", ClipCatalog.clipNameFor(TestAction.LOOK_UP))
        assertEquals("look_down", ClipCatalog.clipNameFor(TestAction.LOOK_DOWN))
        assertEquals("shake", ClipCatalog.clipNameFor(TestAction.SHAKE_HEAD))
        assertEquals("neutral", ClipCatalog.clipNameFor(TestAction.NEUTRAL_FACE))
        assertEquals("center", ClipCatalog.clipNameFor(TestAction.CENTER_FACE))
        assertEquals("closer", ClipCatalog.clipNameFor(TestAction.MOVE_CLOSER))
        assertEquals("farther", ClipCatalog.clipNameFor(TestAction.MOVE_FARTHER))
        assertEquals("hold", ClipCatalog.clipNameFor(TestAction.HOLD_POSITION))
    }

    @Test
    fun missingClipResolutionIsUnavailable() {
        val missing = ClipResolution(
            action = TestAction.SMILE,
            clipName = "smile",
            rawResId = 0,
        )
        assertFalse(missing.isAvailable)
    }

    @Test
    fun invalidClipTreatedAsUnavailable() {
        val invalid = ClipResolution(
            action = TestAction.NOD,
            clipName = "nod",
            rawResId = 0,
        )
        assertFalse(invalid.isAvailable)
        assertEquals("nod", invalid.clipName)
    }

    @Test
    fun completionCallbackContract() {
        val events = mutableListOf<PlayerEvent>()
        val onEvent: (PlayerEvent) -> Unit = { events.add(it) }
        onEvent(PlayerEvent.Completed(TestAction.BLINK_ONCE))
        assertEquals(1, events.size)
        assertTrue(events.first() is PlayerEvent.Completed)
        assertEquals(TestAction.BLINK_ONCE, (events.first() as PlayerEvent.Completed).action)
    }

    @Test
    fun missingClipErrorEventContract() {
        val events = mutableListOf<PlayerEvent>()
        events.add(
            PlayerEvent.Error(
                action = TestAction.SMILE,
                message = "Test clip unavailable",
                missing = true,
            ),
        )
        val error = events.first() as PlayerEvent.Error
        assertTrue(error.missing)
        assertEquals("Test clip unavailable", error.message)
    }
}
