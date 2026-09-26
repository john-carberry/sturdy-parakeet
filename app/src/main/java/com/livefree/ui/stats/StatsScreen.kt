package com.livefree.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livefree.core.model.LockStats
import com.livefree.core.ui.MonoLabel
import com.livefree.core.ui.SpaceGrotesk
import com.livefree.ui.components.BackTopBar
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatsScreen(viewModel: StatsViewModel, onBack: () -> Unit) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    StatsContent(stats, onBack)
}

@Composable
fun StatsContent(stats: LockStats?, onBack: () -> Unit = {}) {
    Scaffold(topBar = { BackTopBar("Stats", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val s = stats ?: return@Column
            // The one hero figure for this view, in the reading sans (not the display face).
            Column {
                MonoLabel("Locked · last 7 days")
                Text(
                    formatDuration(s.totalLockedMs),
                    style = MaterialTheme.typography.displayMedium.copy(fontFamily = SpaceGrotesk),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Streak", "${s.streakDays} day${if (s.streakDays == 1) "" else "s"}", Modifier.weight(1f))
                StatTile("Longest", formatDuration(s.longestSessionMs), Modifier.weight(1f))
                StatTile("Blocked", "%,d".format(s.totalBlockedOpens), Modifier.weight(1f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MonoLabel("Hours locked per day")
                Text(
                    "Tap a day to see its total.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WeekChart(s.days)
            }
            DayTable(s)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MonoLabel(label)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** The table view: every day's values, so nothing depends on reading the chart. */
@Composable
private fun DayTable(stats: LockStats) {
    val locale = Locale.getDefault()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TableRow("Day", "Locked", "Blocked", header = true)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        stats.days.asReversed().forEach { day ->
            TableRow(
                day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
                formatDuration(day.lockedMs),
                "%,d".format(day.blockedOpens),
            )
        }
    }
}

@Composable
private fun TableRow(day: String, locked: String, stopped: String, header: Boolean = false) {
    val style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
    val color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(modifier = Modifier.fillMaxWidth()) {
        val show = { text: String -> if (header) text.uppercase() else text }
        Text(show(day), style = style, color = color, modifier = Modifier.weight(1.4f))
        Text(show(locked), style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text(show(stopped), style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}
