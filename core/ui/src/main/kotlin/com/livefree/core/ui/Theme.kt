@file:OptIn(ExperimentalTextApi::class)

package com.livefree.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Modern retro minimalism: paper and ink, one signal-orange accent reserved for
 * "locked", dot-matrix display type, mono labels, flat outlined surfaces and pills.
 */

/** Raw palette. Screens use MaterialTheme.colorScheme or [LiveFreeTheme.signal], not these. */
private object Palette {
    val Paper = Color(0xFFF3F0E8)
    val PaperDeep = Color(0xFFE9E5DA)
    val Ink = Color(0xFF141414)
    val InkSoft = Color(0xFF5C5A55)
    val Line = Color(0xFFD6D1C4)

    val Night = Color(0xFF0F0F0F)
    val NightRaised = Color(0xFF1B1B1B)
    val Cream = Color(0xFFEDEAE2)
    val CreamSoft = Color(0xFFA09C93)
    val NightLine = Color(0xFF2E2E2E)

    // Validated with the dataviz palette checks against Paper and Night.
    val SignalLight = Color(0xFFD9431A)
    val SignalDark = Color(0xFFE85A2C)
    val ErrorLight = Color(0xFFB3261E)
    val ErrorDark = Color(0xFFF2B8B5)
}

private val LightColors = lightColorScheme(
    primary = Palette.Ink,
    onPrimary = Palette.Paper,
    primaryContainer = Palette.PaperDeep,
    onPrimaryContainer = Palette.Ink,
    secondary = Palette.SignalLight,
    onSecondary = Color.White,
    tertiary = Palette.SignalLight,
    background = Palette.Paper,
    onBackground = Palette.Ink,
    surface = Palette.Paper,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.PaperDeep,
    onSurfaceVariant = Palette.InkSoft,
    surfaceContainerLowest = Palette.Paper,
    surfaceContainerLow = Palette.Paper,
    surfaceContainer = Palette.Paper,
    surfaceContainerHigh = Palette.PaperDeep,
    surfaceContainerHighest = Palette.PaperDeep,
    outline = Palette.Ink,
    outlineVariant = Palette.Line,
    error = Palette.ErrorLight,
    errorContainer = Color(0xFFF6DDD5),
    onErrorContainer = Color(0xFF5A1409),
)

private val DarkColors = darkColorScheme(
    primary = Palette.Cream,
    onPrimary = Palette.Night,
    primaryContainer = Palette.NightRaised,
    onPrimaryContainer = Palette.Cream,
    secondary = Palette.SignalDark,
    onSecondary = Palette.Night,
    tertiary = Palette.SignalDark,
    background = Palette.Night,
    onBackground = Palette.Cream,
    surface = Palette.Night,
    onSurface = Palette.Cream,
    surfaceVariant = Palette.NightRaised,
    onSurfaceVariant = Palette.CreamSoft,
    surfaceContainerLowest = Palette.Night,
    surfaceContainerLow = Palette.Night,
    surfaceContainer = Palette.Night,
    surfaceContainerHigh = Palette.NightRaised,
    surfaceContainerHighest = Palette.NightRaised,
    outline = Palette.Cream,
    outlineVariant = Palette.NightLine,
    error = Palette.ErrorDark,
    errorContainer = Color(0xFF3A1A12),
    onErrorContainer = Color(0xFFF6DDD5),
)

private fun groteskWeight(weight: FontWeight) =
    Font(R.font.space_grotesk, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

/** Geometric sans for reading. */
val SpaceGrotesk = FontFamily(
    groteskWeight(FontWeight.Normal),
    groteskWeight(FontWeight.Medium),
    groteskWeight(FontWeight.SemiBold),
    groteskWeight(FontWeight.Bold),
)

/** Mono for labels and numbers. */
val SpaceMono = FontFamily(
    Font(R.font.space_mono_regular, FontWeight.Normal),
    Font(R.font.space_mono_bold, FontWeight.Bold),
)

/** Dot-matrix display face, with round dots. */
val Doto = FontFamily(
    Font(
        R.font.doto,
        FontWeight.Black,
        variationSettings = FontVariation.Settings(FontVariation.weight(900), FontVariation.Setting("ROND", 100f)),
    ),
)

private val Base = Typography()

private fun TextStyle.grotesk(weight: FontWeight = FontWeight.Normal) =
    copy(fontFamily = SpaceGrotesk, fontWeight = weight)

private fun TextStyle.mono() = copy(fontFamily = SpaceMono, fontWeight = FontWeight.Normal, letterSpacing = 1.2.sp)

private fun TextStyle.dots() = copy(fontFamily = Doto, fontWeight = FontWeight.Black, letterSpacing = 1.sp)

private val LiveFreeTypography = Typography(
    displayLarge = Base.displayLarge.dots(),
    displayMedium = Base.displayMedium.dots(),
    displaySmall = Base.displaySmall.dots(),
    headlineLarge = Base.headlineLarge.grotesk(FontWeight.Bold),
    headlineMedium = Base.headlineMedium.grotesk(FontWeight.Bold),
    headlineSmall = Base.headlineSmall.grotesk(FontWeight.SemiBold),
    titleLarge = Base.titleLarge.grotesk(FontWeight.SemiBold),
    titleMedium = Base.titleMedium.grotesk(FontWeight.SemiBold),
    titleSmall = Base.titleSmall.grotesk(FontWeight.Medium),
    bodyLarge = Base.bodyLarge.grotesk(),
    bodyMedium = Base.bodyMedium.grotesk(),
    bodySmall = Base.bodySmall.grotesk(),
    labelLarge = Base.labelLarge.grotesk(FontWeight.SemiBold),
    labelMedium = Base.labelMedium.mono(),
    labelSmall = Base.labelSmall.mono(),
)

private val LiveFreeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun LiveFreeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = LiveFreeTypography,
        shapes = LiveFreeShapes,
        content = content,
    )
}

object LiveFreeTheme {
    /** The one accent, reserved for "locked" and the chart's marks. */
    val signal: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.secondary
}

/**
 * The one hue for single-series charts: the signal orange. Validated with the
 * dataviz palette checks against each theme's surface.
 */
@Composable
@ReadOnlyComposable
fun chartSeriesColor(): Color = LiveFreeTheme.signal
