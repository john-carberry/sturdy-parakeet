package com.diybrick.core.security

import java.security.SecureRandom
import java.util.Base64

/**
 * The text encoded in a printed QR key: `dbrick://key/v1/<base64url secret>`.
 * See PLAN.md §2.2.
 */
object QrKeyPayload {
    const val PREFIX = "dbrick://key/v1/"
    const val SECRET_BYTES = 32

    fun newSecret(random: SecureRandom = SecureRandom()): ByteArray =
        ByteArray(SECRET_BYTES).also(random::nextBytes)

    fun encode(secret: ByteArray): String {
        require(secret.size == SECRET_BYTES) { "secret must be $SECRET_BYTES bytes" }
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(secret)
    }

    /** Returns the secret, or null if [text] isn't a DIY Brick key. */
    fun decode(text: String): ByteArray? {
        if (!text.startsWith(PREFIX)) return null
        val secret = try {
            Base64.getUrlDecoder().decode(text.removePrefix(PREFIX))
        } catch (e: IllegalArgumentException) {
            return null
        }
        return secret.takeIf { it.size == SECRET_BYTES }
    }
}
