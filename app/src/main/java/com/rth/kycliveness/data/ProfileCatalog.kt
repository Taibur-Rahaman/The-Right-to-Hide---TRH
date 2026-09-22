package com.rth.kycliveness.data

/**
 * Sandbox-only profile catalog. These sequences are illustrative for QA
 * and do not claim to match any real provider KYC requirements.
 */
object ProfileCatalog {
    val primaryProfiles: List<KycTestProfile> = listOf(
        KycTestProfile(
            id = "bkash",
            displayName = "bKash",
            actions = listOf(
                TestAction.BLINK_ONCE,
                TestAction.HEAD_LEFT,
                TestAction.HEAD_RIGHT,
                TestAction.NOD,
                TestAction.OPEN_MOUTH,
                TestAction.SMILE,
            ),
        ),
        KycTestProfile(
            id = "nagad",
            displayName = "Nagad",
            actions = listOf(
                TestAction.LOOK_STRAIGHT,
                TestAction.BLINK_TWICE,
                TestAction.LOOK_UP,
                TestAction.LOOK_DOWN,
                TestAction.SHAKE_HEAD,
                TestAction.NEUTRAL_FACE,
            ),
        ),
        KycTestProfile(
            id = "binance",
            displayName = "Binance",
            actions = listOf(
                TestAction.FACE_CENTER,
                TestAction.MOVE_CLOSER,
                TestAction.SMILE,
                TestAction.HOLD_HEAD_STILL,
                TestAction.RAISE_EYEBROWS,
                TestAction.CENTER_FACE,
            ),
        ),
    )

    val moreProfiles: List<KycTestProfile> = listOf(
        KycTestProfile(
            id = "rocket",
            displayName = "Rocket",
            actions = listOf(TestAction.BLINK_ONCE, TestAction.NOD, TestAction.SMILE),
        ),
        KycTestProfile(
            id = "upay",
            displayName = "Upay",
            actions = listOf(TestAction.LOOK_STRAIGHT, TestAction.HEAD_LEFT, TestAction.HEAD_RIGHT),
        ),
        KycTestProfile(
            id = "tap",
            displayName = "Tap",
            actions = listOf(TestAction.OPEN_MOUTH, TestAction.CLOSE_MOUTH, TestAction.NEUTRAL_FACE),
        ),
        KycTestProfile(
            id = "ok_wallet",
            displayName = "OK Wallet",
            actions = listOf(TestAction.BLINK_TWICE, TestAction.SHAKE_HEAD, TestAction.HOLD_POSITION),
        ),
        KycTestProfile(
            id = "cellfin",
            displayName = "CellFin",
            actions = listOf(TestAction.LOOK_UP, TestAction.LOOK_DOWN, TestAction.CENTER_FACE),
        ),
        KycTestProfile(
            id = "surecash",
            displayName = "SureCash",
            actions = listOf(TestAction.MOVE_CLOSER, TestAction.MOVE_FARTHER, TestAction.HOLD_HEAD_STILL),
        ),
        KycTestProfile(
            id = "payoneer",
            displayName = "Payoneer",
            actions = listOf(TestAction.SMILE, TestAction.RAISE_EYEBROWS, TestAction.BLINK_ONCE),
        ),
        KycTestProfile(
            id = "skrill",
            displayName = "Skrill",
            actions = listOf(TestAction.TURN_SLIGHTLY_LEFT, TestAction.TURN_SLIGHTLY_RIGHT, TestAction.FACE_CENTER),
        ),
        KycTestProfile(
            id = "wise",
            displayName = "Wise",
            actions = listOf(TestAction.CLOSE_BOTH_EYES, TestAction.OPEN_BOTH_EYES, TestAction.HOLD_EYES_OPEN),
        ),
        KycTestProfile(
            id = "paypal",
            displayName = "PayPal",
            actions = listOf(TestAction.NOD, TestAction.SHAKE_HEAD, TestAction.SMILE),
        ),
        KycTestProfile(
            id = "coinbase",
            displayName = "Coinbase",
            actions = listOf(TestAction.LOOK_STRAIGHT, TestAction.BLINK_ONCE, TestAction.OPEN_MOUTH),
        ),
        KycTestProfile(
            id = "bybit",
            displayName = "Bybit",
            actions = listOf(TestAction.HEAD_LEFT, TestAction.HEAD_RIGHT, TestAction.NOD),
        ),
        KycTestProfile(
            id = "okx",
            displayName = "OKX",
            actions = listOf(TestAction.FACE_CENTER, TestAction.SMILE, TestAction.HOLD_POSITION),
        ),
        KycTestProfile(
            id = "kucoin",
            displayName = "KuCoin",
            actions = listOf(TestAction.BLINK_TWICE, TestAction.LOOK_UP, TestAction.LOOK_DOWN),
        ),
        KycTestProfile(
            id = "custom",
            displayName = "Custom",
            actions = listOf(
                TestAction.BLINK_ONCE,
                TestAction.SMILE,
                TestAction.NOD,
                TestAction.CENTER_FACE,
            ),
        ),
    )

    val allProfiles: List<KycTestProfile> = primaryProfiles + moreProfiles

    fun findById(id: String): KycTestProfile? = allProfiles.find { it.id == id }

    fun defaultProfile(): KycTestProfile = primaryProfiles.first()
}
