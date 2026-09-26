package com.livefree.core.model

/** Why an app can never be blocked: the phone has to keep working while locked. */
enum class EssentialReason(val description: String) {
    THIS_APP("Needed to unlock"),
    PHONE("Calls always work"),
    MESSAGES("Texts and verification codes"),
    CONTACTS("Needed for calls and texts"),
    ALARMS("Alarms can always be dismissed"),
    KEYBOARD("Needed to type"),
    HOME_SCREEN("Your home screen"),
    SETTINGS("Phone settings"),
    ACCESSIBILITY("Accessibility tools you use"),
    EMERGENCY("Emergency and safety"),
    SYSTEM("Part of Android"),
}

object EssentialApps {
    /**
     * Well-known essential packages. The phone's actual defaults (dialer, SMS app,
     * launcher, keyboard, alarm app, ...) are detected on the device and added to these.
     */
    val KNOWN: Map<String, EssentialReason> = mapOf(
        "android" to EssentialReason.SYSTEM,
        "com.android.systemui" to EssentialReason.SYSTEM,
        "com.android.permissioncontroller" to EssentialReason.SYSTEM,
        "com.google.android.permissioncontroller" to EssentialReason.SYSTEM,
        "com.android.settings" to EssentialReason.SETTINGS,
        "com.android.phone" to EssentialReason.PHONE,
        "com.android.server.telecom" to EssentialReason.PHONE,
        "com.android.dialer" to EssentialReason.PHONE,
        "com.google.android.dialer" to EssentialReason.PHONE,
        "com.samsung.android.dialer" to EssentialReason.PHONE,
        "com.android.incallui" to EssentialReason.PHONE,
        "com.samsung.android.incallui" to EssentialReason.PHONE,
        "com.android.mms" to EssentialReason.MESSAGES,
        "com.google.android.apps.messaging" to EssentialReason.MESSAGES,
        "com.samsung.android.messaging" to EssentialReason.MESSAGES,
        "com.android.contacts" to EssentialReason.CONTACTS,
        "com.google.android.contacts" to EssentialReason.CONTACTS,
        "com.samsung.android.app.contacts" to EssentialReason.CONTACTS,
        "com.android.deskclock" to EssentialReason.ALARMS,
        "com.google.android.deskclock" to EssentialReason.ALARMS,
        "com.sec.android.app.clockpackage" to EssentialReason.ALARMS,
        "com.android.emergency" to EssentialReason.EMERGENCY,
        "com.google.android.apps.safetyhub" to EssentialReason.EMERGENCY,
        "com.android.cellbroadcastreceiver" to EssentialReason.EMERGENCY,
        "com.google.android.cellbroadcastreceiver" to EssentialReason.EMERGENCY,
    )

    /** Drops essential apps from a mode's list, so they can't be saved as blocked. */
    fun sanitize(mode: Mode, essential: Set<String>): Mode =
        mode.copy(packages = mode.packages - essential)
}
