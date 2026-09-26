package com.livefree.feature.blocker

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Device admin with no policies. Android won't uninstall an active admin, and the
 * page to deactivate it is locked while locked (see [BlockerService]).
 */
class LockAdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        context.getString(R.string.admin_disable_warning)
}
