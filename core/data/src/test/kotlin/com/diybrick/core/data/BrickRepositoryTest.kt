package com.diybrick.core.data

import com.diybrick.core.model.BrickRules.DenyReason
import com.diybrick.core.model.BrickRules.Outcome
import com.diybrick.core.model.BrickSettings
import com.diybrick.core.model.BrickState
import com.diybrick.core.model.EndReason
import com.diybrick.core.model.Mode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private const val MIN = 60_000L

class BrickRepositoryTest {
    private val dao = FakeSessionDao()
    private var now = 1_000L
    private val repo = BrickRepository(dao, BrickSettings(emergencyUnbricks = 2), clock = { now })

    @Test
    fun keyTogglesBetweenFreeAndBricked() = runBlocking {
        assertEquals(BrickState.Free, repo.currentState())

        assertEquals(Outcome.Brick(Mode.DEFAULT_ID), repo.onKey())
        assertEquals(BrickState.Bricked(Mode.DEFAULT_ID, 1_000L), repo.state.first())

        now = 5_000L
        assertEquals(Outcome.Unbrick(EndReason.KEY), repo.onKey())
        assertEquals(BrickState.Free, repo.state.first())
        assertEquals(5_000L, dao.rows.value.single().endedAt)
    }

    private suspend fun waitThenEmergencyUnbrick(): Outcome {
        repo.requestEmergencyUnbrick()
        now += 10 * MIN
        return repo.emergencyUnbrick()
    }

    @Test
    fun emergencyUnbrickNeedsTheWait() = runBlocking {
        repo.onKey()
        assertEquals(Outcome.EmergencyRequested, repo.requestEmergencyUnbrick())
        now += 5 * MIN
        assertEquals(Outcome.Denied(DenyReason.EMERGENCY_STILL_WAITING), repo.emergencyUnbrick())
        now += 5 * MIN
        assertEquals(Outcome.Unbrick(EndReason.EMERGENCY), repo.emergencyUnbrick())
        assertEquals(BrickState.Free, repo.currentState())
    }

    @Test
    fun requestingAgainDoesNotRestartTheWait() = runBlocking {
        repo.onKey()
        repo.requestEmergencyUnbrick()
        now += 8 * MIN
        repo.requestEmergencyUnbrick()
        now += 2 * MIN
        assertEquals(Outcome.Unbrick(EndReason.EMERGENCY), repo.emergencyUnbrick())
    }

    @Test
    fun cancelledRequestCannotBeUsed() = runBlocking {
        repo.onKey()
        repo.requestEmergencyUnbrick()
        repo.cancelEmergencyUnbrick()
        now += 11 * MIN
        assertEquals(Outcome.Denied(DenyReason.EMERGENCY_NOT_REQUESTED), repo.emergencyUnbrick())
    }

    @Test
    fun emergencyUnbricksAreLimited() = runBlocking {
        repeat(2) {
            repo.onKey()
            assertEquals(Outcome.Unbrick(EndReason.EMERGENCY), waitThenEmergencyUnbrick())
        }
        repo.onKey()
        assertEquals(
            Outcome.Denied(DenyReason.NO_EMERGENCY_UNBRICKS_LEFT),
            repo.requestEmergencyUnbrick(),
        )
        assertEquals(0, repo.emergencyUnbricksRemaining.first())
        assert(repo.currentState() is BrickState.Bricked)
    }

    @Test
    fun keyUnbricksDoNotUseEmergencyAllowance() = runBlocking {
        repo.onKey()
        repo.onKey()
        assertEquals(2, repo.emergencyUnbricksRemaining.first())
    }

    @Test
    fun emergencyWhenFreeIsDenied() = runBlocking {
        assertEquals(Outcome.Denied(DenyReason.NOT_BRICKED), repo.requestEmergencyUnbrick())
        assertEquals(Outcome.Denied(DenyReason.NOT_BRICKED), repo.emergencyUnbrick())
        assertEquals(2, repo.emergencyUnbricksRemaining.first())
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
