package com.rth.kycliveness.state

enum class TestPhase {
    IDLE,
    STARTED,
    WAITING_FOR_ACTION,
    ACTION_PLAYING,
    ACTION_COMPLETED,
    NEXT_ACTION,
    RETRY,
    PASSED,
    FAILED,
}

enum class TestResult {
    NONE,
    PASSED,
    FAILED,
}

data class TestSnapshot(
    val phase: TestPhase = TestPhase.IDLE,
    val currentActionIndex: Int = 0,
    val completedActions: List<String> = emptyList(),
    val failedActions: List<String> = emptyList(),
    val retriesUsed: Int = 0,
    val maxRetries: Int = 2,
    val totalActions: Int = 0,
    val result: TestResult = TestResult.NONE,
    val lastError: String? = null,
    val startedAtMillis: Long? = null,
    val elapsedMillis: Long = 0L,
) {
    val progressCompleted: Int get() = completedActions.size
    val progressTotal: Int get() = totalActions
}
