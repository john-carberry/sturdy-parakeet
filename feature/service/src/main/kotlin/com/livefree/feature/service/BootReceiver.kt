package com.livefree.feature.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.livefree.core.data.LiveFreeData
import com.livefree.core.model.LockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Restarts [LockService] after a reboot or app update if the phone was locked. */
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
                if (LiveFreeData.get(context).lock.currentState() is LockState.Locked) {
                    LockService.start(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
