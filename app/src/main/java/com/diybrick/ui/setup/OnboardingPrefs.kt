package com.diybrick.ui.setup

import android.content.Context

/** Remembers whether the first-run setup checklist has been finished or skipped. */
class OnboardingPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)

    var done: Boolean
        get() = prefs.getBoolean(KEY_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_DONE, value).apply()

    private companion object {
        const val KEY_DONE = "done"
    }
}
