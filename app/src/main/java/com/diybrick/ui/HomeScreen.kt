package com.diybrick.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.diybrick.core.model.BrickSettings
import com.diybrick.core.model.BrickState
import com.diybrick.ui.theme.DiyBrickTheme

/**
 * Home screen skeleton (M0). Key pairing arrives in M1 and blocking in M3,
 * so the key buttons are disabled for now.
 */
@Composable
fun HomeScreen(
    state: BrickState = BrickState.Free,
    settings: BrickSettings = BrickSettings(),
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp)) {
                    Text(
                        text = when (state) {
                            BrickState.Free -> "🟢 Free"
                            is BrickState.Bricked -> "🧱 Bricked"
                        },
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        text = "Emergency unbricks left: ${settings.emergencyUnbricksRemaining}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text("Tap card")
            }
            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text("Scan QR")
            }
            Text(
                text = "Key pairing is coming in the next milestone.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    DiyBrickTheme { HomeScreen() }
}
