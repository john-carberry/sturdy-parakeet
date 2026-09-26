package com.diybrick.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface BlockEventDao {
    @Insert
    suspend fun insert(event: BlockEventEntity)

    @Query("SELECT at FROM block_events WHERE at >= :from")
    fun observeTimesSince(from: Long): Flow<List<Long>>
}
