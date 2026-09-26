package com.diybrick.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Brick = Color(0xFFB5462F)
private val BrickLight = Color(0xFFFFB4A2)

@Composable
fun DiyBrickTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = BrickLight)
    } else {
        lightColorScheme(primary = Brick)
    }
    MaterialTheme(colorScheme = colors, content = content)
}

/**
 * The one hue for single-series charts. Validated with the dataviz palette checks
 * against each theme's surface; the dark theme's primary is too pale for marks.
 */
@Composable
fun chartSeriesColor(): Color = if (isSystemInDarkTheme()) Color(0xFFE06A4F) else Color(0xFFB5462F)
