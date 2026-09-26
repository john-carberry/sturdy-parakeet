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
