package com.diybrick.core.data

import com.diybrick.core.model.BrickRules
import com.diybrick.core.model.BrickRules.Outcome
import com.diybrick.core.model.BrickSettings
import com.diybrick.core.model.BrickState
import com.diybrick.core.model.EndReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Persists the brick state (PLAN.md §4.3) so it survives app restarts and reboots. */
class BrickRepository internal constructor(
    private val dao: SessionDao,
    val settings: BrickSettings = BrickSettings(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    // Serialises read-decide-write so two quick taps can't both brick.
    private val mutex = Mutex()

    val state: Flow<BrickState> = dao.observeOpen().map { it.toState() }.distinctUntilChanged()

    val emergencyUnbricksRemaining: Flow<Int> =
        dao.observeCountByEndReason(EndReason.EMERGENCY.name)
            .map { BrickRules.emergencyUnbricksRemaining(settings, it) }

    suspend fun currentState(): BrickState = dao.getOpen().toState()

    /** A paired key was presented: brick if free, unbrick if bricked. */
    suspend fun onKey(): Outcome = mutex.withLock {
        val open = dao.getOpen()
        apply(open, BrickRules.onKey(open.toState()))
    }

    /** Starts the emergency-unbrick wait (PLAN.md §4.1, screen 7). */
    suspend fun requestEmergencyUnbrick(): Outcome = mutex.withLock {
        val open = dao.getOpen()
        val outcome = BrickRules.onEmergencyRequest(open.toState(), emergencyRemaining())
        if (open != null && outcome == Outcome.EmergencyRequested) {
            val pending = BrickRules.emergencyStatus(open.emergencyRequestedAt, clock(), settings)
            // Asking again doesn't restart a wait that's already running.
            if (pending == BrickRules.EmergencyStatus.NotRequested) dao.setEmergencyRequestedAt(open.id, clock())
        }
        outcome
    }

    suspend fun cancelEmergencyUnbrick() = mutex.withLock {
        dao.getOpen()?.let { dao.setEmergencyRequestedAt(it.id, null) }
    }

    /** Uses an emergency unbrick, once its wait is over. */
    suspend fun emergencyUnbrick(): Outcome = mutex.withLock {
        val open = dao.getOpen()
        apply(open, BrickRules.onEmergencyUnbrick(open.toState(), emergencyRemaining(), clock(), settings))
    }

    private suspend fun emergencyRemaining(): Int =
        BrickRules.emergencyUnbricksRemaining(settings, dao.countByEndReason(EndReason.EMERGENCY.name))

    private suspend fun apply(open: SessionEntity?, outcome: Outcome): Outcome {
        when (outcome) {
            is Outcome.Brick -> dao.insert(SessionEntity(modeId = outcome.modeId, startedAt = clock()))
            is Outcome.Unbrick -> open?.let { dao.end(it.id, clock(), outcome.reason.name) }
            is Outcome.Denied, Outcome.EmergencyRequested -> Unit
        }
        return outcome
    }

    private fun SessionEntity?.toState(): BrickState =
        this?.let { BrickState.Bricked(it.modeId, it.startedAt, it.emergencyRequestedAt) } ?: BrickState.Free
}
