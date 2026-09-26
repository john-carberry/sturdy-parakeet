package com.livefree.core.model

/**
 * Spots Settings and uninstaller screens that could switch Live Free off while the
 * phone is locked: its App info (force stop, uninstall), its accessibility switch,
 * its device-admin page, and the uninstall dialog. Matches on-screen text, so it
 * only knows English wording for the Settings buttons.
 */
object TamperGuard {
    const val SETTINGS = "com.android.settings"

    private val UNINSTALLERS = setOf(
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.samsung.android.packageinstaller",
    )

    val GUARDED_PACKAGES: Set<String> = UNINSTALLERS + SETTINGS

    private val DANGER_WORDS = listOf("uninstall", "force stop", "deactivate", "clear storage", "clear data")

    fun isTamperScreen(
        packageName: String,
        screenText: List<String>,
        /** This app's name, e.g. "Live Free". */
        appLabel: String,
        /** The accessibility service's name, e.g. "Live Free app blocker". */
        serviceLabel: String,
    ): Boolean {
        if (packageName !in GUARDED_PACKAGES) return false
        val text = screenText.joinToString("\n").lowercase()
        if (appLabel.lowercase() !in text) return false
        // An uninstaller showing our name can only be about removing us.
        if (packageName in UNINSTALLERS) return true
        if (serviceLabel.lowercase() in text) return true
        return DANGER_WORDS.any { it in text }
    }
}
