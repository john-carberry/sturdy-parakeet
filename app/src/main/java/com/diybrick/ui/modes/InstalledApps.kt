package com.diybrick.ui.modes

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class InstalledApp(val packageName: String, val label: String, val icon: ImageBitmap)

object InstalledApps {
    private const val ICON_PX = 96

    /** Apps with a launcher icon, except this one, sorted by name. Slow: call off the main thread. */
    fun load(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(launcher, 0)
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .map { info ->
                InstalledApp(
                    packageName = info.packageName,
                    label = pm.getApplicationLabel(info).toString(),
                    icon = pm.getApplicationIcon(info).toBitmap(ICON_PX, ICON_PX).asImageBitmap(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
