package com.livefree.feature.blocker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageThrottleTest {
    private var now = 10_000L
    private val throttle = MessageThrottle(clock = { now }, quietMs = 4_000L)

    @Test
    fun repeatsForSameAppAreQuietedForAWhile() {
        assertTrue(throttle.shouldShow("com.instagram.android"))
        now += 500
        assertFalse(throttle.shouldShow("com.instagram.android"))
        now += 4_000
        assertTrue(throttle.shouldShow("com.instagram.android"))
    }

    @Test
    fun differentAppShowsStraightAway() {
        assertTrue(throttle.shouldShow("com.instagram.android"))
        now += 100
        assertTrue(throttle.shouldShow("com.zhiliaoapp.musically"))
    }
}
