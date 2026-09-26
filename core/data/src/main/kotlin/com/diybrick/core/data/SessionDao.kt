package com.diybrick.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SessionDao {
    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeOpen(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getOpen(): SessionEntity?

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("UPDATE sessions SET endedAt = :endedAt, endReason = :endReason WHERE id = :id")
    suspend fun end(id: Long, endedAt: Long, endReason: String)

    @Query("UPDATE sessions SET emergencyRequestedAt = :at WHERE id = :id")
    suspend fun setEmergencyRequestedAt(id: Long, at: Long?)

    @Query("SELECT COUNT(*) FROM sessions WHERE endReason = :reason")
    fun observeCountByEndReason(reason: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM sessions WHERE endReason = :reason")
    suspend fun countByEndReason(reason: String): Int
}
