package com.rth.kycliveness.logging

import android.util.Log

object SandboxLogger {
    const val TAG = "KycSandbox"

    fun testStarted(profileId: String) = log("TEST_STARTED", "profile=$profileId")
    fun actionStarted(action: String) = log("ACTION_STARTED", action)
    fun actionCompleted(action: String) = log("ACTION_COMPLETED", action)
    fun actionFailed(action: String, reason: String) = log("ACTION_FAILED", "$action reason=$reason")
    fun retry(action: String, attempt: Int) = log("RETRY", "$action attempt=$attempt")
    fun testPassed() = log("TEST_PASSED")
    fun testFailed() = log("TEST_FAILED")
    fun reset() = log("RESET")
    fun videoError(message: String) = log("VIDEO_ERROR", message)

    private fun log(event: String, detail: String = "") {
        if (detail.isEmpty()) {
            Log.d(TAG, event)
        } else {
            Log.d(TAG, "$event | $detail")
        }
    }
}
