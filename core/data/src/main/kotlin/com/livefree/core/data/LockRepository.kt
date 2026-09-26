package com.livefree.core.data

import com.livefree.core.model.LockRules
import com.livefree.core.model.LockRules.Outcome
import com.livefree.core.model.LockSettings
import com.livefree.core.model.LockState
import com.livefree.core.model.EndReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Persists the lock state (PLAN.md §4.3) so it survives app restarts and reboots. */
class LockRepository internal constructor(
    private val dao: SessionDao,
    val settings: LockSettings = LockSettings(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    // Serialises read-decide-write so two quick taps can't both lock.
    private val mutex = Mutex()

    val state: Flow<LockState> = dao.observeOpen().map { it.toState() }.distinctUntilChanged()

    val emergencyUnlocksRemaining: Flow<Int> =
        dao.observeCountByEndReason(EndReason.EMERGENCY.name)
            .map { LockRules.emergencyUnlocksRemaining(settings, it) }

    suspend fun currentState(): LockState = dao.getOpen().toState()

    /** A paired key was presented: lock if unlocked, unlock if locked. */
    suspend fun onKey(): Outcome = mutex.withLock {
        val open = dao.getOpen()
        apply(open, LockRules.onKey(open.toState()))
    }

    /** Starts the emergency-unlock wait (PLAN.md §4.1, screen 7). */
    suspend fun requestEmergencyUnlock(): Outcome = mutex.withLock {
        val open = dao.getOpen()
        val outcome = LockRules.onEmergencyRequest(open.toState(), emergencyRemaining())
        if (open != null && outcome == Outcome.EmergencyRequested) {
            val pending = LockRules.emergencyStatus(open.emergencyRequestedAt, clock(), settings)
            // Asking again doesn't restart a wait that's already running.
            if (pending == LockRules.EmergencyStatus.NotRequested) dao.setEmergencyRequestedAt(open.id, clock())
        }
        outcome
    }

    suspend fun cancelEmergencyUnlock() = mutex.withLock {
        dao.getOpen()?.let { dao.setEmergencyRequestedAt(it.id, null) }
    }

    /** Uses an emergency unlock, once its wait is over. */
    suspend fun emergencyUnlock(): Outcome = mutex.withLock {
        val open = dao.getOpen()
        apply(open, LockRules.onEmergencyUnlock(open.toState(), emergencyRemaining(), clock(), settings))
    }

    private suspend fun emergencyRemaining(): Int =
        LockRules.emergencyUnlocksRemaining(settings, dao.countByEndReason(EndReason.EMERGENCY.name))

    private suspend fun apply(open: SessionEntity?, outcome: Outcome): Outcome {
        when (outcome) {
            is Outcome.Lock -> dao.insert(SessionEntity(modeId = outcome.modeId, startedAt = clock()))
            is Outcome.Unlock -> open?.let { dao.end(it.id, clock(), outcome.reason.name) }
            is Outcome.Denied, Outcome.EmergencyRequested -> Unit
        }
        return outcome
    }

    private fun SessionEntity?.toState(): LockState =
        this?.let { LockState.Locked(it.modeId, it.startedAt, it.emergencyRequestedAt) } ?: LockState.Unlocked
}
