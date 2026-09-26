package com.livefree.core.data

import com.livefree.core.model.LockStats
import com.livefree.core.model.SessionSpan
import com.livefree.core.model.StatsCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.time.ZoneId

class StatsRepository internal constructor(
    private val sessions: SessionDao,
    private val events: BlockEventDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    suspend fun recordBlockedOpen(packageName: String) {
        events.insert(BlockEventEntity(packageName = packageName, at = clock()))
    }

    /** Stats for the last [dayCount] days, refreshed every minute so an ongoing lock keeps counting. */
    fun observe(dayCount: Int = 7): Flow<LockStats> {
        val since = clock() - (dayCount + 1) * DAY_MS
        val ticks = flow {
            while (true) {
                emit(Unit)
                delay(TICK_MS)
            }
        }
        return combine(sessions.observeAll(), events.observeTimesSince(since), ticks) { all, opens, _ ->
            StatsCalculator.compute(
                sessions = all.map { SessionSpan(it.startedAt, it.endedAt) },
                blockedOpenTimes = opens,
                now = clock(),
                zone = zone(),
                dayCount = dayCount,
            )
        }
    }

    private companion object {
        const val DAY_MS = 86_400_000L
        const val TICK_MS = 60_000L
    }
}
