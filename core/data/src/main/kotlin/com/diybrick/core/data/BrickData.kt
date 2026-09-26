package com.diybrick.core.data

import android.content.Context
import com.diybrick.core.model.BrickState
import com.diybrick.core.security.KeyHasher

/** Entry point to the app's stored data. One per process; get it with [BrickData.get]. */
class BrickData private constructor(context: Context) {
    private val database = BrickDatabase.create(context)

    val brick = BrickRepository(database.sessionDao())

    val keys = KeyRepository(
        dao = database.keyDao(),
        hasher = KeyHasher(SaltStore.getOrCreate(context)),
        isLocked = { brick.currentState() is BrickState.Bricked },
    )

    companion object {
        @Volatile
        private var instance: BrickData? = null

        fun get(context: Context): BrickData =
            instance ?: synchronized(this) {
                instance ?: BrickData(context.applicationContext).also { instance = it }
            }
    }
}
