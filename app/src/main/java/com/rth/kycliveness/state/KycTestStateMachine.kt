package com.rth.kycliveness.state

import com.rth.kycliveness.data.KycTestProfile
import com.rth.kycliveness.data.TestAction

/**
 * Dedicated controller for KYC sandbox test phase transitions.
 * UI and ViewModel must not invent transitions outside this class.
 */
class KycTestStateMachine(
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private var profile: KycTestProfile? = null
    private var snapshot: TestSnapshot = TestSnapshot()

    fun snapshot(): TestSnapshot = snapshot

    fun requiredAction(): TestAction? {
        val p = profile ?: return null
        if (snapshot.phase == TestPhase.IDLE ||
            snapshot.phase == TestPhase.PASSED ||
            snapshot.phase == TestPhase.FAILED
        ) {
            return null
        }
        if (snapshot.currentActionIndex !in p.actions.indices) return null
        return p.actions[snapshot.currentActionIndex]
    }

    fun start(selectedProfile: KycTestProfile): TestSnapshot {
        require(selectedProfile.actions.isNotEmpty()) { "Profile must include actions" }
        profile = selectedProfile
        snapshot = TestSnapshot(
            phase = TestPhase.STARTED,
            currentActionIndex = 0,
            totalActions = selectedProfile.actions.size,
            maxRetries = selectedProfile.retryCount,
            startedAtMillis = clock(),
            result = TestResult.NONE,
        )
        snapshot = snapshot.copy(phase = TestPhase.WAITING_FOR_ACTION, elapsedMillis = elapsed())
        return snapshot
    }

    fun beginAction(selected: TestAction): TestSnapshot {
        ensureActive()
        val required = requiredAction() ?: return failHard("No required action")
        if (snapshot.phase != TestPhase.WAITING_FOR_ACTION &&
            snapshot.phase != TestPhase.RETRY
        ) {
            return snapshot
        }
        return if (selected == required) {
            snapshot = snapshot.copy(
                phase = TestPhase.ACTION_PLAYING,
                lastError = null,
                elapsedMillis = elapsed(),
            )
            snapshot
        } else {
            registerFailure(
                actionLabel = required.displayLabel(),
                reason = "Wrong action selected",
                resumePlaying = false,
            )
        }
    }

    fun onActionCompleted(): TestSnapshot {
        if (snapshot.phase != TestPhase.ACTION_PLAYING) return snapshot
        val action = requiredAction() ?: return failHard("Missing action on completion")
        val completed = snapshot.completedActions + action.displayLabel()
        snapshot = snapshot.copy(
            phase = TestPhase.ACTION_COMPLETED,
            completedActions = completed,
            retriesUsed = 0,
            lastError = null,
            elapsedMillis = elapsed(),
        )
        return advanceAfterCompleted()
    }

    fun onActionFailed(reason: String): TestSnapshot {
        if (snapshot.phase != TestPhase.ACTION_PLAYING &&
            snapshot.phase != TestPhase.WAITING_FOR_ACTION &&
            snapshot.phase != TestPhase.RETRY
        ) {
            return snapshot
        }
        val label = requiredAction()?.displayLabel() ?: "Unknown"
        val resume = snapshot.phase == TestPhase.ACTION_PLAYING
        return registerFailure(label, reason, resumePlaying = resume)
    }

    fun reset(): TestSnapshot {
        profile = null
        snapshot = TestSnapshot()
        return snapshot
    }

    fun tickElapsed(): TestSnapshot {
        if (snapshot.startedAtMillis == null) return snapshot
        if (snapshot.phase == TestPhase.IDLE ||
            snapshot.phase == TestPhase.PASSED ||
            snapshot.phase == TestPhase.FAILED
        ) {
            return snapshot
        }
        snapshot = snapshot.copy(elapsedMillis = elapsed())
        return snapshot
    }

    private fun advanceAfterCompleted(): TestSnapshot {
        val p = profile ?: return failHard("Profile missing")
        val nextIndex = snapshot.currentActionIndex + 1
        snapshot = snapshot.copy(phase = TestPhase.NEXT_ACTION, elapsedMillis = elapsed())
        return if (nextIndex >= p.actions.size) {
            snapshot = snapshot.copy(
                phase = TestPhase.PASSED,
                result = TestResult.PASSED,
                currentActionIndex = nextIndex,
                elapsedMillis = elapsed(),
            )
            snapshot
        } else {
            snapshot = snapshot.copy(
                phase = TestPhase.WAITING_FOR_ACTION,
                currentActionIndex = nextIndex,
                elapsedMillis = elapsed(),
            )
            snapshot
        }
    }

    private fun registerFailure(
        actionLabel: String,
        reason: String,
        resumePlaying: Boolean,
    ): TestSnapshot {
        val failed = if (snapshot.failedActions.lastOrNull() == actionLabel) {
            snapshot.failedActions
        } else {
            snapshot.failedActions + actionLabel
        }
        val retriesUsed = snapshot.retriesUsed + 1
        snapshot = snapshot.copy(
            phase = TestPhase.FAILED,
            failedActions = failed,
            retriesUsed = retriesUsed,
            lastError = reason,
            elapsedMillis = elapsed(),
        )
        return if (retriesUsed <= snapshot.maxRetries) {
            snapshot = snapshot.copy(
                phase = TestPhase.RETRY,
                elapsedMillis = elapsed(),
            )
            snapshot = snapshot.copy(
                phase = if (resumePlaying) TestPhase.ACTION_PLAYING else TestPhase.WAITING_FOR_ACTION,
                elapsedMillis = elapsed(),
            )
            snapshot
        } else {
            snapshot = snapshot.copy(
                phase = TestPhase.FAILED,
                result = TestResult.FAILED,
                elapsedMillis = elapsed(),
            )
            snapshot
        }
    }

    private fun failHard(reason: String): TestSnapshot {
        snapshot = snapshot.copy(
            phase = TestPhase.FAILED,
            result = TestResult.FAILED,
            lastError = reason,
            elapsedMillis = elapsed(),
        )
        return snapshot
    }

    private fun ensureActive() {
        check(profile != null) { "Test not started" }
    }

    private fun elapsed(): Long {
        val started = snapshot.startedAtMillis ?: return 0L
        return (clock() - started).coerceAtLeast(0L)
    }
}
