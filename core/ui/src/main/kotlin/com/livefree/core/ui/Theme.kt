package com.livefree.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Accent = Color(0xFFB5462F)
private val AccentLight = Color(0xFFFFB4A2)

@Composable
fun LiveFreeTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = AccentLight)
    } else {
        lightColorScheme(primary = Accent)
    }
    MaterialTheme(colorScheme = colors, content = content)
}

/**
 * The one hue for single-series charts. Validated with the dataviz palette checks
 * against each theme's surface; the dark theme's primary is too pale for marks.
 */
@Composable
fun chartSeriesColor(): Color = if (isSystemInDarkTheme()) Color(0xFFE06A4F) else Color(0xFFB5462F)
