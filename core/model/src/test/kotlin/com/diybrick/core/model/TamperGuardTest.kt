package com.diybrick.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TamperGuardTest {
    private fun check(packageName: String, vararg text: String) =
        TamperGuard.isTamperScreen(packageName, text.toList(), "DIY Brick", "DIY Brick app blocker")

    @Test
    fun appInfoForThisAppIsGuarded() {
        assertTrue(check("com.android.settings", "App info", "DIY Brick", "Open", "Uninstall", "Force stop"))
    }

    @Test
    fun accessibilitySwitchIsGuarded() {
        assertTrue(check("com.android.settings", "DIY Brick app blocker", "Use DIY Brick app blocker"))
    }

    @Test
    fun deviceAdminPageIsGuarded() {
        assertTrue(check("com.android.settings", "DIY Brick", "Deactivate this device admin app"))
    }

    @Test
    fun uninstallDialogIsGuarded() {
        assertTrue(check("com.google.android.packageinstaller", "DIY Brick", "Do you want to uninstall this app?"))
    }

    @Test
    fun otherAppsInfoIsNotGuarded() {
        assertFalse(check("com.android.settings", "App info", "Instagram", "Uninstall", "Force stop"))
    }

    @Test
    fun harmlessPagesMentioningUsAreNotGuarded() {
        assertFalse(check("com.android.settings", "Notifications", "DIY Brick", "Brick status"))
    }

    @Test
    fun otherPackagesAreNeverGuarded() {
        assertFalse(check("com.example.notes", "DIY Brick", "Uninstall"))
    }
}
