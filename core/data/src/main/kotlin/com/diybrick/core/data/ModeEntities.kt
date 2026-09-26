package com.diybrick.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "modes")
internal data class ModeEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val listType: String,
)

/** One app in a mode's block list (or allow list). */
@Entity(tableName = "mode_apps", primaryKeys = ["modeId", "packageName"])
internal data class ModeAppEntity(
    val modeId: Long,
    val packageName: String,
)
