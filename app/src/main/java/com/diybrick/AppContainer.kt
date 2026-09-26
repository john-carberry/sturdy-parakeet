package com.diybrick

import android.content.Context
import com.diybrick.core.data.BrickData

/** Hand-rolled dependency container; small enough that a DI framework isn't worth it yet. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val data = BrickData.get(context)

    val keyRepository = data.keys
    val brickRepository = data.brick
    val modeRepository = data.modes
}
