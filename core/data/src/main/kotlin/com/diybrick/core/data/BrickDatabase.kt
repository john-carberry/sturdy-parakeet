package com.diybrick.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [KeyEntity::class], version = 1)
internal abstract class BrickDatabase : RoomDatabase() {
    abstract fun keyDao(): KeyDao

    companion object {
        fun create(context: Context): BrickDatabase =
            Room.databaseBuilder(context, BrickDatabase::class.java, "brick.db").build()
    }
}
