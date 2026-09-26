package com.diybrick.feature.blocker

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
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

    private var lastBlockedPackage: String? = null
    private var lastBlockedAt = 0L

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
        val mode = mode ?: return
        val bricked = state as? BrickState.Bricked ?: return
        if (!BlockPolicy.shouldBlock(packageName, bricked, mode, exempt)) return

        // One app can fire several window events in a burst; block it once.
        val now = SystemClock.elapsedRealtime()
        if (packageName == lastBlockedPackage && now - lastBlockedAt < DEBOUNCE_MS) return
        lastBlockedPackage = packageName
        lastBlockedAt = now

        performGlobalAction(GLOBAL_ACTION_HOME)
        startActivity(BlockedActivity.intent(this, packageName, mode.listType, bricked.since))
        // Some phones stop background services from opening screens; the toast still explains.
        val label = AppLabels.of(this, packageName)
        Toast.makeText(this, "DIY Brick closed $label: your phone is bricked", Toast.LENGTH_LONG).show()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val DEBOUNCE_MS = 1_000L
    }
}
