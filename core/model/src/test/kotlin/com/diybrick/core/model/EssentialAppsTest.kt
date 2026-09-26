package com.diybrick.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EssentialAppsTest {
    @Test
    fun sanitizeRemovesEssentialApps() {
        val packages = setOf("com.instagram.android", "com.android.settings")
        val mode = Mode(Mode.DEFAULT_ID, "Focus", ListType.BLOCK, packages)
        val clean = EssentialApps.sanitize(mode, EssentialApps.KNOWN.keys)
        assertEquals(setOf("com.instagram.android"), clean.packages)
    }

    @Test
    fun coreFunctionsAreKnownEssentials() {
        val reasons = EssentialApps.KNOWN.values.toSet()
        listOf(
            EssentialReason.PHONE,
            EssentialReason.MESSAGES,
            EssentialReason.CONTACTS,
            EssentialReason.ALARMS,
            EssentialReason.SETTINGS,
            EssentialReason.EMERGENCY,
        ).forEach { assertTrue("$it", it in reasons) }
    }

    @Test
    fun essentialAppsAreNeverBlockedEvenInAllowMode() {
        val allowNothing = Mode(Mode.DEFAULT_ID, "Focus", ListType.ALLOW, emptySet())
        val bricked = BrickState.Bricked(Mode.DEFAULT_ID, since = 0L)
        EssentialApps.KNOWN.keys.forEach {
            assertFalse(it, BlockPolicy.shouldBlock(it, bricked, allowNothing, EssentialApps.KNOWN.keys))
        }
    }
}
