package com.diybrick.feature.blocker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager

/**
 * Apps that are never blocked, even in allow-list mode, so the phone stays usable
 * and you can always get back to DIY Brick to unbrick.
 */
object ExemptApps {
    private val ALWAYS = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.emergency",
        "com.android.server.telecom",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
    )

    fun load(context: Context): Set<String> = buildSet {
        addAll(ALWAYS)
        add(context.packageName)
        addAll(launchers(context))
        context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage?.let(::add)
        context.getSystemService(InputMethodManager::class.java)
            ?.enabledInputMethodList
            ?.forEach { add(it.packageName) }
    }

    private fun launchers(context: Context): List<String> {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return context.packageManager
            .queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }
    }
}
