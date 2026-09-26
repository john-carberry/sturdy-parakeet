package com.diybrick.core.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [KeyEntity::class, SessionEntity::class, ModeEntity::class, ModeAppEntity::class],
    version = 4,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
    ],
)
internal abstract class BrickDatabase : RoomDatabase() {
    abstract fun keyDao(): KeyDao
    abstract fun sessionDao(): SessionDao
    abstract fun modeDao(): ModeDao

    companion object {
        fun create(context: Context): BrickDatabase =
            Room.databaseBuilder(context, BrickDatabase::class.java, "brick.db").build()
    }
}
