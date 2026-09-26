package com.diybrick.feature.service

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
import com.diybrick.core.data.BrickData
import com.diybrick.core.model.BrickState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Runs while the phone is bricked: shows the "Bricked" notification and keeps the
 * app's process alive (PLAN.md §3). Stops itself as soon as the phone is unbricked.
 */
class BrickService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watching = false
    private var bricked: BrickState.Bricked? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForeground must be called promptly after startForegroundService.
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(bricked?.since), FOREGROUND_TYPE)
        if (!watching) {
            watching = true
            scope.launch {
                BrickData.get(this@BrickService).brick.state.collect { state ->
                    when (state) {
                        is BrickState.Bricked -> {
                            bricked = state
                            getSystemService(NotificationManager::class.java)
                                .notify(NOTIFICATION_ID, notification(state.since))
                        }
                        BrickState.Free -> {
                            ServiceCompat.stopForeground(this@BrickService, ServiceCompat.STOP_FOREGROUND_REMOVE)
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
            .setSmallIcon(R.drawable.ic_stat_brick)
            .setContentTitle(getString(R.string.brick_notification_title))
            .setContentText(getString(R.string.brick_notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .apply {
                if (since != null) {
                    // Shows a live "bricked for" timer.
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
            getString(R.string.brick_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = getString(R.string.brick_channel_description) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "BrickService"
        private const val CHANNEL_ID = "brick_status"
        private const val NOTIFICATION_ID = 1

        private val FOREGROUND_TYPE =
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0

        /**
         * Starts the service. Only allowed while the app is visible or from the boot
         * receiver; elsewhere Android refuses, which we log rather than crash on.
         */
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, BrickService::class.java))
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Not allowed to start the brick service right now", e)
            }
        }
    }
}
