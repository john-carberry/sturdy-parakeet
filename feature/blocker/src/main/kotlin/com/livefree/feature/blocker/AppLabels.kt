package com.livefree.feature.blocker

import android.content.Context
import android.content.pm.PackageManager

internal object AppLabels {
    fun of(context: Context, packageName: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        "This app"
    }
}
