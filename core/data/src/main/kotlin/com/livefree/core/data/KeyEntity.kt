package com.livefree.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.livefree.core.model.Key
import com.livefree.core.model.KeyType

@Entity(tableName = "keys")
internal data class KeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val label: String,
    val secretHash: String,
    val createdAt: Long,
) {
    fun toModel() = Key(id, KeyType.valueOf(type), label, secretHash, createdAt)
}
