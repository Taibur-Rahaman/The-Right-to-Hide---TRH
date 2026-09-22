package com.rth.kycliveness.data

data class KycTestProfile(
    val id: String,
    val displayName: String,
    val actions: List<TestAction>,
    val timeoutSeconds: Int = 10,
    val retryCount: Int = 2,
)
