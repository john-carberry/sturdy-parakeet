package com.livefree.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockPolicyTest {
    private val locked = LockState.Locked(Mode.DEFAULT_ID, since = 0L)
    private val exempt = setOf("com.livefree", "com.android.launcher3")
    private val blockInstagram = Mode(Mode.DEFAULT_ID, "Focus", ListType.BLOCK, setOf("com.instagram.android"))
    private val allowMaps = Mode(Mode.DEFAULT_ID, "Focus", ListType.ALLOW, setOf("com.google.android.apps.maps"))

    private fun blocks(
        packageName: String,
        mode: Mode,
        state: LockState = locked,
        screenLocked: Boolean = false,
    ) = BlockPolicy.shouldBlock(packageName, state, mode, exempt, screenLocked)

    @Test
    fun blocksListedAppWhileLocked() {
        assertTrue(blocks("com.instagram.android", blockInstagram))
    }

    @Test
    fun nothingIsBlockedWhileThePhonesScreenLockIsShowing() {
        // Even a blocked app, and even in allow-list mode: the lock screen is off limits.
        assertFalse(blocks("com.instagram.android", blockInstagram, screenLocked = true))
        assertFalse(blocks("com.example.biometrics", allowMaps, screenLocked = true))
    }

    @Test
    fun nothingIsBlockedWhenUnlocked() {
        assertFalse(blocks("com.instagram.android", blockInstagram, state = LockState.Unlocked))
    }

    @Test
    fun unlistedAppIsAllowedInBlockMode() {
        assertFalse(blocks("com.google.android.apps.maps", blockInstagram))
    }

    @Test
    fun allowModeBlocksEverythingElse() {
        assertTrue(blocks("com.instagram.android", allowMaps))
        assertFalse(blocks("com.google.android.apps.maps", allowMaps))
    }

    @Test
    fun exemptAppsAreNeverBlocked() {
        assertFalse(blocks("com.livefree", allowMaps))
        assertFalse(blocks("com.android.launcher3", allowMaps))
        assertFalse(blocks("com.android.launcher3", blockInstagram.copy(packages = setOf("com.android.launcher3"))))
    }
}
