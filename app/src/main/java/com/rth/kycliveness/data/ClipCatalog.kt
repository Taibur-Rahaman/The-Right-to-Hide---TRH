package com.rth.kycliveness.data

import android.content.Context

data class ClipResolution(
    val action: TestAction,
    val clipName: String,
    val rawResId: Int,
) {
    val isAvailable: Boolean get() = rawResId != 0
}

object ClipCatalog {
    private val clipNameByAction: Map<TestAction, String> = mapOf(
        TestAction.LOOK_STRAIGHT to "center",
        TestAction.LOOK_UP to "look_up",
        TestAction.LOOK_DOWN to "look_down",
        TestAction.HEAD_LEFT to "left",
        TestAction.HEAD_RIGHT to "right",
        TestAction.FACE_CENTER to "center",
        TestAction.TURN_SLIGHTLY_LEFT to "left",
        TestAction.TURN_SLIGHTLY_RIGHT to "right",
        TestAction.BLINK_ONCE to "blink",
        TestAction.BLINK_TWICE to "blink_twice",
        TestAction.CLOSE_BOTH_EYES to "blink",
        TestAction.OPEN_BOTH_EYES to "blink",
        TestAction.HOLD_EYES_OPEN to "hold",
        TestAction.OPEN_MOUTH to "mouth",
        TestAction.CLOSE_MOUTH to "mouth",
        TestAction.SMILE to "smile",
        TestAction.NEUTRAL_FACE to "neutral",
        TestAction.RAISE_EYEBROWS to "neutral",
        TestAction.NOD to "nod",
        TestAction.SHAKE_HEAD to "shake",
        TestAction.HOLD_HEAD_STILL to "hold",
        TestAction.MOVE_CLOSER to "closer",
        TestAction.MOVE_FARTHER to "farther",
        TestAction.CENTER_FACE to "center",
        TestAction.HOLD_POSITION to "hold",
    )

    fun clipNameFor(action: TestAction): String =
        clipNameByAction[action] ?: error("No clip mapping for $action")

    fun resolve(context: Context, action: TestAction): ClipResolution {
        val clipName = clipNameFor(action)
        val rawResId = context.resources.getIdentifier(clipName, "raw", context.packageName)
        return ClipResolution(action = action, clipName = clipName, rawResId = rawResId)
    }

    fun resolveByName(context: Context, clipName: String): Int =
        context.resources.getIdentifier(clipName, "raw", context.packageName)
}
