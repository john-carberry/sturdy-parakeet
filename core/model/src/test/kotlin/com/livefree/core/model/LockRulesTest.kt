package com.livefree.core.model

import com.livefree.core.model.LockRules.DenyReason
import com.livefree.core.model.LockRules.EmergencyStatus
import com.livefree.core.model.LockRules.Outcome
import org.junit.Assert.assertEquals
import org.junit.Test

class LockRulesTest {
    private val settings = LockSettings(emergencyWaitMinutes = 10, emergencyReadyMinutes = 5)
    private val locked = LockState.Locked(modeId = Mode.DEFAULT_ID, since = 1000L)
    private val min = 60_000L

    @Test
    fun keyLocksWhenUnlocked() {
        assertEquals(Outcome.Lock(Mode.DEFAULT_ID), LockRules.onKey(LockState.Unlocked))
    }

    @Test
    fun keyUnlocksWhenLocked() {
        assertEquals(Outcome.Unlock(EndReason.KEY), LockRules.onKey(locked))
    }

    @Test
    fun emergencyRequestNeedsLockAndAllowance() {
        assertEquals(Outcome.EmergencyRequested, LockRules.onEmergencyRequest(locked, 1))
        assertEquals(Outcome.Denied(DenyReason.NOT_LOCKED), LockRules.onEmergencyRequest(LockState.Unlocked, 5))
        assertEquals(
            Outcome.Denied(DenyReason.NO_EMERGENCY_UNLOCKS_LEFT),
            LockRules.onEmergencyRequest(locked, 0),
        )
    }

    @Test
    fun emergencyUnlockNeedsARequest() {
        assertEquals(
            Outcome.Denied(DenyReason.EMERGENCY_NOT_REQUESTED),
            LockRules.onEmergencyUnlock(locked, 5, now = 0L, settings),
        )
    }

    @Test
    fun emergencyUnlockWaitsThenWorks() {
        val requested = locked.copy(emergencyRequestedAt = 0L)
        assertEquals(
            Outcome.Denied(DenyReason.EMERGENCY_STILL_WAITING),
            LockRules.onEmergencyUnlock(requested, 5, now = 9 * min, settings),
        )
        assertEquals(
            Outcome.Unlock(EndReason.EMERGENCY),
            LockRules.onEmergencyUnlock(requested, 5, now = 10 * min, settings),
        )
    }

    @Test
    fun readyEmergencyLapsesSoItCantBeArmedInAdvance() {
        val requested = locked.copy(emergencyRequestedAt = 0L)
        assertEquals(
            Outcome.Denied(DenyReason.EMERGENCY_NOT_REQUESTED),
            LockRules.onEmergencyUnlock(requested, 5, now = 15 * min, settings),
        )
    }

    @Test
    fun emergencyDeniedWhenNoneLeft() {
        val requested = locked.copy(emergencyRequestedAt = 0L)
        assertEquals(
            Outcome.Denied(DenyReason.NO_EMERGENCY_UNLOCKS_LEFT),
            LockRules.onEmergencyUnlock(requested, 0, now = 11 * min, settings),
        )
    }

    @Test
    fun emergencyStatusCountsDown() {
        assertEquals(EmergencyStatus.NotRequested, LockRules.emergencyStatus(null, 0L, settings))
        assertEquals(EmergencyStatus.Waiting(4 * min), LockRules.emergencyStatus(0L, 6 * min, settings))
        assertEquals(EmergencyStatus.Ready(3 * min), LockRules.emergencyStatus(0L, 12 * min, settings))
        assertEquals(EmergencyStatus.NotRequested, LockRules.emergencyStatus(0L, 15 * min, settings))
    }

    @Test
    fun remainingNeverGoesNegative() {
        assertEquals(5, LockRules.emergencyUnlocksRemaining(settings, 0))
        assertEquals(2, LockRules.emergencyUnlocksRemaining(settings, 3))
        assertEquals(0, LockRules.emergencyUnlocksRemaining(settings, 9))
    }
}
