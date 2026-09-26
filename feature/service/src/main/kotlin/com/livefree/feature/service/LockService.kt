package com.livefree.feature.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.livefree.core.data.LiveFreeData
import com.livefree.core.model.LockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Runs while the phone is locked: shows the "Locked" notification and keeps the
 * app's process alive (PLAN.md §3). Stops itself as soon as the phone is unlocked.
 */
class LockService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watching = false
    private var locked: LockState.Locked? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForeground must be called promptly after startForegroundService.
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(locked?.since), FOREGROUND_TYPE)
        if (!watching) {
            watching = true
            scope.launch {
                LiveFreeData.get(this@LockService).lock.state.collect { state ->
                    when (state) {
                        is LockState.Locked -> {
                            locked = state
                            getSystemService(NotificationManager::class.java)
                                .notify(NOTIFICATION_ID, notification(state.since))
                        }
                        LockState.Unlocked -> {
                            ServiceCompat.stopForeground(this@LockService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        }
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun notification(since: Long?): Notification {
        ensureChannel()
        val openApp = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_lock)
            .setContentTitle(getString(R.string.lock_notification_title))
            .setContentText(getString(R.string.lock_notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .apply {
                if (since != null) {
                    // Shows a live "locked for" timer.
                    setWhen(since)
                    setShowWhen(true)
                    setUsesChronometer(true)
                }
            }
            .build()
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.lock_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = getString(R.string.lock_channel_description) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "LockService"
        private const val CHANNEL_ID = "lock_status"
        private const val NOTIFICATION_ID = 1

        private val FOREGROUND_TYPE =
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0

        /**
         * Starts the service. Only allowed while the app is visible or from the boot
         * receiver; elsewhere Android refuses, which we log rather than crash on.
         */
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, LockService::class.java))
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Not allowed to start the lock service right now", e)
            }
        }
    }
}
