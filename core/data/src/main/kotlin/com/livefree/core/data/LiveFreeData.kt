package com.livefree.core.data

import android.content.Context
import com.livefree.core.model.LockState
import com.livefree.core.security.KeyHasher

/** Entry point to the app's stored data. One per process; get it with [LiveFreeData.get]. */
class LiveFreeData private constructor(context: Context) {
    private val database = LiveFreeDatabase.create(context)

    val lock = LockRepository(database.sessionDao())

    private val isLocked: suspend () -> Boolean = { lock.currentState() is LockState.Locked }

    val keys = KeyRepository(
        dao = database.keyDao(),
        hasher = KeyHasher(SaltStore.getOrCreate(context)),
        isLocked = isLocked,
    )

    val modes = ModeRepository(database.modeDao(), isLocked = isLocked)

    val stats = StatsRepository(database.sessionDao(), database.blockEventDao())

    companion object {
        @Volatile
        private var instance: LiveFreeData? = null

        fun get(context: Context): LiveFreeData =
            instance ?: synchronized(this) {
                instance ?: LiveFreeData(context.applicationContext).also { instance = it }
            }
    }
}
