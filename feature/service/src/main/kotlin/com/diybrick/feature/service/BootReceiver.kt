package com.diybrick.feature.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.diybrick.core.data.BrickData
import com.diybrick.core.model.BrickState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Restarts [BrickService] after a reboot or app update if the phone was bricked. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (BrickData.get(context).brick.currentState() is BrickState.Bricked) {
                    BrickService.start(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
