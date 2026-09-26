package com.livefree.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StatsCalculatorTest {
    private val zone = ZoneId.of("UTC")
    private val hour = 3_600_000L

    private fun at(date: String, hourOfDay: Int): Long =
        LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli() + hourOfDay * hour

    @Test
    fun sessionAcrossMidnightIsSplitBetweenDays() {
        val sessions = listOf(SessionSpan(at("2026-09-24", 22), at("2026-09-25", 3)))
        val stats = StatsCalculator.compute(sessions, emptyList(), now = at("2026-09-25", 12), zone)
        assertEquals(2 * hour, stats.days.first { it.date == LocalDate.parse("2026-09-24") }.lockedMs)
        assertEquals(3 * hour, stats.days.last().lockedMs)
        assertEquals(5 * hour, stats.totalLockedMs)
    }

    @Test
    fun ongoingSessionCountsUpToNow() {
        val sessions = listOf(SessionSpan(at("2026-09-25", 9), end = null))
        val stats = StatsCalculator.compute(sessions, emptyList(), now = at("2026-09-25", 11), zone)
        assertEquals(2 * hour, stats.days.last().lockedMs)
        assertEquals(2 * hour, stats.longestSessionMs)
    }

    @Test
    fun sevenDaysOldestFirstEndingToday() {
        val stats = StatsCalculator.compute(emptyList(), emptyList(), now = at("2026-09-25", 12), zone)
        assertEquals(7, stats.days.size)
        assertEquals(LocalDate.parse("2026-09-19"), stats.days.first().date)
        assertEquals(LocalDate.parse("2026-09-25"), stats.days.last().date)
    }

    @Test
    fun blockedOpensAreCountedPerDay() {
        val opens = listOf(at("2026-09-25", 9), at("2026-09-25", 10), at("2026-09-23", 9))
        val stats = StatsCalculator.compute(emptyList(), opens, now = at("2026-09-25", 12), zone)
        assertEquals(2, stats.days.last().blockedOpens)
        assertEquals(3, stats.totalBlockedOpens)
    }

    @Test
    fun streakCountsConsecutiveDaysAndTodayDoesNotBreakIt() {
        val sessions = listOf(
            SessionSpan(at("2026-09-22", 9), at("2026-09-22", 10)),
            SessionSpan(at("2026-09-23", 9), at("2026-09-23", 10)),
            SessionSpan(at("2026-09-24", 9), at("2026-09-24", 10)),
        )
        // Nothing yet today: the streak up to yesterday still counts.
        assertEquals(3, StatsCalculator.compute(sessions, emptyList(), now = at("2026-09-25", 8), zone).streakDays)
        // A gap day breaks it.
        val withGap = sessions.drop(1) + SessionSpan(at("2026-09-20", 9), at("2026-09-20", 10))
        assertEquals(2, StatsCalculator.compute(withGap, emptyList(), now = at("2026-09-25", 8), zone).streakDays)
    }

    @Test
    fun noSessionsMeansNoStreak() {
        val stats = StatsCalculator.compute(emptyList(), emptyList(), now = at("2026-09-25", 8), zone)
        assertEquals(0, stats.streakDays)
        assertEquals(0L, stats.longestSessionMs)
    }
}
