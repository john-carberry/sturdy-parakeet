package com.diybrick.core.model

/**
 * The brick state machine (PLAN.md §4.3). Pure rules: callers load the current
 * state, ask what should happen, then persist the outcome.
 *
 * ```
 * FREE --valid key--> BRICKED --valid key--> FREE
 *                        └--emergency unbrick (if any left)--> FREE
 * ```
 */
object BrickRules {

    sealed interface Outcome {
        data class Brick(val modeId: Long) : Outcome
        data class Unbrick(val reason: EndReason) : Outcome
        data class Denied(val reason: DenyReason) : Outcome
    }

    enum class DenyReason { NOT_BRICKED, NO_EMERGENCY_UNBRICKS_LEFT }

    /** A paired key toggles the state. */
    fun onKey(state: BrickState, modeId: Long = Mode.DEFAULT_ID): Outcome = when (state) {
        BrickState.Free -> Outcome.Brick(modeId)
        is BrickState.Bricked -> Outcome.Unbrick(EndReason.KEY)
    }

    fun onEmergencyUnbrick(state: BrickState, remaining: Int): Outcome = when {
        state !is BrickState.Bricked -> Outcome.Denied(DenyReason.NOT_BRICKED)
        remaining <= 0 -> Outcome.Denied(DenyReason.NO_EMERGENCY_UNBRICKS_LEFT)
        else -> Outcome.Unbrick(EndReason.EMERGENCY)
    }

    fun emergencyUnbricksRemaining(settings: BrickSettings, used: Int): Int =
        (settings.emergencyUnbricks - used).coerceAtLeast(0)
}
