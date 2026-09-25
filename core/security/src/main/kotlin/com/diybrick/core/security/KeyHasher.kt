package com.diybrick.core.security

import java.security.MessageDigest
import java.security.SecureRandom

/** Hashes key secrets (NFC UIDs, NDEF secrets, QR secrets) so raw values are never stored. */
class KeyHasher(private val salt: ByteArray) {

    init {
        require(salt.size >= MIN_SALT_BYTES) { "salt must be at least $MIN_SALT_BYTES bytes" }
    }

    fun hash(secret: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(secret)
        return digest.digest().toHex()
    }

    /** Constant-time comparison of a scanned secret against a stored hash. */
    fun matches(secret: ByteArray, storedHash: String): Boolean =
        MessageDigest.isEqual(hash(secret).toByteArray(), storedHash.toByteArray())

    companion object {
        const val MIN_SALT_BYTES = 16

        fun newSalt(random: SecureRandom = SecureRandom()): ByteArray =
            ByteArray(32).also(random::nextBytes)
    }
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
