package com.livefree

import android.content.Context
import com.livefree.core.data.LiveFreeData
import com.livefree.ui.setup.OnboardingPrefs

/** Hand-rolled dependency container; small enough that a DI framework isn't worth it yet. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val data = LiveFreeData.get(context)

    val keyRepository = data.keys
    val lockRepository = data.lock
    val modeRepository = data.modes
    val statsRepository = data.stats
    val onboarding = OnboardingPrefs(context)
}
