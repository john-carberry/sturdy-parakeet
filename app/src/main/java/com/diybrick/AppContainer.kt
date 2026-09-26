package com.diybrick

import android.content.Context
import com.diybrick.core.data.BrickData

/** Hand-rolled dependency container; small enough that a DI framework isn't worth it yet. */
class AppContainer(context: Context) {
    private val data = BrickData(context)

    val keyRepository = data.keys
}
