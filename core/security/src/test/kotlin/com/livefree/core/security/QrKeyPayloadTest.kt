package com.livefree.core.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrKeyPayloadTest {
    @Test
    fun roundTrips() {
        val secret = QrKeyPayload.newSecret()
        val text = QrKeyPayload.encode(secret)
        assertTrue(text.startsWith("livefree://key/v1/"))
        assertArrayEquals(secret, QrKeyPayload.decode(text))
    }

    @Test
    fun rejectsOtherQrCodes() {
        assertNull(QrKeyPayload.decode("https://example.com"))
        assertNull(QrKeyPayload.decode("livefree://key/v2/abc"))
    }

    @Test
    fun rejectsBadBase64() {
        assertNull(QrKeyPayload.decode("livefree://key/v1/not base64!"))
    }

    @Test
    fun rejectsWrongLength() {
        assertNull(QrKeyPayload.decode("livefree://key/v1/AAAA"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun encodeRejectsShortSecret() {
        QrKeyPayload.encode(ByteArray(8))
    }
}
