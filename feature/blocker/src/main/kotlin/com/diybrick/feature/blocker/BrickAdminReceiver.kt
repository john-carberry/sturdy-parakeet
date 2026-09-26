package com.diybrick.feature.blocker

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Device admin with no policies. Android won't uninstall an active admin, and the
 * page to deactivate it is locked while bricked (see [BlockerService]).
 */
class BrickAdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        context.getString(R.string.admin_disable_warning)
}
