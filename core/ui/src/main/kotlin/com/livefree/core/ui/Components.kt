package com.livefree.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Small mono uppercase label, e.g. "STATUS". */
@Composable
fun MonoLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = color, modifier = modifier)
}

/** Flat card: surface fill with a hairline outline instead of a shadow. */
@Composable
fun FlatCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = color,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

/** A filled or hollow dot: the minimalist replacement for checkmarks and status emoji. */
@Composable
fun StatusDot(filled: Boolean, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Canvas(modifier.size(12.dp)) {
        val r = size.minDimension / 2
        if (filled) drawCircle(color, r) else drawCircle(color, r - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))
    }
}

/** A row in a minimal menu: label on the left, an arrow on the right, hairline below. */
@Composable
fun MenuRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, detail: String? = null) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                detail?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text("→", style = MaterialTheme.typography.titleMedium)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Retro graph-paper dots behind content. */
fun Modifier.dotGrid(color: Color, spacing: Dp = 14.dp, radius: Dp = 1.dp): Modifier = drawBehind {
    val step = spacing.toPx()
    val r = radius.toPx()
    var y = step / 2
    while (y < size.height) {
        var x = step / 2
        while (x < size.width) {
            drawCircle(color, r, Offset(x, y))
            x += step
        }
        y += step
    }
}
