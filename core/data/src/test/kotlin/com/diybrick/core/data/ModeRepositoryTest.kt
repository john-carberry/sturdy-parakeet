package com.diybrick.core.data

import com.diybrick.core.model.ListType
import com.diybrick.core.model.Mode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeRepositoryTest {
    private val dao = FakeModeDao()
    private var locked = false
    private val repo = ModeRepository(dao, isLocked = { locked })

    @Test
    fun defaultModeBlocksNothing() = runBlocking {
        val mode = repo.observe().first()
        assertEquals(Mode.DEFAULT_ID, mode.id)
        assertEquals(ListType.BLOCK, mode.listType)
        assertTrue(mode.packages.isEmpty())
    }

    @Test
    fun savedModeIsObserved() = runBlocking {
        val mode = Mode(Mode.DEFAULT_ID, "Focus", ListType.ALLOW, setOf("a", "b"))
        assertTrue(repo.save(mode))
        assertEquals(mode, repo.observe().first())
    }

    @Test
    fun savingReplacesTheAppList() = runBlocking {
        repo.save(Mode(Mode.DEFAULT_ID, "Focus", ListType.BLOCK, setOf("a", "b")))
        repo.save(Mode(Mode.DEFAULT_ID, "Focus", ListType.BLOCK, setOf("c")))
        assertEquals(setOf("c"), repo.observe().first().packages)
    }

    @Test
    fun cannotSaveWhileLocked() = runBlocking {
        locked = true
        assertFalse(repo.save(Mode(Mode.DEFAULT_ID, "Focus", ListType.BLOCK, setOf("a"))))
        assertTrue(repo.observe().first().packages.isEmpty())
    }
}

private class FakeModeDao : ModeDao() {
    private val modes = MutableStateFlow(emptyMap<Long, ModeEntity>())
    private val apps = MutableStateFlow(emptyList<ModeAppEntity>())

    override fun observeMode(id: Long): Flow<ModeEntity?> = modes.map { it[id] }

    override fun observeApps(id: Long): Flow<List<String>> =
        apps.map { list -> list.filter { it.modeId == id }.map { it.packageName } }

    override suspend fun upsertMode(mode: ModeEntity) {
        modes.value = modes.value + (mode.id to mode)
    }

    override suspend fun deleteApps(id: Long) {
        apps.value = apps.value.filterNot { it.modeId == id }
    }

    override suspend fun insertApps(apps: List<ModeAppEntity>) {
        this.apps.value = this.apps.value + apps
    }
}
