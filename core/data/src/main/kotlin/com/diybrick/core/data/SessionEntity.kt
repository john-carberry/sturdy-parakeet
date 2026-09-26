package com.diybrick.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One bricked period. The open session (endedAt == null) is the current brick. */
@Entity(tableName = "sessions", indices = [Index("endedAt")])
internal data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modeId: Long,
    val startedAt: Long,
    val endedAt: Long? = null,
    val endReason: String? = null,
)
