package com.livefree.core.model

/** How a physical key identifies itself. See PLAN.md §2. */
enum class KeyType { NFC_UID, NFC_NDEF, QR }

/** A paired key. Only a salted hash of its secret is ever stored. */
data class Key(
    val id: Long,
    val type: KeyType,
    val label: String,
    val secretHash: String,
    val createdAt: Long,
)

enum class ListType { BLOCK, ALLOW }

/** A named set of app rules, e.g. "Work" or "Sleep". */
data class Mode(
    val id: Long,
    val name: String,
    val listType: ListType,
    val packages: Set<String>,
) {
    fun isBlocked(packageName: String): Boolean = when (listType) {
        ListType.BLOCK -> packageName in packages
        ListType.ALLOW -> packageName !in packages
    }

    companion object {
        /** The single mode used until mode editing arrives in M3. */
        const val DEFAULT_ID = 1L
    }
}

enum class EndReason { KEY, EMERGENCY, SCHEDULE }

data class Session(
    val id: Long,
    val modeId: Long,
    val startedAt: Long,
    val endedAt: Long? = null,
    val endReason: EndReason? = null,
)

sealed interface LockState {
    data object Unlocked : LockState
    data class Locked(
        val modeId: Long,
        val since: Long,
        /** When an emergency unlock was requested, if one is pending. */
        val emergencyRequestedAt: Long? = null,
    ) : LockState
}
