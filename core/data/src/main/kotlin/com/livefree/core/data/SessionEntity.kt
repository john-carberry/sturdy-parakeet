package com.livefree.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One locked period. The open session (endedAt == null) is the current lock. */
@Entity(tableName = "sessions", indices = [Index("endedAt")])
internal data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modeId: Long,
    val startedAt: Long,
    val endedAt: Long? = null,
    val endReason: String? = null,
    /** When an emergency unlock was requested for this session, if one is pending. */
    val emergencyRequestedAt: Long? = null,
)
