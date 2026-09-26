package com.diybrick.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diybrick.core.model.BrickStats
import com.diybrick.ui.components.BackTopBar
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatsScreen(viewModel: StatsViewModel, onBack: () -> Unit) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar("Your stats", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val s = stats ?: return@Column
            // The one hero figure for this view.
            Column {
                Text("Bricked in the last 7 days", style = MaterialTheme.typography.titleMedium)
                Text(
                    formatDuration(s.totalBrickedMs),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Streak", "${s.streakDays} day${if (s.streakDays == 1) "" else "s"}", Modifier.weight(1f))
                StatTile("Longest brick", formatDuration(s.longestSessionMs), Modifier.weight(1f))
                StatTile("Apps stopped", "%,d".format(s.totalBlockedOpens), Modifier.weight(1f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Hours bricked per day", style = MaterialTheme.typography.titleMedium)
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
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** The table view: every day's values, so nothing depends on reading the chart. */
@Composable
private fun DayTable(stats: BrickStats) {
    val locale = Locale.getDefault()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TableRow("Day", "Bricked", "Apps stopped", header = true)
        HorizontalDivider()
        stats.days.asReversed().forEach { day ->
            TableRow(
                day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
                formatDuration(day.brickedMs),
                "%,d".format(day.blockedOpens),
            )
        }
    }
}

@Composable
private fun TableRow(day: String, bricked: String, stopped: String, header: Boolean = false) {
    val style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
    val color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(day, style = style, color = color, modifier = Modifier.weight(1.4f))
        Text(bricked, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text(stopped, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}
