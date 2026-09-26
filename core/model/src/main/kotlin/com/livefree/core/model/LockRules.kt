package com.livefree.core.model

/**
 * The lock state machine (PLAN.md §4.3). Pure rules: callers load the current
 * state, ask what should happen, then persist the outcome.
 *
 * ```
 * UNLOCKED --valid key--> LOCKED --valid key--> UNLOCKED
 *                        └--request emergency, wait, confirm (if any left)--> FREE
 * ```
 */
object LockRules {

    sealed interface Outcome {
        data class Lock(val modeId: Long) : Outcome
        data class Unlock(val reason: EndReason) : Outcome
        data object EmergencyRequested : Outcome
        data class Denied(val reason: DenyReason) : Outcome
    }

    enum class DenyReason {
        NOT_LOCKED,
        NO_EMERGENCY_UNLOCKS_LEFT,
        EMERGENCY_NOT_REQUESTED,
        EMERGENCY_STILL_WAITING,
    }

    /** Where a pending emergency unlock stands. */
    sealed interface EmergencyStatus {
        data object NotRequested : EmergencyStatus
        data class Waiting(val msLeft: Long) : EmergencyStatus
        /** The wait is over; it can be used for [msLeft] more before it lapses. */
        data class Ready(val msLeft: Long) : EmergencyStatus
    }

    /** A paired key toggles the state (and cancels any pending emergency unlock). */
    fun onKey(state: LockState, modeId: Long = Mode.DEFAULT_ID): Outcome = when (state) {
        LockState.Unlocked -> Outcome.Lock(modeId)
        is LockState.Locked -> Outcome.Unlock(EndReason.KEY)
    }

    /** Starts the wait. Asking again while one is pending keeps the original start time. */
    fun onEmergencyRequest(state: LockState, remaining: Int): Outcome = when {
        state !is LockState.Locked -> Outcome.Denied(DenyReason.NOT_LOCKED)
        remaining <= 0 -> Outcome.Denied(DenyReason.NO_EMERGENCY_UNLOCKS_LEFT)
        else -> Outcome.EmergencyRequested
    }

    fun onEmergencyUnlock(
        state: LockState,
        remaining: Int,
        now: Long,
        settings: LockSettings = LockSettings(),
    ): Outcome {
        if (state !is LockState.Locked) return Outcome.Denied(DenyReason.NOT_LOCKED)
        if (remaining <= 0) return Outcome.Denied(DenyReason.NO_EMERGENCY_UNLOCKS_LEFT)
        return when (emergencyStatus(state.emergencyRequestedAt, now, settings)) {
            EmergencyStatus.NotRequested -> Outcome.Denied(DenyReason.EMERGENCY_NOT_REQUESTED)
            is EmergencyStatus.Waiting -> Outcome.Denied(DenyReason.EMERGENCY_STILL_WAITING)
            is EmergencyStatus.Ready -> Outcome.Unlock(EndReason.EMERGENCY)
        }
    }

    /**
     * An emergency unlock has to be waited for, then used within a short window.
     * The window stops you requesting one in advance and keeping it ready.
     */
    fun emergencyStatus(requestedAt: Long?, now: Long, settings: LockSettings = LockSettings()): EmergencyStatus {
        if (requestedAt == null) return EmergencyStatus.NotRequested
        val readyAt = requestedAt + settings.emergencyWaitMs
        val lapsesAt = readyAt + settings.emergencyReadyMs
        return when {
            now < readyAt -> EmergencyStatus.Waiting(readyAt - now)
            now < lapsesAt -> EmergencyStatus.Ready(lapsesAt - now)
            else -> EmergencyStatus.NotRequested
        }
    }

    fun emergencyUnlocksRemaining(settings: LockSettings, used: Int): Int =
        (settings.emergencyUnlocks - used).coerceAtLeast(0)
}
