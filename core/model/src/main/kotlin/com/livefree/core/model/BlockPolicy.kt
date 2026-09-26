package com.livefree.core.model

/** Decides whether an app that just came to the foreground should be blocked. */
object BlockPolicy {
    fun shouldBlock(
        packageName: String,
        state: LockState,
        mode: Mode,
        /** Apps that must always work: this app, the launcher, dialer, keyboard, system UI. */
        exempt: Set<String>,
    ): Boolean = state is LockState.Locked &&
        packageName !in exempt &&
        mode.isBlocked(packageName)
}
