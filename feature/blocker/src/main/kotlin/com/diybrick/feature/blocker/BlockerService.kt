package com.diybrick.feature.blocker

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.os.HandlerCompat
import com.diybrick.core.data.BrickData
import com.diybrick.core.model.BlockPolicy
import com.diybrick.core.model.BrickState
import com.diybrick.core.model.EssentialApps
import com.diybrick.core.model.Mode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Tier A blocking (PLAN.md §3): when a blocked app comes to the foreground while the
 * phone is bricked, go Home and show [BlockedActivity] explaining why it was closed.
 */
class BlockerService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Written by the collector and read by onAccessibilityEvent, both on the main thread.
    private var state: BrickState = BrickState.Free
    private var mode: Mode? = null
    private var exempt: Set<String> = EssentialApps.KNOWN.keys

    /** The app most recently seen coming to the foreground. */
    private var foregroundPackage: String? = null
    private val messages = MessageThrottle(clock = SystemClock::elapsedRealtime)
    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        exempt = ExemptApps.load(this).keys
        val data = BrickData.get(this)
        scope.launch {
            combine(data.brick.state, data.modes.observe()) { s, m -> s to m }
                .collect { (s, m) ->
                    state = s
                    mode = m
                    // The launcher or keyboard may have changed since we connected.
                    exempt = ExemptApps.load(this@BlockerService).keys
                }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        foregroundPackage = packageName
        blockIfNeeded(packageName)
    }

    /** Every launch of a blocked app is closed, however quickly it's reopened. */
    private fun blockIfNeeded(packageName: String) {
        val mode = mode ?: return
        val bricked = state as? BrickState.Bricked ?: return
        if (!BlockPolicy.shouldBlock(packageName, bricked, mode, exempt)) return

        performGlobalAction(GLOBAL_ACTION_HOME)
        startActivity(BlockedActivity.intent(this, packageName, mode.listType, bricked.since))
        if (messages.shouldShow(packageName)) {
            // Some phones stop background services from opening screens; the toast still explains.
            val label = AppLabels.of(this, packageName)
            Toast.makeText(this, "DIY Brick closed $label: your phone is bricked", Toast.LENGTH_LONG).show()
        }

        // If the app somehow ends up in front again without a new window event
        // (e.g. a fast relaunch racing the Home action), close it again. Replacing any
        // pending re-checks keeps at most a couple queued.
        handler.removeCallbacksAndMessages(RECHECK_TOKEN)
        RECHECK_DELAYS_MS.forEach { delay ->
            HandlerCompat.postDelayed(
                handler,
                { if (foregroundPackage == packageName) blockIfNeeded(packageName) },
                RECHECK_TOKEN,
                delay,
            )
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        val RECHECK_DELAYS_MS = listOf(500L, 1_500L)
        val RECHECK_TOKEN = Any()
    }
}
