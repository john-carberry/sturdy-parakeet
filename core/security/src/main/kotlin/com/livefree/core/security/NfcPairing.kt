package com.livefree.core.security

/**
 * Two-tap pairing for NFC cards. A card is accepted only if it shows the same UID
 * on both taps, which rules out bank cards and phone wallets (they use a new random
 * UID each time). See PLAN.md §2.1.
 */
class NfcPairing {

    sealed interface State {
        data object WaitingForFirstTap : State
        class WaitingForSecondTap(val firstUid: ByteArray) : State
        class Confirmed(val uid: ByteArray) : State
        data class Rejected(val reason: RejectReason) : State
    }

    enum class RejectReason { RANDOM_UID, UID_CHANGED, EMPTY_UID }

    var state: State = State.WaitingForFirstTap
        private set

    fun onTap(uid: ByteArray): State {
        state = when (val current = state) {
            is State.Confirmed -> current
            is State.WaitingForSecondTap -> when {
                uid.contentEquals(current.firstUid) -> State.Confirmed(uid.copyOf())
                else -> State.Rejected(RejectReason.UID_CHANGED)
            }
            // A tap after a rejection starts over with the new card.
            State.WaitingForFirstTap, is State.Rejected -> firstTap(uid)
        }
        return state
    }

    fun reset() {
        state = State.WaitingForFirstTap
    }

    private fun firstTap(uid: ByteArray): State = when {
        uid.isEmpty() -> State.Rejected(RejectReason.EMPTY_UID)
        NfcUidPolicy.isRandomUid(uid) -> State.Rejected(RejectReason.RANDOM_UID)
        else -> State.WaitingForSecondTap(uid.copyOf())
    }
}

object NfcUidPolicy {
    private const val RANDOM_UID_PREFIX: Byte = 0x08

    /**
     * ISO/IEC 14443-3: a 4-byte UID that starts with 0x08 is a random ID that the
     * card regenerates every time it powers up, so it can't identify the card.
     */
    fun isRandomUid(uid: ByteArray): Boolean = uid.size == 4 && uid[0] == RANDOM_UID_PREFIX
}
