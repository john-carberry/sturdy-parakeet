package com.diybrick.core.model

/** Decides whether an app that just came to the foreground should be blocked. */
object BlockPolicy {
    fun shouldBlock(
        packageName: String,
        state: BrickState,
        mode: Mode,
        /** Apps that must always work: this app, the launcher, dialer, keyboard, system UI. */
        exempt: Set<String>,
    ): Boolean = state is BrickState.Bricked &&
        packageName !in exempt &&
        mode.isBlocked(packageName)
}
