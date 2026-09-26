package com.livefree.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.livefree.core.model.DayStat
import com.livefree.core.ui.chartSeriesColor
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil

private const val HOUR_MS = 3_600_000L

/**
 * Hours locked per day: one series, one hue, columns from a single baseline with
 * 4dp rounded tops. Only the selected day (today by default, or the one tapped) is
 * labelled; the table under the chart carries every value.
 */
@Composable
fun WeekChart(days: List<DayStat>, modifier: Modifier = Modifier) {
    if (days.isEmpty()) return
    var selected by remember(days.size) { mutableIntStateOf(days.lastIndex) }
    val barColor = chartSeriesColor()
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val valueColor = MaterialTheme.colorScheme.onSurface
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val valueStyle = MaterialTheme.typography.labelMedium.copy(color = valueColor)
    val measurer = rememberTextMeasurer()
    val locale = Locale.getDefault()

    // Clean top tick: whole hours, at least 1h.
    val topHours = ceil(days.maxOf { it.lockedMs } / HOUR_MS.toDouble()).toInt().coerceAtLeast(1)
    val summary = days.joinToString { day ->
        "${day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)} ${formatDuration(day.lockedMs)}"
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .semantics { contentDescription = "Hours locked per day: $summary" }
            .pointerInput(days) {
                detectTapGestures { offset ->
                    val slot = size.width / days.size.toFloat()
                    selected = (offset.x / slot).toInt().coerceIn(0, days.lastIndex)
                }
            },
    ) {
        val dayLabelHeight = 20.dp.toPx()
        val valueLabelHeight = 20.dp.toPx()
        val plotTop = valueLabelHeight
        val baseline = size.height - dayLabelHeight
        val plotHeight = baseline - plotTop
        val slot = size.width / days.size
        val barWidth = minOf(24.dp.toPx(), slot * 0.6f)
        val corner = CornerRadius(4.dp.toPx())
        val hairline = 1.dp.toPx()

        // Recessive axes: a top gridline with its tick label, and the baseline.
        drawLine(axisColor, Offset(0f, plotTop), Offset(size.width, plotTop), hairline)
        drawLine(axisColor, Offset(0f, baseline), Offset(size.width, baseline), hairline)
        drawText(measurer, "${topHours}h", Offset(0f, plotTop - valueLabelHeight + 2.dp.toPx()), labelStyle)

        days.forEachIndexed { i, day ->
            val centerX = slot * i + slot / 2
            val barHeight = plotHeight * (day.lockedMs.toFloat() / (topHours * HOUR_MS))
            if (barHeight > 0f) {
                val left = centerX - barWidth / 2
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = left,
                            top = baseline - barHeight,
                            right = left + barWidth,
                            bottom = baseline,
                            topLeftCornerRadius = corner,
                            topRightCornerRadius = corner,
                            bottomRightCornerRadius = CornerRadius.Zero,
                            bottomLeftCornerRadius = CornerRadius.Zero,
                        ),
                    )
                }
                drawPath(path, barColor)
            }

            val dayName = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            val dayText = measurer.measure(dayName, labelStyle)
            drawText(dayText, topLeft = Offset(centerX - dayText.size.width / 2f, baseline + 4.dp.toPx()))

            if (i == selected) {
                val valueText = measurer.measure(formatDuration(day.lockedMs), valueStyle)
                val x = (centerX - valueText.size.width / 2f).coerceIn(0f, size.width - valueText.size.width)
                val y = (baseline - barHeight - valueText.size.height - 2.dp.toPx()).coerceAtLeast(0f)
                drawText(valueText, topLeft = Offset(x, y))
            }
        }
    }
}
