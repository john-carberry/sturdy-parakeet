package com.livefree.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.NightMode
import com.livefree.core.model.KeyType
import com.livefree.core.model.ListType
import com.livefree.core.model.LockState
import com.livefree.core.model.Mode
import com.livefree.core.ui.LiveFreeTheme
import com.livefree.feature.nfc.NfcStatus
import com.livefree.ui.home.HomeActions
import com.livefree.ui.home.HomeContent
import com.livefree.ui.home.HomeUi
import com.livefree.ui.home.Notice
import org.junit.Rule
import org.junit.Test

/** Renders screens on the JVM (no phone needed) so the look can be checked by eye. */
class HomeScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6, maxPercentDifference = 0.1)

    private val mode = Mode(Mode.DEFAULT_ID, "Focus", ListType.BLOCK, setOf("a", "b", "c", "d"))
    private val unlocked = HomeUi(
        state = LockState.Unlocked,
        keyCount = 2,
        keyTypes = setOf(KeyType.NFC_UID, KeyType.QR),
        emergencyLeft = 5,
        mode = mode,
        notice = null,
        blockerEnabled = true,
        adminActive = true,
        nfcStatus = NfcStatus.READY,
    )
    private val locked = unlocked.copy(
        state = LockState.Locked(Mode.DEFAULT_ID, since = 1_758_870_000_000L),
        notice = Notice("Locked with “Hotel card”."),
        emergencyLeft = 4,
    )

    @Test
    fun unlockedLight() = paparazzi.snapshot {
        LiveFreeTheme(darkTheme = false) { HomeContent(unlocked, HomeActions()) }
    }

    @Test
    fun lockedLight() = paparazzi.snapshot { LiveFreeTheme(darkTheme = false) { HomeContent(locked, HomeActions()) } }

    @Test
    fun lockedDark() {
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_6.copy(nightMode = NightMode.NIGHT))
        paparazzi.snapshot { LiveFreeTheme(darkTheme = true) { HomeContent(locked, HomeActions()) } }
    }
}
