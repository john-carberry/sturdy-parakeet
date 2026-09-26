package com.livefree.feature.blocker

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

object BlockerStatus {
    /** Whether the user has turned on the blocker in Accessibility settings. */
    fun isEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val me = ComponentName(context, BlockerService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    fun accessibilitySettingsIntent() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    /** Whether uninstall protection (device admin) is on. */
    fun isAdminActive(context: Context): Boolean =
        context.getSystemService(DevicePolicyManager::class.java)
            ?.isAdminActive(ComponentName(context, LockAdminReceiver::class.java)) == true

    fun addAdminIntent(context: Context) =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(context, LockAdminReceiver::class.java))
            .putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Stops Live Free being uninstalled while your phone is locked. " +
                    "It can't see or change anything else on your phone.",
            )

    /** App info, where Android 13+ hides "Allow restricted settings" for sideloaded apps. */
    fun appInfoIntent(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
}

/** Blocker on/off, re-checked whenever the screen resumes (e.g. back from Settings). */
@Composable
fun rememberBlockerEnabled(): Boolean = rememberOnResume(BlockerStatus::isEnabled)

/** Uninstall protection on/off, re-checked whenever the screen resumes. */
@Composable
fun rememberAdminActive(): Boolean = rememberOnResume(BlockerStatus::isAdminActive)

@Composable
private fun rememberOnResume(check: (Context) -> Boolean): Boolean {
    val context = LocalContext.current
    var value by remember { mutableStateOf(check(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { value = check(context) }
    return value
}
