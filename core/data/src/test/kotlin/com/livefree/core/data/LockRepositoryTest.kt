package com.livefree.core.data

import com.livefree.core.model.LockRules.DenyReason
import com.livefree.core.model.LockRules.Outcome
import com.livefree.core.model.LockSettings
import com.livefree.core.model.LockState
import com.livefree.core.model.EndReason
import com.livefree.core.model.Mode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private const val MIN = 60_000L

class LockRepositoryTest {
    private val dao = FakeSessionDao()
    private var now = 1_000L
    private val repo = LockRepository(dao, LockSettings(emergencyUnlocks = 2), clock = { now })

    @Test
    fun keyTogglesBetweenFreeAndLocked() = runBlocking {
        assertEquals(LockState.Unlocked, repo.currentState())

        assertEquals(Outcome.Lock(Mode.DEFAULT_ID), repo.onKey())
        assertEquals(LockState.Locked(Mode.DEFAULT_ID, 1_000L), repo.state.first())

        now = 5_000L
        assertEquals(Outcome.Unlock(EndReason.KEY), repo.onKey())
        assertEquals(LockState.Unlocked, repo.state.first())
        assertEquals(5_000L, dao.rows.value.single().endedAt)
    }

    private suspend fun waitThenEmergencyUnlock(): Outcome {
        repo.requestEmergencyUnlock()
        now += 10 * MIN
        return repo.emergencyUnlock()
    }

    @Test
    fun emergencyUnlockNeedsTheWait() = runBlocking {
        repo.onKey()
        assertEquals(Outcome.EmergencyRequested, repo.requestEmergencyUnlock())
        now += 5 * MIN
        assertEquals(Outcome.Denied(DenyReason.EMERGENCY_STILL_WAITING), repo.emergencyUnlock())
        now += 5 * MIN
        assertEquals(Outcome.Unlock(EndReason.EMERGENCY), repo.emergencyUnlock())
        assertEquals(LockState.Unlocked, repo.currentState())
    }

    @Test
    fun requestingAgainDoesNotRestartTheWait() = runBlocking {
        repo.onKey()
        repo.requestEmergencyUnlock()
        now += 8 * MIN
        repo.requestEmergencyUnlock()
        now += 2 * MIN
        assertEquals(Outcome.Unlock(EndReason.EMERGENCY), repo.emergencyUnlock())
    }

    @Test
    fun cancelledRequestCannotBeUsed() = runBlocking {
        repo.onKey()
        repo.requestEmergencyUnlock()
        repo.cancelEmergencyUnlock()
        now += 11 * MIN
        assertEquals(Outcome.Denied(DenyReason.EMERGENCY_NOT_REQUESTED), repo.emergencyUnlock())
    }

    @Test
    fun emergencyUnlocksAreLimited() = runBlocking {
        repeat(2) {
            repo.onKey()
            assertEquals(Outcome.Unlock(EndReason.EMERGENCY), waitThenEmergencyUnlock())
        }
        repo.onKey()
        assertEquals(
            Outcome.Denied(DenyReason.NO_EMERGENCY_UNLOCKS_LEFT),
            repo.requestEmergencyUnlock(),
        )
        assertEquals(0, repo.emergencyUnlocksRemaining.first())
        assert(repo.currentState() is LockState.Locked)
    }

    @Test
    fun keyUnlocksDoNotUseEmergencyAllowance() = runBlocking {
        repo.onKey()
        repo.onKey()
        assertEquals(2, repo.emergencyUnlocksRemaining.first())
    }

    @Test
    fun emergencyWhenFreeIsDenied() = runBlocking {
        assertEquals(Outcome.Denied(DenyReason.NOT_LOCKED), repo.requestEmergencyUnlock())
        assertEquals(Outcome.Denied(DenyReason.NOT_LOCKED), repo.emergencyUnlock())
        assertEquals(2, repo.emergencyUnlocksRemaining.first())
    }
}

private class FakeSessionDao : SessionDao {
    val rows = MutableStateFlow(emptyList<SessionEntity>())
    private var nextId = 1L

    private fun open(list: List<SessionEntity>) = list.lastOrNull { it.endedAt == null }

    override fun observeOpen(): Flow<SessionEntity?> = rows.map(::open)

    override fun observeAll(): Flow<List<SessionEntity>> = rows

    override suspend fun getOpen() = open(rows.value)

    override suspend fun insert(session: SessionEntity): Long {
        val id = nextId++
        rows.value = rows.value + session.copy(id = id)
        return id
    }

    override suspend fun end(id: Long, endedAt: Long, endReason: String) {
        rows.value = rows.value.map {
            if (it.id == id) it.copy(endedAt = endedAt, endReason = endReason) else it
        }
    }

    override fun observeCountByEndReason(reason: String): Flow<Int> =
        rows.map { list -> list.count { it.endReason == reason } }

    override suspend fun countByEndReason(reason: String) = rows.value.count { it.endReason == reason }

    override suspend fun setEmergencyRequestedAt(id: Long, at: Long?) {
        rows.value = rows.value.map { if (it.id == id) it.copy(emergencyRequestedAt = at) else it }
    }
}
