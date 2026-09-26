package com.livefree.core.model

enum class EmergencyRefill { NEVER, MONTHLY }

/**
 * User settings. The defaults are the v1 answers to the open questions in PLAN.md §8.
 */
data class LockSettings(
    /** Any paired key unlocks every mode (keys aren't bound to specific modes). */
    val anyKeyUnlocksAllModes: Boolean = true,
    /** How many emergency unlocks you get in total (they never refill by default). */
    val emergencyUnlocks: Int = DEFAULT_EMERGENCY_UNLOCKS,
    val emergencyRefill: EmergencyRefill = EmergencyRefill.NEVER,
    /** How long to wait after asking for an emergency unlock before it can be used. */
    val emergencyWaitMinutes: Int = 10,
    /** How long an emergency unlock stays usable once the wait is over. */
    val emergencyReadyMinutes: Int = 5,
    /** VPN-based website blocking; out of scope for v1. */
    val websiteBlocking: Boolean = false,
    /** Device Owner strict mode (needs one-time adb setup); deferred to v2. */
    val strictMode: Boolean = false,
) {
    val emergencyWaitMs: Long get() = emergencyWaitMinutes * 60_000L
    val emergencyReadyMs: Long get() = emergencyReadyMinutes * 60_000L

    companion object {
        const val DEFAULT_EMERGENCY_UNLOCKS = 5
    }
}
