package com.livefree.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TamperGuardTest {
    private fun check(packageName: String, vararg text: String) =
        TamperGuard.isTamperScreen(packageName, text.toList(), "Live Free", "Live Free app blocker")

    @Test
    fun appInfoForThisAppIsGuarded() {
        assertTrue(check("com.android.settings", "App info", "Live Free", "Open", "Uninstall", "Force stop"))
    }

    @Test
    fun accessibilitySwitchIsGuarded() {
        assertTrue(check("com.android.settings", "Live Free app blocker", "Use Live Free app blocker"))
    }

    @Test
    fun deviceAdminPageIsGuarded() {
        assertTrue(check("com.android.settings", "Live Free", "Deactivate this device admin app"))
    }

    @Test
    fun uninstallDialogIsGuarded() {
        assertTrue(check("com.google.android.packageinstaller", "Live Free", "Do you want to uninstall this app?"))
    }

    @Test
    fun otherAppsInfoIsNotGuarded() {
        assertFalse(check("com.android.settings", "App info", "Instagram", "Uninstall", "Force stop"))
    }

    @Test
    fun harmlessPagesMentioningUsAreNotGuarded() {
        assertFalse(check("com.android.settings", "Notifications", "Live Free", "Accent status"))
    }

    @Test
    fun otherPackagesAreNeverGuarded() {
        assertFalse(check("com.example.notes", "Live Free", "Uninstall"))
    }
}
