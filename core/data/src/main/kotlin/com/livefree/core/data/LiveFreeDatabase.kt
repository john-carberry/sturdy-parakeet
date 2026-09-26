package com.livefree.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        KeyEntity::class,
        SessionEntity::class,
        ModeEntity::class,
        ModeAppEntity::class,
        BlockEventEntity::class,
    ],
    version = 1,
)
internal abstract class LiveFreeDatabase : RoomDatabase() {
    abstract fun keyDao(): KeyDao
    abstract fun sessionDao(): SessionDao
    abstract fun modeDao(): ModeDao
    abstract fun blockEventDao(): BlockEventDao

    companion object {
        fun create(context: Context): LiveFreeDatabase =
            Room.databaseBuilder(context, LiveFreeDatabase::class.java, "livefree.db").build()
    }
}
