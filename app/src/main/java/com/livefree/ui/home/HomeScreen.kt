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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livefree.core.model.KeyType
import com.livefree.core.model.ListType
import com.livefree.core.model.LockRules
import com.livefree.core.model.LockSettings
import com.livefree.core.model.LockState
import com.livefree.core.model.Mode
import com.livefree.core.ui.FlatCard
import com.livefree.core.ui.LiveFreeTheme
import com.livefree.core.ui.MenuRow
import com.livefree.core.ui.MonoLabel
import com.livefree.core.ui.StatusDot
import com.livefree.core.ui.dotGrid
import com.livefree.feature.blocker.rememberAdminActive
import com.livefree.feature.blocker.rememberBlockerEnabled
import com.livefree.feature.nfc.NfcStatus
import com.livefree.feature.nfc.OnNfcTag
import com.livefree.feature.nfc.rememberNfcStatus
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

/** Everything the home screen shows, so it can be drawn without a ViewModel (e.g. in screenshots). */
data class HomeUi(
    val state: LockState?,
    val keyCount: Int?,
    val keyTypes: Set<KeyType>,
    val emergencyLeft: Int?,
    val mode: Mode?,
    val notice: Notice?,
    val blockerEnabled: Boolean,
    val adminActive: Boolean,
    val nfcStatus: NfcStatus,
    val settings: LockSettings = LockSettings(),
)

class HomeActions(
    val onScanQr: () -> Unit = {},
    val onTapCard: () -> Unit = {},
    val onAddCard: () -> Unit = {},
    val onManageKeys: () -> Unit = {},
    val onChooseApps: () -> Unit = {},
    val onSetUpBlocker: () -> Unit = {},
    val onStats: () -> Unit = {},
    val onSetupChecklist: () -> Unit = {},
    val onOpenNfcSettings: () -> Unit = {},
    val onRequestEmergency: () -> Unit = {},
    val onUseEmergency: () -> Unit = {},
    val onCancelEmergency: () -> Unit = {},
)

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
    val context = LocalContext.current
    OnNfcTag(viewModel::onNfcTag)
    RequestNotificationPermissionWhen(state is LockState.Locked)

    HomeContent(
        ui = HomeUi(
            state = state,
            keyCount = keyCount,
            keyTypes = keyTypes,
            emergencyLeft = emergencyLeft,
            mode = mode,
            notice = viewModel.notice,
            blockerEnabled = rememberBlockerEnabled(),
            adminActive = rememberAdminActive(),
            nfcStatus = rememberNfcStatus(),
            settings = viewModel.settings,
        ),
        actions = HomeActions(
            onScanQr = onScanQr,
            onTapCard = onTapCard,
            onAddCard = onAddCard,
            onManageKeys = onManageKeys,
            onChooseApps = onChooseApps,
            onSetUpBlocker = onSetUpBlocker,
            onStats = onStats,
            onSetupChecklist = onSetupChecklist,
            onOpenNfcSettings = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
            onRequestEmergency = viewModel::requestEmergencyUnlock,
            onUseEmergency = viewModel::emergencyUnlock,
            onCancelEmergency = viewModel::cancelEmergencyUnlock,
        ),
    )
}

@Composable
fun HomeContent(ui: HomeUi, actions: HomeActions, now: Long? = null) {
    var confirmEmergency by remember { mutableStateOf(false) }
    val locked = ui.state is LockState.Locked

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val dotColor = if (locked) LiveFreeTheme.signal else MaterialTheme.colorScheme.onSurface
                StatusDot(filled = true, color = dotColor)
                MonoLabel("Live Free", color = MaterialTheme.colorScheme.onSurface)
            }

            ui.state?.let { StatusHero(it) }
            ui.notice?.let { NoticeCard(it) }
            when {
                !ui.blockerEnabled -> BlockerOffCard(actions.onSetUpBlocker)
                !ui.adminActive -> TextButton(onClick = actions.onSetUpBlocker) {
                    Text("Recommended: turn on uninstall protection")
                }
            }

            when (ui.keyCount) {
                null -> Unit
                0 -> {
                    Text("Pair an old NFC card or a printed QR code to use as your key.")
                    PrimaryButton("Pair your first key", actions.onManageKeys)
                }
                else -> {
                    KeyActions(ui, actions, locked)
                    Spacer(Modifier.height(8.dp))
                    Column {
                        MenuRow("Apps to block", actions.onChooseApps, detail = ui.mode?.let(::summary))
                        MenuRow("Keys", actions.onManageKeys)
                        MenuRow("Stats", actions.onStats)
                        MenuRow("Setup checklist", actions.onSetupChecklist)
                    }
                }
            }

            (ui.state as? LockState.Locked)?.let { current ->
                EmergencySection(
                    requestedAt = current.emergencyRequestedAt,
                    remaining = ui.emergencyLeft,
                    settings = ui.settings,
                    fixedNow = now,
                    onStart = { confirmEmergency = true },
                    onUse = actions.onUseEmergency,
                    onCancel = actions.onCancelEmergency,
                )
            }
        }
    }

    if (confirmEmergency) {
        EmergencyDialog(
            remaining = ui.emergencyLeft ?: 0,
            settings = ui.settings,
            onConfirm = {
                actions.onRequestEmergency()
                confirmEmergency = false
            },
            onDismiss = { confirmEmergency = false },
        )
    }
}

@Composable
private fun KeyActions(ui: HomeUi, actions: HomeActions, locked: Boolean) {
    val action = if (locked) "unlock" else "lock"
    val hasCard = ui.keyTypes.any { it == KeyType.NFC_UID || it == KeyType.NFC_NDEF }
    val hasQr = KeyType.QR in ui.keyTypes
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            ui.nfcStatus == NfcStatus.NOT_SUPPORTED -> Unit
            hasCard -> PrimaryButton("Tap card to $action", actions.onTapCard)
            !locked -> SecondaryButton("Add a card to lock and unlock with", actions.onAddCard)
        }
        if (ui.nfcStatus == NfcStatus.DISABLED && hasCard) {
            TextButton(onClick = actions.onOpenNfcSettings) { Text("NFC is off. Turn it on to use your card.") }
        }
        if (hasQr) {
            if (hasCard) {
                SecondaryButton("Scan QR key to $action", actions.onScanQr)
            } else {
                PrimaryButton("Scan QR key to $action", actions.onScanQr)
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** The big status: dot-matrix LOCKED in the signal colour, or UNLOCKED in ink, on graph-paper dots. */
@Composable
private fun StatusHero(state: LockState) {
    val locked = state is LockState.Locked
    FlatCard(
        modifier = Modifier,
        borderColor = if (locked) LiveFreeTheme.signal else MaterialTheme.colorScheme.outlineVariant,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .dotGrid(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MonoLabel("Status")
            Text(
                if (locked) "LOCKED" else "UNLOCKED",
                style = MaterialTheme.typography.displayMedium,
                color = if (locked) LiveFreeTheme.signal else MaterialTheme.colorScheme.onSurface,
            )
            when (state) {
                LockState.Unlocked ->
                    Text("Tap your key to lock your phone.", style = MaterialTheme.typography.bodyMedium)
                is LockState.Locked -> {
                    val since = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(state.since))
                    MonoLabel("Since $since", color = MaterialTheme.colorScheme.onSurface)
                }
            }
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
    FlatCard(borderColor = MaterialTheme.colorScheme.error) {
        MonoLabel("Action needed", color = MaterialTheme.colorScheme.error)
        Text("App blocking is off", style = MaterialTheme.typography.titleMedium)
        Text("Locking won't block anything until you turn it on.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Button(onClick = onSetUp) { Text("Turn on", style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
private fun NoticeCard(notice: Notice) {
    val color = if (notice.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    FlatCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusDot(filled = true, color = color)
            Text(notice.text, style = MaterialTheme.typography.bodyMedium)
        }
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
    fixedNow: Long?,
    onStart: () -> Unit,
    onUse: () -> Unit,
    onCancel: () -> Unit,
) {
    var now by remember { mutableLongStateOf(fixedNow ?: System.currentTimeMillis()) }
    LaunchedEffect(requestedAt, fixedNow) {
        while (requestedAt != null && fixedNow == null) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    when (val status = LockRules.emergencyStatus(requestedAt, now, settings)) {
        LockRules.EmergencyStatus.NotRequested -> TextButton(onClick = onStart) {
            Text("Emergency unlock · ${remaining ?: "…"} left", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        is LockRules.EmergencyStatus.Waiting -> FlatCard {
            MonoLabel("Emergency unlock in")
            Text(countdown(status.msLeft), style = MaterialTheme.typography.displaySmall)
            Text("Tapping your key still unlocks straight away.", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
        is LockRules.EmergencyStatus.Ready -> FlatCard {
            MonoLabel("Emergency unlock ready")
            Text(
                "Use it in the next ${countdown(status.msLeft)} or it lapses.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onUse, modifier = Modifier.fillMaxWidth()) {
                Text("Unlock now · uses 1 of ${remaining ?: "…"}")
            }
            TextButton(onClick = onCancel) { Text("Cancel") }
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
