package com.diybrick.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diybrick.core.model.BrickState
import com.diybrick.feature.nfc.NfcStatus
import com.diybrick.feature.nfc.OnNfcTag
import com.diybrick.feature.nfc.rememberNfcStatus
import java.text.DateFormat
import java.util.Date

/**
 * Home screen. Tapping a paired card anywhere in the app, or scanning a paired QR
 * code, bricks the phone when free and unbricks it when bricked.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onScanQr: () -> Unit,
    onManageKeys: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyCount by viewModel.keyCount.collectAsStateWithLifecycle()
    val emergencyLeft by viewModel.emergencyUnbricksRemaining.collectAsStateWithLifecycle()
    val nfcStatus = rememberNfcStatus()
    val context = LocalContext.current
    var confirmEmergency by remember { mutableStateOf(false) }
    OnNfcTag(viewModel::onNfcTag)

    val bricked = state is BrickState.Bricked
    RequestNotificationPermissionWhen(bricked)

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            state?.let { StatusCard(it) }
            viewModel.notice?.let { NoticeCard(it) }

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
                    val action = if (bricked) "unbrick" else "brick"
                    when (nfcStatus) {
                        NfcStatus.READY -> Text(
                            "Hold a paired card to the back of your phone to $action it.",
                            textAlign = TextAlign.Center,
                        )
                        NfcStatus.DISABLED -> TextButton(
                            onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                        ) { Text("NFC is off. Turn it on to use card keys.") }
                        NfcStatus.NOT_SUPPORTED -> Unit
                    }
                    Button(onClick = onScanQr, modifier = Modifier.fillMaxWidth()) {
                        Text("Scan QR key to $action")
                    }
                    OutlinedButton(onClick = onManageKeys, modifier = Modifier.fillMaxWidth()) {
                        Text("Manage keys")
                    }
                }
            }

            if (bricked) {
                TextButton(onClick = { confirmEmergency = true }) {
                    Text("Emergency unbrick (${emergencyLeft ?: "…"} left)")
                }
            }
        }
    }

    if (confirmEmergency) {
        EmergencyDialog(
            remaining = emergencyLeft ?: 0,
            onConfirm = {
                viewModel.emergencyUnbrick()
                confirmEmergency = false
            },
            onDismiss = { confirmEmergency = false },
        )
    }
}

@Composable
private fun StatusCard(state: BrickState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp)) {
            when (state) {
                BrickState.Free -> {
                    Text("🟢 Free", style = MaterialTheme.typography.headlineLarge)
                    Text("Tap your key to brick your phone.", style = MaterialTheme.typography.bodyMedium)
                }
                is BrickState.Bricked -> {
                    val since = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(state.since))
                    Text("🧱 Bricked", style = MaterialTheme.typography.headlineLarge)
                    Text("Since $since", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "App blocking arrives in the next update.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun NoticeCard(notice: Notice) {
    val colors = if (notice.isError) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    }
    Card(modifier = Modifier.fillMaxWidth(), colors = colors) {
        Text(notice.text, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun EmergencyDialog(remaining: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Emergency unbrick?") },
        text = {
            Text(
                if (remaining > 0) {
                    "You have $remaining left, and they don't come back. " +
                        "Only use one if you really can't get to your key."
                } else {
                    "You've used all your emergency unbricks. You'll need your key."
                },
            )
        },
        confirmButton = {
            if (remaining > 0) {
                TextButton(onClick = onConfirm) { Text("Use one") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Asks once for notification permission (Android 13+) so the "Bricked" notification shows. */
@Composable
private fun RequestNotificationPermissionWhen(condition: Boolean) {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(condition) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (condition && !granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
