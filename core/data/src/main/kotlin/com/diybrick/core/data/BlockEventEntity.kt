package com.diybrick.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One time a blocked app was opened (and closed) while bricked. Used for stats. */
@Entity(tableName = "block_events", indices = [Index("at")])
internal data class BlockEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val at: Long,
)
