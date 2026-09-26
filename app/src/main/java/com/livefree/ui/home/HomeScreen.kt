package com.livefree.ui.home

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
import androidx.compose.runtime.mutableLongStateOf
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
import com.livefree.core.model.LockRules
import com.livefree.core.model.LockSettings
import com.livefree.core.model.LockState
import com.livefree.core.model.KeyType
import com.livefree.core.model.ListType
import com.livefree.core.model.Mode
import com.livefree.feature.blocker.rememberAdminActive
import com.livefree.feature.blocker.rememberBlockerEnabled
import com.livefree.feature.nfc.NfcStatus
import com.livefree.feature.nfc.OnNfcTag
import com.livefree.feature.nfc.rememberNfcStatus
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

/**
 * Home screen. Tapping a paired card anywhere in the app, or scanning a paired QR
 * code, locks the phone when unlocked and unlocks it when locked.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onScanQr: () -> Unit,
    onTapCard: () -> Unit,
    onAddCard: () -> Unit,
    onManageKeys: () -> Unit,
    onChooseApps: () -> Unit,
    onSetUpBlocker: () -> Unit,
    onStats: () -> Unit,
    onSetupChecklist: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyCount by viewModel.keyCount.collectAsStateWithLifecycle()
    val keyTypes by viewModel.keyTypes.collectAsStateWithLifecycle()
    val emergencyLeft by viewModel.emergencyUnlocksRemaining.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val blockerEnabled = rememberBlockerEnabled()
    val adminActive = rememberAdminActive()
    val nfcStatus = rememberNfcStatus()
    val context = LocalContext.current
    var confirmEmergency by remember { mutableStateOf(false) }
    OnNfcTag(viewModel::onNfcTag)

    val locked = state is LockState.Locked
    RequestNotificationPermissionWhen(locked)

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
            state?.let { StatusCard(it, mode) }
            viewModel.notice?.let { NoticeCard(it) }
            when {
                !blockerEnabled -> BlockerOffCard(onSetUpBlocker)
                !adminActive -> TextButton(onClick = onSetUpBlocker) {
                    Text("Recommended: turn on uninstall protection")
                }
            }

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
                    val action = if (locked) "unlock" else "lock"
                    val hasCard = keyTypes.any { it == KeyType.NFC_UID || it == KeyType.NFC_NDEF }
                    when {
                        nfcStatus == NfcStatus.NOT_SUPPORTED -> Unit
                        hasCard -> Button(onClick = onTapCard, modifier = Modifier.fillMaxWidth()) {
                            Text("📇 Tap card to $action")
                        }
                        !locked -> OutlinedButton(onClick = onAddCard, modifier = Modifier.fillMaxWidth()) {
                            Text("📇 Add a card to lock and unlock with")
                        }
                    }
                    if (nfcStatus == NfcStatus.DISABLED && hasCard) {
                        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }) {
                            Text("NFC is off. Turn it on to use your card.")
                        }
                    }
                    if (KeyType.QR in keyTypes) {
                        Button(onClick = onScanQr, modifier = Modifier.fillMaxWidth()) {
                            Text("🔳 Scan QR key to $action")
                        }
                    }
                    OutlinedButton(onClick = onChooseApps, modifier = Modifier.fillMaxWidth()) {
                        Text("Choose apps to block")
                    }
                    OutlinedButton(onClick = onManageKeys, modifier = Modifier.fillMaxWidth()) {
                        Text("Manage keys")
                    }
                    OutlinedButton(onClick = onStats, modifier = Modifier.fillMaxWidth()) {
                        Text("Your stats")
                    }
                    TextButton(onClick = onSetupChecklist) { Text("Setup checklist") }
                }
            }

            (state as? LockState.Locked)?.let { current ->
                EmergencySection(
                    requestedAt = current.emergencyRequestedAt,
                    remaining = emergencyLeft,
                    settings = viewModel.settings,
                    onStart = { confirmEmergency = true },
                    onUse = viewModel::emergencyUnlock,
                    onCancel = viewModel::cancelEmergencyUnlock,
                )
            }
        }
    }

    if (confirmEmergency) {
        EmergencyDialog(
            remaining = emergencyLeft ?: 0,
            settings = viewModel.settings,
            onConfirm = {
                viewModel.requestEmergencyUnlock()
                confirmEmergency = false
            },
            onDismiss = { confirmEmergency = false },
        )
    }
}

@Composable
private fun StatusCard(state: LockState, mode: Mode?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp)) {
            when (state) {
                LockState.Unlocked -> {
                    Text("🔓 Unlocked", style = MaterialTheme.typography.headlineLarge)
                    Text("Tap your key to lock your phone.", style = MaterialTheme.typography.bodyMedium)
                }
                is LockState.Locked -> {
                    val since = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(state.since))
                    Text("🔒 Locked", style = MaterialTheme.typography.headlineLarge)
                    Text("Since $since", style = MaterialTheme.typography.bodyMedium)
                }
            }
            mode?.let { Text(summary(it), style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private fun summary(mode: Mode): String {
    val n = mode.packages.size
    val apps = "$n app${if (n == 1) "" else "s"}"
    return when (mode.listType) {
        ListType.BLOCK -> if (n == 0) "No apps chosen to block yet." else "Blocks $apps."
        ListType.ALLOW -> "Blocks everything except $apps and essentials like calls, messages and alarms."
    }
}

@Composable
private fun BlockerOffCard(onSetUp: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("App blocking is off", style = MaterialTheme.typography.titleMedium)
            Text("Locking won't block anything until you turn it on.")
            Button(onClick = onSetUp) { Text("Turn on") }
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
private fun EmergencyDialog(
    remaining: Int,
    settings: LockSettings,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Emergency unlock?") },
        text = {
            Text(
                if (remaining > 0) {
                    "This starts a ${settings.emergencyWaitMinutes}-minute wait. After that you'll have " +
                        "${settings.emergencyReadyMinutes} minutes to unlock. You have $remaining left, " +
                        "and they don't come back. Only use one if you really can't get to your key."
                } else {
                    "You've used all your emergency unlocks. You'll need your key."
                },
            )
        },
        confirmButton = {
            if (remaining > 0) {
                TextButton(onClick = onConfirm) { Text("Start the wait") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Emergency unlock controls: start, count down, then use within the ready window. */
@Composable
private fun EmergencySection(
    requestedAt: Long?,
    remaining: Int?,
    settings: LockSettings,
    onStart: () -> Unit,
    onUse: () -> Unit,
    onCancel: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(requestedAt) {
        while (requestedAt != null) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    when (val status = LockRules.emergencyStatus(requestedAt, now, settings)) {
        LockRules.EmergencyStatus.NotRequested -> TextButton(onClick = onStart) {
            Text("Emergency unlock (${remaining ?: "…"} left)")
        }
        is LockRules.EmergencyStatus.Waiting -> Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Emergency unlock in ${countdown(status.msLeft)}", style = MaterialTheme.typography.titleMedium)
                Text("Tapping your key still unlocks straight away.")
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        }
        is LockRules.EmergencyStatus.Ready -> Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Emergency unlock ready", style = MaterialTheme.typography.titleMedium)
                Text("Use it in the next ${countdown(status.msLeft)} or it lapses.")
                Button(onClick = onUse, modifier = Modifier.fillMaxWidth()) {
                    Text("Unlock now (uses 1 of ${remaining ?: "…"})")
                }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    }
}

private fun countdown(ms: Long): String {
    val totalSeconds = (ms + 999) / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

/** Asks once for notification permission (Android 13+) so the "Locked" notification shows. */
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
