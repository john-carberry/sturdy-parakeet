package com.livefree.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockPolicyTest {
    private val locked = LockState.Locked(Mode.DEFAULT_ID, since = 0L)
    private val exempt = setOf("com.livefree", "com.android.launcher3")
    private val blockInstagram = Mode(Mode.DEFAULT_ID, "Focus", ListType.BLOCK, setOf("com.instagram.android"))
    private val allowMaps = Mode(Mode.DEFAULT_ID, "Focus", ListType.ALLOW, setOf("com.google.android.apps.maps"))

    @Test
    fun blocksListedAppWhileLocked() {
        assertTrue(BlockPolicy.shouldBlock("com.instagram.android", locked, blockInstagram, exempt))
    }

    @Test
    fun nothingIsBlockedWhenFree() {
        assertFalse(BlockPolicy.shouldBlock("com.instagram.android", LockState.Unlocked, blockInstagram, exempt))
    }

    @Test
    fun unlistedAppIsAllowedInBlockMode() {
        assertFalse(BlockPolicy.shouldBlock("com.google.android.apps.maps", locked, blockInstagram, exempt))
    }

    @Test
    fun allowModeBlocksEverythingElse() {
        assertTrue(BlockPolicy.shouldBlock("com.instagram.android", locked, allowMaps, exempt))
        assertFalse(BlockPolicy.shouldBlock("com.google.android.apps.maps", locked, allowMaps, exempt))
    }

    @Test
    fun exemptAppsAreNeverBlocked() {
        assertFalse(BlockPolicy.shouldBlock("com.livefree", locked, allowMaps, exempt))
        assertFalse(BlockPolicy.shouldBlock("com.android.launcher3", locked, allowMaps, exempt))
        val blockLauncher = blockInstagram.copy(packages = setOf("com.android.launcher3"))
        assertFalse(BlockPolicy.shouldBlock("com.android.launcher3", locked, blockLauncher, exempt))
    }
}
