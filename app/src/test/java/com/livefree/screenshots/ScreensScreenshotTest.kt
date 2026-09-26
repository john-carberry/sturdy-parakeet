package com.livefree.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.livefree.core.model.LockStats
import com.livefree.core.model.SessionSpan
import com.livefree.core.model.StatsCalculator
import com.livefree.core.ui.LiveFreeTheme
import com.livefree.feature.blocker.BlockedScreen
import com.livefree.ui.setup.SetupContent
import com.livefree.ui.setup.SetupSteps
import com.livefree.ui.stats.StatsContent
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ScreensScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6, maxPercentDifference = 0.1)

    private val zone = ZoneId.of("UTC")
    private val hour = 3_600_000L

    private fun at(date: String, hourOfDay: Int) =
        LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli() + hourOfDay * hour

    private val stats: LockStats = StatsCalculator.compute(
        sessions = listOf(
            SessionSpan(at("2026-09-20", 9), at("2026-09-20", 12)),
            SessionSpan(at("2026-09-21", 20), at("2026-09-22", 6)),
            SessionSpan(at("2026-09-23", 9), at("2026-09-23", 11)),
            SessionSpan(at("2026-09-24", 13), at("2026-09-24", 18)),
            SessionSpan(at("2026-09-25", 8), at("2026-09-25", 10)),
            SessionSpan(at("2026-09-26", 9), null),
        ),
        blockedOpenTimes = List(23) { at("2026-09-24", 14) } + List(9) { at("2026-09-26", 10) },
        now = at("2026-09-26", 12),
        zone = zone,
    )

    @Test
    fun statsLight() = paparazzi.snapshot { LiveFreeTheme(darkTheme = false) { StatsContent(stats) } }

    @Test
    fun setupLight() = paparazzi.snapshot {
        LiveFreeTheme(darkTheme = false) {
            SetupContent(SetupSteps(true, true, false, false, false, showNotifications = true))
        }
    }

    @Test
    fun blocked() = paparazzi.snapshot {
        BlockedScreen(
            title = "Instagram was closed",
            explanation = "Your phone has been locked since 9:14 AM, and Instagram is on your list of apps to block.",
            nextStep = "To use it again, tap your key to unlock your phone.",
        )
    }
}
