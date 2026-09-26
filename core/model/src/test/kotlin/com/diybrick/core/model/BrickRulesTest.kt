package com.diybrick.core.model

import com.diybrick.core.model.BrickRules.DenyReason
import com.diybrick.core.model.BrickRules.EmergencyStatus
import com.diybrick.core.model.BrickRules.Outcome
import org.junit.Assert.assertEquals
import org.junit.Test

class BrickRulesTest {
    private val settings = BrickSettings(emergencyWaitMinutes = 10, emergencyReadyMinutes = 5)
    private val bricked = BrickState.Bricked(modeId = Mode.DEFAULT_ID, since = 1000L)
    private val min = 60_000L

    @Test
    fun keyBricksWhenFree() {
        assertEquals(Outcome.Brick(Mode.DEFAULT_ID), BrickRules.onKey(BrickState.Free))
    }

    @Test
    fun keyUnbricksWhenBricked() {
        assertEquals(Outcome.Unbrick(EndReason.KEY), BrickRules.onKey(bricked))
    }

    @Test
    fun emergencyRequestNeedsBrickAndAllowance() {
        assertEquals(Outcome.EmergencyRequested, BrickRules.onEmergencyRequest(bricked, 1))
        assertEquals(Outcome.Denied(DenyReason.NOT_BRICKED), BrickRules.onEmergencyRequest(BrickState.Free, 5))
        assertEquals(
            Outcome.Denied(DenyReason.NO_EMERGENCY_UNBRICKS_LEFT),
            BrickRules.onEmergencyRequest(bricked, 0),
        )
    }

    @Test
    fun emergencyUnbrickNeedsARequest() {
        assertEquals(
            Outcome.Denied(DenyReason.EMERGENCY_NOT_REQUESTED),
            BrickRules.onEmergencyUnbrick(bricked, 5, now = 0L, settings),
        )
    }

    @Test
    fun emergencyUnbrickWaitsThenWorks() {
        val requested = bricked.copy(emergencyRequestedAt = 0L)
        assertEquals(
            Outcome.Denied(DenyReason.EMERGENCY_STILL_WAITING),
            BrickRules.onEmergencyUnbrick(requested, 5, now = 9 * min, settings),
        )
        assertEquals(
            Outcome.Unbrick(EndReason.EMERGENCY),
            BrickRules.onEmergencyUnbrick(requested, 5, now = 10 * min, settings),
        )
    }

    @Test
    fun readyEmergencyLapsesSoItCantBeArmedInAdvance() {
        val requested = bricked.copy(emergencyRequestedAt = 0L)
        assertEquals(
            Outcome.Denied(DenyReason.EMERGENCY_NOT_REQUESTED),
            BrickRules.onEmergencyUnbrick(requested, 5, now = 15 * min, settings),
        )
    }

    @Test
    fun emergencyDeniedWhenNoneLeft() {
        val requested = bricked.copy(emergencyRequestedAt = 0L)
        assertEquals(
            Outcome.Denied(DenyReason.NO_EMERGENCY_UNBRICKS_LEFT),
            BrickRules.onEmergencyUnbrick(requested, 0, now = 11 * min, settings),
        )
    }

    @Test
    fun emergencyStatusCountsDown() {
        assertEquals(EmergencyStatus.NotRequested, BrickRules.emergencyStatus(null, 0L, settings))
        assertEquals(EmergencyStatus.Waiting(4 * min), BrickRules.emergencyStatus(0L, 6 * min, settings))
        assertEquals(EmergencyStatus.Ready(3 * min), BrickRules.emergencyStatus(0L, 12 * min, settings))
        assertEquals(EmergencyStatus.NotRequested, BrickRules.emergencyStatus(0L, 15 * min, settings))
    }

    @Test
    fun remainingNeverGoesNegative() {
        assertEquals(5, BrickRules.emergencyUnbricksRemaining(settings, 0))
        assertEquals(2, BrickRules.emergencyUnbricksRemaining(settings, 3))
        assertEquals(0, BrickRules.emergencyUnbricksRemaining(settings, 9))
    }
}
