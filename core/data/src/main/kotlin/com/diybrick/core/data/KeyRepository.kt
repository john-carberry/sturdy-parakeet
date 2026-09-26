package com.diybrick.core.data

import com.diybrick.core.model.Key
import com.diybrick.core.model.KeyType
import com.diybrick.core.security.KeyHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class KeyRepository internal constructor(
    private val dao: KeyDao,
    private val hasher: KeyHasher,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    sealed interface AddResult {
        data class Added(val key: Key) : AddResult
        data class AlreadyPaired(val existing: Key) : AddResult
    }

    val keys: Flow<List<Key>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun add(type: KeyType, label: String, secret: ByteArray): AddResult {
        find(type, secret)?.let { return AddResult.AlreadyPaired(it) }
        val entity = KeyEntity(
            type = type.name,
            label = label.trim(),
            secretHash = hasher.hash(secret),
            createdAt = clock(),
        )
        val id = dao.insert(entity)
        return AddResult.Added(entity.copy(id = id).toModel())
    }

    /** The paired key matching this scanned secret, or null if it isn't one of ours. */
    suspend fun find(type: KeyType, secret: ByteArray): Key? =
        dao.getByType(type.name)
            .firstOrNull { hasher.matches(secret, it.secretHash) }
            ?.toModel()

    suspend fun remove(id: Long) = dao.delete(id)
}
