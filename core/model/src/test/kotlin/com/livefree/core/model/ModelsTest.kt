package com.livefree.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {
    @Test
    fun blockListBlocksOnlyListedApps() {
        val mode = Mode(1, "Work", ListType.BLOCK, setOf("com.instagram.android"))
        assertTrue(mode.isBlocked("com.instagram.android"))
        assertFalse(mode.isBlocked("com.google.android.dialer"))
    }

    @Test
    fun allowListBlocksEverythingElse() {
        val mode = Mode(2, "Sleep", ListType.ALLOW, setOf("com.google.android.dialer"))
        assertFalse(mode.isBlocked("com.google.android.dialer"))
        assertTrue(mode.isBlocked("com.instagram.android"))
    }

    @Test
    fun defaultSettingsMatchPlan() {
        val s = LockSettings()
        assertTrue(s.anyKeyUnlocksAllModes)
        assertEquals(5, s.emergencyUnlocks)
        assertEquals(EmergencyRefill.NEVER, s.emergencyRefill)
        assertFalse(s.websiteBlocking)
        assertFalse(s.strictMode)
    }
}
