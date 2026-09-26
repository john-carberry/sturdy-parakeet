package com.livefree.feature.blocker

/**
 * Limits how often the "app was closed" toast shows for the same app, so spamming
 * an app doesn't queue up a pile of toasts. Blocking itself is never throttled.
 */
internal class MessageThrottle(
    private val clock: () -> Long,
    private val quietMs: Long = QUIET_MS,
) {
    private var lastPackage: String? = null
    private var lastShownAt = 0L

    fun shouldShow(packageName: String): Boolean {
        val now = clock()
        if (packageName == lastPackage && now - lastShownAt < quietMs) return false
        lastPackage = packageName
        lastShownAt = now
        return true
    }

    companion object {
        const val QUIET_MS = 4_000L
    }
}
