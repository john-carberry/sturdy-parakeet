package com.diybrick.core.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [KeyEntity::class, SessionEntity::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
internal abstract class BrickDatabase : RoomDatabase() {
    abstract fun keyDao(): KeyDao
    abstract fun sessionDao(): SessionDao

    companion object {
        fun create(context: Context): BrickDatabase =
            Room.databaseBuilder(context, BrickDatabase::class.java, "brick.db").build()
    }
}
