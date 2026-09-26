package com.livefree.ui.scan

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.livefree.feature.nfc.NfcStatus
import com.livefree.feature.nfc.OnNfcTag
import com.livefree.feature.nfc.rememberNfcStatus
import com.livefree.ui.components.BackTopBar

/** Waits for a card tap. Calls [onTapped] once, with the first card's UID. */
@Composable
fun TapCardScreen(title: String, onTapped: (ByteArray) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val nfcStatus = rememberNfcStatus()
    var handled by remember { mutableStateOf(false) }
    OnNfcTag { uid ->
        if (!handled) {
            handled = true
            onTapped(uid)
        }
    }

    Scaffold(topBar = { BackTopBar(title, onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (nfcStatus) {
                NfcStatus.READY -> {
                    Text("📇", style = MaterialTheme.typography.displayLarge)
                    Text(
                        "Hold your card flat against the back of your phone",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "The reader is usually near the camera. Keep the card still until the phone vibrates.",
                        textAlign = TextAlign.Center,
                    )
                }
                NfcStatus.DISABLED -> {
                    Text("NFC is turned off.", style = MaterialTheme.typography.headlineSmall)
                    Button(onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }) {
                        Text("Open NFC settings")
                    }
                }
                NfcStatus.NOT_SUPPORTED -> Text(
                    "This phone doesn't have NFC. Use a QR code key instead.",
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
