package com.rth.kycliveness

import com.rth.kycliveness.data.KycTestProfile
import com.rth.kycliveness.data.ProfileCatalog
import com.rth.kycliveness.data.TestAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileCatalogTest {
    @Test
    fun bkashProfileLoads() {
        val profile = ProfileCatalog.findById("bkash")
        assertNotNull(profile)
        assertEquals("bKash", profile!!.displayName)
        assertEquals(
            listOf(
                TestAction.BLINK_ONCE,
                TestAction.HEAD_LEFT,
                TestAction.HEAD_RIGHT,
                TestAction.NOD,
                TestAction.OPEN_MOUTH,
                TestAction.SMILE,
            ),
            profile.actions,
        )
    }

    @Test
    fun nagadProfileLoads() {
        val profile = ProfileCatalog.findById("nagad")
        assertNotNull(profile)
        assertEquals("Nagad", profile!!.displayName)
        assertTrue(profile.actions.isNotEmpty())
    }

    @Test
    fun binanceProfileLoads() {
        val profile = ProfileCatalog.findById("binance")
        assertNotNull(profile)
        assertEquals("Binance", profile!!.displayName)
        assertTrue(profile.actions.contains(TestAction.SMILE))
    }

    @Test
    fun moreProfileLoads() {
        val rocket = ProfileCatalog.findById("rocket")
        assertNotNull(rocket)
        assertTrue(ProfileCatalog.moreProfiles.any { it.id == "paypal" })
        assertTrue(ProfileCatalog.moreProfiles.any { it.displayName == "KuCoin" })
    }

    @Test
    fun customProfileWorks() {
        val catalogCustom = ProfileCatalog.findById("custom")
        assertNotNull(catalogCustom)

        val constructed = KycTestProfile(
            id = "custom_direct",
            displayName = "Custom Direct",
            actions = listOf(TestAction.BLINK_ONCE, TestAction.NOD),
            timeoutSeconds = 8,
            retryCount = 1,
        )
        assertEquals("custom_direct", constructed.id)
        assertEquals(2, constructed.actions.size)
        assertEquals(8, constructed.timeoutSeconds)
        assertEquals(1, constructed.retryCount)
    }
}
