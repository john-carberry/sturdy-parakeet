package com.diybrick.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyHasherTest {
    private val uid = byteArrayOf(0x04, 0x1A, 0x2B, 0x3C, 0x4D, 0x5E, 0x6F)

    @Test
    fun sameSecretMatches() {
        val hasher = KeyHasher(KeyHasher.newSalt())
        val stored = hasher.hash(uid)
        assertTrue(hasher.matches(uid, stored))
    }

    @Test
    fun differentSecretDoesNotMatch() {
        val hasher = KeyHasher(KeyHasher.newSalt())
        val stored = hasher.hash(uid)
        assertFalse(hasher.matches(byteArrayOf(0x04, 0x00), stored))
    }

    @Test
    fun saltChangesHash() {
        assertNotEquals(
            KeyHasher(KeyHasher.newSalt()).hash(uid),
            KeyHasher(KeyHasher.newSalt()).hash(uid),
        )
    }

    @Test
    fun hashIsHexSha256() {
        assertEquals(64, KeyHasher(ByteArray(16)).hash(uid).length)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsShortSalt() {
        KeyHasher(ByteArray(4))
    }
}
