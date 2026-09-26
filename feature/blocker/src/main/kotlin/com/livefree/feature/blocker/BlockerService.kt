package com.livefree.feature.blocker

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.HandlerCompat
import com.livefree.core.data.LiveFreeData
import com.livefree.core.model.BlockPolicy
import com.livefree.core.model.LockState
import com.livefree.core.model.EssentialApps
import com.livefree.core.model.Mode
import com.livefree.core.model.TamperGuard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Tier A blocking (PLAN.md §3): when a blocked app comes to the foreground while the
 * phone is locked, go Home and show [BlockedActivity] explaining why it was closed.
 * While locked it also closes Settings screens that could switch Live Free off.
 *
 * It never acts while the phone's own screen lock is showing or the screen is off:
 * the lock screen, its PIN pad and fingerprint/face prompts are off limits, and
 * pressing Home there could dismiss them.
 */
class BlockerService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Written by the collector and read by onAccessibilityEvent, both on the main thread.
    private var state: LockState = LockState.Unlocked
    private var mode: Mode? = null
    private var exempt: Set<String> = EssentialApps.KNOWN.keys

    /** The app most recently seen coming to the foreground. */
    private var foregroundPackage: String? = null
    private val messages = MessageThrottle(clock = SystemClock::elapsedRealtime)
    private val handler = Handler(Looper.getMainLooper())
    private var lastTamperCheckAt = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                // Drop any pending re-checks so nothing fires on the lock screen.
                Intent.ACTION_SCREEN_OFF -> cancelPendingActions()
                // Just unlocked: check whatever app is now in front (it may have been
                // open before the phone was locked, so no new window event arrives).
                Intent.ACTION_USER_PRESENT -> handler.postDelayed(::checkForegroundAfterUnlock, UNLOCK_CHECK_DELAY_MS)
            }
        }
    }

    override fun onServiceConnected() {
        exempt = ExemptApps.load(this).keys
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        val data = LiveFreeData.get(this)
        scope.launch {
            combine(data.lock.state, data.modes.observe()) { s, m -> s to m }
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
                blockIfNeeded(packageName, isNewOpen = true)
                guardSettings(packageName, force = true)
            }
            // Settings often swaps pages without a new window, so watch its content too.
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> guardSettings(packageName, force = false)
            else -> Unit
        }
    }

    /**
     * While locked, close Settings or uninstaller screens that could switch Live Free
     * off. Screen text is only read here, for those packages, while locked.
     */
    private fun guardSettings(packageName: String, force: Boolean) {
        if (packageName !in TamperGuard.GUARDED_PACKAGES) return
        if (isScreenLocked()) return
        val locked = state as? LockState.Locked ?: return
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
        if (isScreenLocked()) return
        val text = collectText(root)
        if (TamperGuard.isTamperScreen(packageName, text, appLabel(), getString(R.string.blocker_service_label))) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            startActivity(BlockedActivity.tamperIntent(this, locked.since))
        }
    }

    /** The phone's own lock screen is up (or the screen is off). */
    private fun isScreenLocked(): Boolean {
        val keyguardLocked = getSystemService(KeyguardManager::class.java)?.isKeyguardLocked ?: true
        val screenOn = getSystemService(PowerManager::class.java)?.isInteractive ?: false
        return keyguardLocked || !screenOn
    }

    private fun cancelPendingActions() {
        handler.removeCallbacksAndMessages(RECHECK_TOKEN)
        handler.removeCallbacksAndMessages(TAMPER_TOKEN)
    }

    private fun checkForegroundAfterUnlock() {
        val packageName = rootInActiveWindow?.packageName?.toString() ?: return
        foregroundPackage = packageName
        blockIfNeeded(packageName, isNewOpen = true)
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
    private fun blockIfNeeded(packageName: String, isNewOpen: Boolean) {
        val mode = mode ?: return
        val locked = state as? LockState.Locked ?: return
        if (!BlockPolicy.shouldBlock(packageName, locked, mode, exempt, screenLocked = isScreenLocked())) return

        performGlobalAction(GLOBAL_ACTION_HOME)
        startActivity(BlockedActivity.intent(this, packageName, mode.listType, locked.since))
        if (isNewOpen) scope.launch { LiveFreeData.get(this@BlockerService).stats.recordBlockedOpen(packageName) }
        if (messages.shouldShow(packageName)) {
            // Some phones stop background services from opening screens; the toast still explains.
            val label = AppLabels.of(this, packageName)
            Toast.makeText(this, "Live Free closed $label: your phone is locked", Toast.LENGTH_LONG).show()
        }

        // If the app somehow ends up in front again without a new window event
        // (e.g. a fast relaunch racing the Home action), close it again. Replacing any
        // pending re-checks keeps at most a couple queued.
        handler.removeCallbacksAndMessages(RECHECK_TOKEN)
        RECHECK_DELAYS_MS.forEach { delay ->
            HandlerCompat.postDelayed(
                handler,
                { if (foregroundPackage == packageName) blockIfNeeded(packageName, isNewOpen = false) },
                RECHECK_TOKEN,
                delay,
            )
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
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
        const val UNLOCK_CHECK_DELAY_MS = 400L
    }
}
