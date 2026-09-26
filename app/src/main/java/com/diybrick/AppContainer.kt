package com.diybrick

import android.content.Context
import com.diybrick.core.data.BrickData
import com.diybrick.ui.setup.OnboardingPrefs

/** Hand-rolled dependency container; small enough that a DI framework isn't worth it yet. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val data = BrickData.get(context)

    val keyRepository = data.keys
    val brickRepository = data.brick
    val modeRepository = data.modes
    val statsRepository = data.stats
    val onboarding = OnboardingPrefs(context)
}
