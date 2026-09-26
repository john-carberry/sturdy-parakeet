package com.livefree.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface KeyDao {
    @Query("SELECT * FROM keys ORDER BY createdAt")
    fun observeAll(): Flow<List<KeyEntity>>

    @Query("SELECT * FROM keys WHERE type = :type")
    suspend fun getByType(type: String): List<KeyEntity>

    @Insert
    suspend fun insert(key: KeyEntity): Long

    @Query("DELETE FROM keys WHERE id = :id")
    suspend fun delete(id: Long)
}
