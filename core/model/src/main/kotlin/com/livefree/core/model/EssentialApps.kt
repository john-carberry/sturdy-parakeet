package com.livefree.core.model

/** Why an app can never be blocked: the phone has to keep working while locked. */
enum class EssentialReason(val description: String) {
    THIS_APP("Needed to unlock"),
    SECURITY("Screen lock, fingerprint and face unlock"),
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
        // The phone's own lock screen and biometric prompts, which some makers ship as
        // separate apps. Live Free must never touch these.
        "com.android.keyguard" to EssentialReason.SECURITY,
        "com.android.systemui.biometrics" to EssentialReason.SECURITY,
        "com.google.android.settings.intelligence" to EssentialReason.SECURITY,
        "com.google.android.apps.faceunlock" to EssentialReason.SECURITY,
        "com.samsung.android.biometrics.app.setting" to EssentialReason.SECURITY,
        "com.samsung.android.bio.face.service" to EssentialReason.SECURITY,
        "com.samsung.android.app.aodservice" to EssentialReason.SECURITY,
        "com.samsung.android.dynamiclock" to EssentialReason.SECURITY,
        "com.miui.face" to EssentialReason.SECURITY,
        "com.miui.securitycenter" to EssentialReason.SECURITY,
        "com.oneplus.faceunlock" to EssentialReason.SECURITY,
        "com.oplus.faceunlock" to EssentialReason.SECURITY,
        "com.coloros.fingerprint" to EssentialReason.SECURITY,
        "com.motorola.faceunlock" to EssentialReason.SECURITY,
        "com.huawei.systemmanager" to EssentialReason.SECURITY,
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
