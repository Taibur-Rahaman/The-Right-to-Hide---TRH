package com.rth.kycliveness

import com.rth.kycliveness.data.ProfileCatalog
import com.rth.kycliveness.data.TestAction
import com.rth.kycliveness.state.KycTestStateMachine
import com.rth.kycliveness.state.TestPhase
import com.rth.kycliveness.state.TestResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UI-flow logic tests without Espresso — exercises the same controller the UI drives.
 */
class SandboxViewModelTest {
    @Test
    fun startBeginsWaitingForRequiredAction() {
        val machine = KycTestStateMachine()
        val profile = ProfileCatalog.defaultProfile()
        val snap = machine.start(profile)
        assertEquals(TestPhase.WAITING_FOR_ACTION, snap.phase)
        assertEquals(profile.actions.first(), machine.requiredAction())
        assertTrue(snap.totalActions > 0)
    }

    @Test
    fun resetReturnsToIdle() {
        val machine = KycTestStateMachine()
        machine.start(ProfileCatalog.findById("nagad")!!)
        machine.beginAction(TestAction.LOOK_STRAIGHT)
        machine.onActionCompleted()
        val snap = machine.reset()
        assertEquals(TestPhase.IDLE, snap.phase)
        assertEquals(0, snap.progressCompleted)
        assertEquals(TestResult.NONE, snap.result)
    }

    @Test
    fun moreMenuProfilesAreSelectable() {
        val more = ProfileCatalog.moreProfiles
        assertTrue(more.size >= 10)
        assertTrue(more.any { it.displayName == "Custom" })
        assertTrue(more.any { it.displayName == "PayPal" })
        val selected = more.first { it.id == "wise" }
        val machine = KycTestStateMachine()
        machine.start(selected)
        assertEquals(TestAction.CLOSE_BOTH_EYES, machine.requiredAction())
    }

    @Test
    fun profileSelectionLoadsDistinctSequences() {
        val bkash = ProfileCatalog.findById("bkash")!!
        val nagad = ProfileCatalog.findById("nagad")!!
        val binance = ProfileCatalog.findById("binance")!!
        assertFalse(bkash.actions == nagad.actions)
        assertFalse(nagad.actions == binance.actions)
        assertEquals("bKash", bkash.displayName)
        assertEquals("Nagad", nagad.displayName)
        assertEquals("Binance", binance.displayName)
    }

    @Test
    fun actionSelectionPlaysMatchingRequiredAction() {
        val machine = KycTestStateMachine()
        machine.start(ProfileCatalog.findById("bkash")!!)
        val required = machine.requiredAction()!!
        assertEquals(TestAction.BLINK_ONCE, required)
        val playing = machine.beginAction(TestAction.BLINK_ONCE)
        assertEquals(TestPhase.ACTION_PLAYING, playing.phase)
        val after = machine.onActionCompleted()
        assertEquals(listOf("Blink Once"), after.completedActions)
        assertEquals(TestAction.HEAD_LEFT, machine.requiredAction())
    }
}
