package com.diybrick.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diybrick.core.model.BrickSettings
import com.diybrick.feature.nfc.NfcStatus
import com.diybrick.feature.nfc.OnNfcTag
import com.diybrick.feature.nfc.rememberNfcStatus

/**
 * Home screen. Until bricking arrives in M2, tapping a card or scanning a QR code
 * here just checks whether it's a paired key.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onScanQr: () -> Unit,
    onManageKeys: () -> Unit,
) {
    val keyCount by viewModel.keyCount.collectAsStateWithLifecycle()
    val nfcStatus = rememberNfcStatus()
    val context = LocalContext.current
    OnNfcTag(viewModel::onNfcTag)

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
                    Text("🟢 Free", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        text = "Emergency unbricks left: ${BrickSettings().emergencyUnbricksRemaining}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            viewModel.lastCheck?.let { CheckResultCard(it) }

            when (keyCount) {
                null -> Unit
                0 -> {
                    Text(
                        "Pair an old NFC card or a printed QR code to use as your key.",
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = onManageKeys, modifier = Modifier.fillMaxWidth()) {
                        Text("Pair your first key")
                    }
                }
                else -> {
                    when (nfcStatus) {
                        NfcStatus.READY -> Text(
                            "Hold a paired card to the back of your phone to check it.",
                            textAlign = TextAlign.Center,
                        )
                        NfcStatus.DISABLED -> TextButton(
                            onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                        ) { Text("NFC is off. Turn it on to use card keys.") }
                        NfcStatus.NOT_SUPPORTED -> Unit
                    }
                    Button(onClick = onScanQr, modifier = Modifier.fillMaxWidth()) {
                        Text("Scan QR key")
                    }
                    OutlinedButton(onClick = onManageKeys, modifier = Modifier.fillMaxWidth()) {
                        Text("Manage keys")
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckResultCard(check: KeyCheck) {
    val (text, colors) = when (check) {
        is KeyCheck.Recognised -> "✓ Recognised: ${check.label}" to
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        is KeyCheck.NotRecognised -> "✗ ${check.message}" to
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    }
    Card(modifier = Modifier.fillMaxWidth(), colors = colors) {
        Text(text, modifier = Modifier.padding(16.dp))
    }
}
