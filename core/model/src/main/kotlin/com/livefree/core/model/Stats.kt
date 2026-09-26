package com.livefree.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A locked period; [end] is null while it's still going. */
data class SessionSpan(val start: Long, val end: Long?)

data class DayStat(val date: LocalDate, val lockedMs: Long, val blockedOpens: Int)

data class LockStats(
    /** The last few days, oldest first, ending today. */
    val days: List<DayStat>,
    val longestSessionMs: Long,
    /** Days in a row with some time locked, ending today (or yesterday, if today hasn't started yet). */
    val streakDays: Int,
) {
    val totalLockedMs: Long get() = days.sumOf { it.lockedMs }
    val totalBlockedOpens: Int get() = days.sumOf { it.blockedOpens }
}

object StatsCalculator {
    private const val MAX_STREAK_DAYS = 3_650

    fun compute(
        sessions: List<SessionSpan>,
        blockedOpenTimes: List<Long>,
        now: Long,
        zone: ZoneId,
        dayCount: Int = 7,
    ): LockStats {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val days = (dayCount - 1 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
            DayStat(
                date = date,
                lockedMs = lockedMsOn(date, sessions, now, zone),
                blockedOpens = blockedOpenTimes.count { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == date },
            )
        }
        val longest = sessions.maxOfOrNull { (it.end ?: now) - it.start }?.coerceAtLeast(0) ?: 0
        return LockStats(days, longest, streak(today, sessions, now, zone))
    }

    fun lockedMsOn(date: LocalDate, sessions: List<SessionSpan>, now: Long, zone: ZoneId): Long {
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return sessions.sumOf { session ->
            val start = maxOf(session.start, dayStart)
            val end = minOf(session.end ?: now, dayEnd, now)
            (end - start).coerceAtLeast(0)
        }
    }

    private fun streak(today: LocalDate, sessions: List<SessionSpan>, now: Long, zone: ZoneId): Int {
        if (sessions.isEmpty()) return 0
        // Today doesn't break the streak until it's over.
        var date = if (lockedMsOn(today, sessions, now, zone) > 0) today else today.minusDays(1)
        var count = 0
        while (count < MAX_STREAK_DAYS && lockedMsOn(date, sessions, now, zone) > 0) {
            count++
            date = date.minusDays(1)
        }
        return count
    }
}
