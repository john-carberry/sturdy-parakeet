package com.diybrick.core.model

enum class EmergencyRefill { NEVER, MONTHLY }

/**
 * User settings. The defaults are the v1 answers to the open questions in PLAN.md §8.
 */
data class BrickSettings(
    /** Any paired key unlocks every mode (keys aren't bound to specific modes). */
    val anyKeyUnlocksAllModes: Boolean = true,
    val emergencyUnbricksRemaining: Int = DEFAULT_EMERGENCY_UNBRICKS,
    val emergencyRefill: EmergencyRefill = EmergencyRefill.NEVER,
    /** VPN-based website blocking; out of scope for v1. */
    val websiteBlocking: Boolean = false,
    /** Device Owner strict mode (needs one-time adb setup); deferred to v2. */
    val strictMode: Boolean = false,
) {
    companion object {
        const val DEFAULT_EMERGENCY_UNBRICKS = 5
    }
}
