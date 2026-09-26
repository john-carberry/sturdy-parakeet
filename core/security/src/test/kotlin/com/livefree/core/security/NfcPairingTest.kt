package com.livefree.core.security

import com.livefree.core.security.NfcPairing.RejectReason
import com.livefree.core.security.NfcPairing.State
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcPairingTest {
    private val card = byteArrayOf(0x04, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66)
    private val otherCard = byteArrayOf(0x04, 0x77, 0x22, 0x33, 0x44, 0x55, 0x66)
    private val randomUid = byteArrayOf(0x08, 0x12, 0x34, 0x56)

    @Test
    fun sameCardTwiceIsConfirmed() {
        val pairing = NfcPairing()
        assertTrue(pairing.onTap(card) is State.WaitingForSecondTap)
        val state = pairing.onTap(card)
        assertTrue(state is State.Confirmed)
        assertArrayEquals(card, (state as State.Confirmed).uid)
    }

    @Test
    fun changingUidIsRejected() {
        val pairing = NfcPairing()
        pairing.onTap(card)
        assertEquals(State.Rejected(RejectReason.UID_CHANGED), pairing.onTap(otherCard))
    }

    @Test
    fun randomUidIsRejectedOnFirstTap() {
        assertEquals(State.Rejected(RejectReason.RANDOM_UID), NfcPairing().onTap(randomUid))
    }

    @Test
    fun emptyUidIsRejected() {
        assertEquals(State.Rejected(RejectReason.EMPTY_UID), NfcPairing().onTap(ByteArray(0)))
    }

    @Test
    fun tapAfterRejectionStartsOver() {
        val pairing = NfcPairing()
        pairing.onTap(randomUid)
        assertTrue(pairing.onTap(card) is State.WaitingForSecondTap)
    }

    @Test
    fun confirmedIgnoresFurtherTaps() {
        val pairing = NfcPairing()
        pairing.onTap(card)
        pairing.onTap(card)
        assertTrue(pairing.onTap(otherCard) is State.Confirmed)
    }

    @Test
    fun resetStartsOver() {
        val pairing = NfcPairing()
        pairing.onTap(card)
        pairing.reset()
        assertEquals(State.WaitingForFirstTap, pairing.state)
    }

    @Test
    fun randomUidPolicy() {
        assertTrue(NfcUidPolicy.isRandomUid(randomUid))
        assertFalse(NfcUidPolicy.isRandomUid(byteArrayOf(0x08, 1, 2, 3, 4, 5, 6)))
        assertFalse(NfcUidPolicy.isRandomUid(byteArrayOf(0x04, 1, 2, 3)))
    }
}
