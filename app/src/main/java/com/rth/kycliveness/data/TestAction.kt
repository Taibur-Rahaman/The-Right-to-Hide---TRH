package com.rth.kycliveness.data

enum class TestAction {
    LOOK_STRAIGHT,
    LOOK_UP,
    LOOK_DOWN,
    HEAD_LEFT,
    HEAD_RIGHT,
    FACE_CENTER,
    TURN_SLIGHTLY_LEFT,
    TURN_SLIGHTLY_RIGHT,

    BLINK_ONCE,
    BLINK_TWICE,
    CLOSE_BOTH_EYES,
    OPEN_BOTH_EYES,
    HOLD_EYES_OPEN,

    OPEN_MOUTH,
    CLOSE_MOUTH,
    SMILE,
    NEUTRAL_FACE,
    RAISE_EYEBROWS,

    NOD,
    SHAKE_HEAD,
    HOLD_HEAD_STILL,

    MOVE_CLOSER,
    MOVE_FARTHER,
    CENTER_FACE,
    HOLD_POSITION;

    fun displayLabel(): String = when (this) {
        LOOK_STRAIGHT -> "Look Straight"
        LOOK_UP -> "Look Up"
        LOOK_DOWN -> "Look Down"
        HEAD_LEFT -> "Head Left"
        HEAD_RIGHT -> "Head Right"
        FACE_CENTER -> "Face Center"
        TURN_SLIGHTLY_LEFT -> "Turn Slightly Left"
        TURN_SLIGHTLY_RIGHT -> "Turn Slightly Right"
        BLINK_ONCE -> "Blink Once"
        BLINK_TWICE -> "Blink Twice"
        CLOSE_BOTH_EYES -> "Close Both Eyes"
        OPEN_BOTH_EYES -> "Open Both Eyes"
        HOLD_EYES_OPEN -> "Hold Eyes Open"
        OPEN_MOUTH -> "Open Mouth"
        CLOSE_MOUTH -> "Close Mouth"
        SMILE -> "Smile"
        NEUTRAL_FACE -> "Neutral Face"
        RAISE_EYEBROWS -> "Raise Eyebrows"
        NOD -> "Nod"
        SHAKE_HEAD -> "Shake Head"
        HOLD_HEAD_STILL -> "Hold Head Still"
        MOVE_CLOSER -> "Move Closer"
        MOVE_FARTHER -> "Move Farther"
        CENTER_FACE -> "Center Face"
        HOLD_POSITION -> "Hold Position"
    }
}
