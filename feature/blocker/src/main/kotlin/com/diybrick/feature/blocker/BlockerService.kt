package com.diybrick.feature.blocker

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.os.HandlerCompat
import com.diybrick.core.data.BrickData
import com.diybrick.core.model.BlockPolicy
import com.diybrick.core.model.BrickState
import com.diybrick.core.model.EssentialApps
import com.diybrick.core.model.Mode
import com.diybrick.core.model.TamperGuard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Tier A blocking (PLAN.md §3): when a blocked app comes to the foreground while the
 * phone is bricked, go Home and show [BlockedActivity] explaining why it was closed.
 * While bricked it also closes Settings screens that could switch DIY Brick off.
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
    private var lastTamperCheckAt = 0L

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
        val packageName = event.packageName?.toString() ?: return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                foregroundPackage = packageName
                blockIfNeeded(packageName)
                guardSettings(packageName, force = true)
            }
            // Settings often swaps pages without a new window, so watch its content too.
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> guardSettings(packageName, force = false)
            else -> Unit
        }
    }

    /**
     * While bricked, close Settings or uninstaller screens that could switch DIY Brick
     * off. Screen text is only read here, for those packages, while bricked.
     */
    private fun guardSettings(packageName: String, force: Boolean) {
        if (packageName !in TamperGuard.GUARDED_PACKAGES) return
        val bricked = state as? BrickState.Bricked ?: return
        val now = SystemClock.elapsedRealtime()
        val wait = TAMPER_CHECK_INTERVAL_MS - (now - lastTamperCheckAt)
        if (!force && wait > 0) {
            // Rate-limited, but always check once the burst of changes settles.
            handler.removeCallbacksAndMessages(TAMPER_TOKEN)
            HandlerCompat.postDelayed(handler, { guardSettings(packageName, force = true) }, TAMPER_TOKEN, wait)
            return
        }
        lastTamperCheckAt = now

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != packageName) return
        val text = collectText(root)
        if (TamperGuard.isTamperScreen(packageName, text, appLabel(), getString(R.string.blocker_service_label))) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            startActivity(BlockedActivity.tamperIntent(this, bricked.since))
        }
    }

    private fun appLabel(): String = applicationInfo.loadLabel(packageManager).toString()

    /** Visible text on screen, breadth-first, capped so huge screens stay cheap. */
    private fun collectText(root: AccessibilityNodeInfo): List<String> {
        val out = mutableListOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++
            node.text?.let { out += it.toString() }
            node.contentDescription?.let { out += it.toString() }
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        return out
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
        val TAMPER_TOKEN = Any()
        const val TAMPER_CHECK_INTERVAL_MS = 300L
        const val MAX_NODES = 500
    }
}
