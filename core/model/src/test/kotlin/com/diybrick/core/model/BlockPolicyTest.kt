package com.diybrick.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockPolicyTest {
    private val bricked = BrickState.Bricked(Mode.DEFAULT_ID, since = 0L)
    private val exempt = setOf("com.diybrick", "com.android.launcher3")
    private val blockInstagram = Mode(Mode.DEFAULT_ID, "Focus", ListType.BLOCK, setOf("com.instagram.android"))
    private val allowMaps = Mode(Mode.DEFAULT_ID, "Focus", ListType.ALLOW, setOf("com.google.android.apps.maps"))

    @Test
    fun blocksListedAppWhileBricked() {
        assertTrue(BlockPolicy.shouldBlock("com.instagram.android", bricked, blockInstagram, exempt))
    }

    @Test
    fun nothingIsBlockedWhenFree() {
        assertFalse(BlockPolicy.shouldBlock("com.instagram.android", BrickState.Free, blockInstagram, exempt))
    }

    @Test
    fun unlistedAppIsAllowedInBlockMode() {
        assertFalse(BlockPolicy.shouldBlock("com.google.android.apps.maps", bricked, blockInstagram, exempt))
    }

    @Test
    fun allowModeBlocksEverythingElse() {
        assertTrue(BlockPolicy.shouldBlock("com.instagram.android", bricked, allowMaps, exempt))
        assertFalse(BlockPolicy.shouldBlock("com.google.android.apps.maps", bricked, allowMaps, exempt))
    }

    @Test
    fun exemptAppsAreNeverBlocked() {
        assertFalse(BlockPolicy.shouldBlock("com.diybrick", bricked, allowMaps, exempt))
        assertFalse(BlockPolicy.shouldBlock("com.android.launcher3", bricked, allowMaps, exempt))
        val blockLauncher = blockInstagram.copy(packages = setOf("com.android.launcher3"))
        assertFalse(BlockPolicy.shouldBlock("com.android.launcher3", bricked, blockLauncher, exempt))
    }
}
