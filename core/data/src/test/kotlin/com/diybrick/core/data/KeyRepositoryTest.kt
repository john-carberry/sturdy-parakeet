package com.diybrick.core.data

import com.diybrick.core.model.KeyType
import com.diybrick.core.security.KeyHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyRepositoryTest {
    private val dao = FakeKeyDao()
    private var locked = false
    private val repo = KeyRepository(
        dao,
        KeyHasher(ByteArray(32) { it.toByte() }),
        clock = { 1000L },
        isLocked = { locked },
    )
    private val uid = byteArrayOf(0x04, 0x11, 0x22, 0x33)

    @Test
    fun addedKeyIsFoundAndStoresNoRawSecret() = runBlocking {
        val result = repo.add(KeyType.NFC_UID, "  Hotel card ", uid)
        assertTrue(result is KeyRepository.AddResult.Added)
        val found = repo.find(KeyType.NFC_UID, uid)
        assertEquals("Hotel card", found?.label)
        assertFalse(dao.rows.value.single().secretHash.contains("04112233"))
    }

    @Test
    fun sameSecretTwiceIsAlreadyPaired() = runBlocking {
        repo.add(KeyType.NFC_UID, "Card", uid)
        val result = repo.add(KeyType.NFC_UID, "Again", uid)
        assertTrue(result is KeyRepository.AddResult.AlreadyPaired)
        assertEquals(1, dao.rows.value.size)
    }

    @Test
    fun typeMustMatch() = runBlocking {
        repo.add(KeyType.NFC_UID, "Card", uid)
        assertNull(repo.find(KeyType.QR, uid))
    }

    @Test
    fun removedKeyIsGone() = runBlocking {
        val added = repo.add(KeyType.QR, "Fridge", ByteArray(32)) as KeyRepository.AddResult.Added
        repo.remove(added.key.id)
        assertTrue(repo.keys.first().isEmpty())
    }

    @Test
    fun keysCannotChangeWhileLocked() = runBlocking {
        val added = repo.add(KeyType.QR, "Fridge", ByteArray(32)) as KeyRepository.AddResult.Added
        locked = true
        assertEquals(KeyRepository.AddResult.Locked, repo.add(KeyType.NFC_UID, "Card", uid))
        assertFalse(repo.remove(added.key.id))
        assertEquals(1, dao.rows.value.size)
    }
}

private class FakeKeyDao : KeyDao {
    val rows = MutableStateFlow(emptyList<KeyEntity>())
    private var nextId = 1L

    override fun observeAll(): Flow<List<KeyEntity>> = rows

    override suspend fun getByType(type: String) = rows.value.filter { it.type == type }

    override suspend fun insert(key: KeyEntity): Long {
        val id = nextId++
        rows.value = rows.value + key.copy(id = id)
        return id
    }

    override suspend fun delete(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }
}
