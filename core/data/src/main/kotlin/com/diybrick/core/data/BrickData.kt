package com.diybrick.core.data

import android.content.Context
import com.diybrick.core.security.KeyHasher

/** Entry point to the app's stored data. Create one per process. */
class BrickData(context: Context) {
    private val database = BrickDatabase.create(context)

    val keys = KeyRepository(
        dao = database.keyDao(),
        hasher = KeyHasher(SaltStore.getOrCreate(context)),
    )
}
