package com.diybrick.core.model

/**
 * The brick state machine (PLAN.md §4.3). Pure rules: callers load the current
 * state, ask what should happen, then persist the outcome.
 *
 * ```
 * FREE --valid key--> BRICKED --valid key--> FREE
 *                        └--request emergency, wait, confirm (if any left)--> FREE
 * ```
 */
object BrickRules {

    sealed interface Outcome {
        data class Brick(val modeId: Long) : Outcome
        data class Unbrick(val reason: EndReason) : Outcome
        data object EmergencyRequested : Outcome
        data class Denied(val reason: DenyReason) : Outcome
    }

    enum class DenyReason {
        NOT_BRICKED,
        NO_EMERGENCY_UNBRICKS_LEFT,
        EMERGENCY_NOT_REQUESTED,
        EMERGENCY_STILL_WAITING,
    }

    /** Where a pending emergency unbrick stands. */
    sealed interface EmergencyStatus {
        data object NotRequested : EmergencyStatus
        data class Waiting(val msLeft: Long) : EmergencyStatus
        /** The wait is over; it can be used for [msLeft] more before it lapses. */
        data class Ready(val msLeft: Long) : EmergencyStatus
    }

    /** A paired key toggles the state (and cancels any pending emergency unbrick). */
    fun onKey(state: BrickState, modeId: Long = Mode.DEFAULT_ID): Outcome = when (state) {
        BrickState.Free -> Outcome.Brick(modeId)
        is BrickState.Bricked -> Outcome.Unbrick(EndReason.KEY)
    }

    /** Starts the wait. Asking again while one is pending keeps the original start time. */
    fun onEmergencyRequest(state: BrickState, remaining: Int): Outcome = when {
        state !is BrickState.Bricked -> Outcome.Denied(DenyReason.NOT_BRICKED)
        remaining <= 0 -> Outcome.Denied(DenyReason.NO_EMERGENCY_UNBRICKS_LEFT)
        else -> Outcome.EmergencyRequested
    }

    fun onEmergencyUnbrick(
        state: BrickState,
        remaining: Int,
        now: Long,
        settings: BrickSettings = BrickSettings(),
    ): Outcome {
        if (state !is BrickState.Bricked) return Outcome.Denied(DenyReason.NOT_BRICKED)
        if (remaining <= 0) return Outcome.Denied(DenyReason.NO_EMERGENCY_UNBRICKS_LEFT)
        return when (emergencyStatus(state.emergencyRequestedAt, now, settings)) {
            EmergencyStatus.NotRequested -> Outcome.Denied(DenyReason.EMERGENCY_NOT_REQUESTED)
            is EmergencyStatus.Waiting -> Outcome.Denied(DenyReason.EMERGENCY_STILL_WAITING)
            is EmergencyStatus.Ready -> Outcome.Unbrick(EndReason.EMERGENCY)
        }
    }

    /**
     * An emergency unbrick has to be waited for, then used within a short window.
     * The window stops you requesting one in advance and keeping it ready.
     */
    fun emergencyStatus(requestedAt: Long?, now: Long, settings: BrickSettings = BrickSettings()): EmergencyStatus {
        if (requestedAt == null) return EmergencyStatus.NotRequested
        val readyAt = requestedAt + settings.emergencyWaitMs
        val lapsesAt = readyAt + settings.emergencyReadyMs
        return when {
            now < readyAt -> EmergencyStatus.Waiting(readyAt - now)
            now < lapsesAt -> EmergencyStatus.Ready(lapsesAt - now)
            else -> EmergencyStatus.NotRequested
        }
    }

    fun emergencyUnbricksRemaining(settings: BrickSettings, used: Int): Int =
        (settings.emergencyUnbricks - used).coerceAtLeast(0)
}
