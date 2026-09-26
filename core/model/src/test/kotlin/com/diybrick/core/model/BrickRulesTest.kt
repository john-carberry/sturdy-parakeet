package com.diybrick.core.model

import com.diybrick.core.model.BrickRules.DenyReason
import com.diybrick.core.model.BrickRules.Outcome
import org.junit.Assert.assertEquals
import org.junit.Test

class BrickRulesTest {
    private val bricked = BrickState.Bricked(modeId = Mode.DEFAULT_ID, since = 1000L)

    @Test
    fun keyBricksWhenFree() {
        assertEquals(Outcome.Brick(Mode.DEFAULT_ID), BrickRules.onKey(BrickState.Free))
    }

    @Test
    fun keyUnbricksWhenBricked() {
        assertEquals(Outcome.Unbrick(EndReason.KEY), BrickRules.onKey(bricked))
    }

    @Test
    fun emergencyUnbricksWhenSomeLeft() {
        assertEquals(Outcome.Unbrick(EndReason.EMERGENCY), BrickRules.onEmergencyUnbrick(bricked, 1))
    }

    @Test
    fun emergencyDeniedWhenNoneLeft() {
        assertEquals(
            Outcome.Denied(DenyReason.NO_EMERGENCY_UNBRICKS_LEFT),
            BrickRules.onEmergencyUnbrick(bricked, 0),
        )
    }

    @Test
    fun emergencyDeniedWhenFree() {
        assertEquals(
            Outcome.Denied(DenyReason.NOT_BRICKED),
            BrickRules.onEmergencyUnbrick(BrickState.Free, 5),
        )
    }

    @Test
    fun remainingNeverGoesNegative() {
        val settings = BrickSettings()
        assertEquals(5, BrickRules.emergencyUnbricksRemaining(settings, 0))
        assertEquals(2, BrickRules.emergencyUnbricksRemaining(settings, 3))
        assertEquals(0, BrickRules.emergencyUnbricksRemaining(settings, 9))
    }
}
