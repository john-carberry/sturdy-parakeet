package com.diybrick.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
internal abstract class ModeDao {
    @Query("SELECT * FROM modes WHERE id = :id")
    abstract fun observeMode(id: Long): Flow<ModeEntity?>

    @Query("SELECT packageName FROM mode_apps WHERE modeId = :id")
    abstract fun observeApps(id: Long): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertMode(mode: ModeEntity)

    @Query("DELETE FROM mode_apps WHERE modeId = :id")
    protected abstract suspend fun deleteApps(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertApps(apps: List<ModeAppEntity>)

    /** Replaces a mode and its app list in one go. */
    @Transaction
    open suspend fun replace(mode: ModeEntity, packages: Set<String>) {
        upsertMode(mode)
        deleteApps(mode.id)
        insertApps(packages.map { ModeAppEntity(mode.id, it) })
    }
}
