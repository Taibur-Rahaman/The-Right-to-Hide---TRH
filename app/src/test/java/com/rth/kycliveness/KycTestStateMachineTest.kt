package com.rth.kycliveness

import com.rth.kycliveness.data.KycTestProfile
import com.rth.kycliveness.data.TestAction
import com.rth.kycliveness.state.KycTestStateMachine
import com.rth.kycliveness.state.TestPhase
import com.rth.kycliveness.state.TestResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KycTestStateMachineTest {
    private lateinit var machine: KycTestStateMachine
    private var now = 1_000_000L

    private val profile = KycTestProfile(
        id = "unit",
        displayName = "Unit",
        actions = listOf(
            TestAction.BLINK_ONCE,
            TestAction.SMILE,
            TestAction.NOD,
        ),
        timeoutSeconds = 10,
        retryCount = 2,
    )

    @Before
    fun setUp() {
        now = 1_000_000L
        machine = KycTestStateMachine(clock = { now })
    }

    @Test
    fun initialStateIsIdle() {
        val snap = machine.snapshot()
        assertEquals(TestPhase.IDLE, snap.phase)
        assertEquals(0, snap.progressCompleted)
        assertNull(machine.requiredAction())
        assertEquals(TestResult.NONE, snap.result)
    }

    @Test
    fun startMovesToWaitingForAction() {
        val snap = machine.start(profile)
        assertEquals(TestPhase.WAITING_FOR_ACTION, snap.phase)
        assertEquals(TestAction.BLINK_ONCE, machine.requiredAction())
        assertEquals(3, snap.totalActions)
    }

    @Test
    fun actionCompletionAdvances() {
        machine.start(profile)
        machine.beginAction(TestAction.BLINK_ONCE)
        val snap = machine.onActionCompleted()
        assertEquals(TestPhase.WAITING_FOR_ACTION, snap.phase)
        assertEquals(listOf("Blink Once"), snap.completedActions)
        assertEquals(TestAction.SMILE, machine.requiredAction())
    }

    @Test
    fun nextActionAfterCompletion() {
        machine.start(profile)
        machine.beginAction(TestAction.BLINK_ONCE)
        machine.onActionCompleted()
        machine.beginAction(TestAction.SMILE)
        val snap = machine.onActionCompleted()
        assertEquals(TestPhase.WAITING_FOR_ACTION, snap.phase)
        assertEquals(TestAction.NOD, machine.requiredAction())
        assertEquals(2, snap.progressCompleted)
    }

    @Test
    fun retryAfterFailureWhilePlaying() {
        machine.start(profile)
        machine.beginAction(TestAction.BLINK_ONCE)
        val snap = machine.onActionFailed("timeout")
        assertEquals(TestPhase.ACTION_PLAYING, snap.phase)
        assertEquals(1, snap.retriesUsed)
        assertEquals(listOf("Blink Once"), snap.failedActions)
        assertEquals(TestAction.BLINK_ONCE, machine.requiredAction())
    }

    @Test
    fun failureAfterRetryLimit() {
        machine.start(profile)
        machine.beginAction(TestAction.BLINK_ONCE)
        machine.onActionFailed("1")
        machine.onActionFailed("2")
        val snap = machine.onActionFailed("3")
        assertEquals(TestPhase.FAILED, snap.phase)
        assertEquals(TestResult.FAILED, snap.result)
        assertTrue(snap.retriesUsed > snap.maxRetries)
    }

    @Test
    fun passWhenAllActionsComplete() {
        machine.start(profile)
        profile.actions.forEach { action ->
            machine.beginAction(action)
            machine.onActionCompleted()
        }
        val snap = machine.snapshot()
        assertEquals(TestPhase.PASSED, snap.phase)
        assertEquals(TestResult.PASSED, snap.result)
        assertEquals(3, snap.completedActions.size)
    }

    @Test
    fun resetClearsEverything() {
        machine.start(profile)
        machine.beginAction(TestAction.BLINK_ONCE)
        machine.onActionCompleted()
        now += 5_000
        machine.tickElapsed()
        val snap = machine.reset()
        assertEquals(TestPhase.IDLE, snap.phase)
        assertEquals(0, snap.progressCompleted)
        assertEquals(TestResult.NONE, snap.result)
        assertNull(snap.lastError)
        assertNull(machine.requiredAction())
        assertEquals(0L, snap.elapsedMillis)
        assertTrue(snap.completedActions.isEmpty())
        assertTrue(snap.failedActions.isEmpty())
    }

    @Test
    fun wrongActionUsesRetryThenWaiting() {
        machine.start(profile)
        val snap = machine.beginAction(TestAction.SMILE)
        assertEquals(TestPhase.WAITING_FOR_ACTION, snap.phase)
        assertEquals(1, snap.retriesUsed)
        assertEquals(TestAction.BLINK_ONCE, machine.requiredAction())
    }
}
