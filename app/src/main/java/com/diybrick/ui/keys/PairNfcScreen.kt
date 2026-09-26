package com.diybrick.ui.keys

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.diybrick.core.security.NfcPairing.RejectReason
import com.diybrick.core.security.NfcPairing.State
import com.diybrick.feature.nfc.NfcStatus
import com.diybrick.feature.nfc.OnNfcTag
import com.diybrick.feature.nfc.rememberNfcStatus
import com.diybrick.ui.components.BackTopBar

@Composable
fun PairNfcScreen(viewModel: PairNfcViewModel, onDone: () -> Unit) {
    val nfcStatus = rememberNfcStatus()
    val context = LocalContext.current
    OnNfcTag(viewModel::onTap)

    LaunchedEffect(viewModel.saveResult) {
        if (viewModel.saveResult == PairNfcViewModel.SaveResult.Saved) onDone()
    }

    Scaffold(topBar = { BackTopBar("Add NFC card", onDone) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (nfcStatus) {
                NfcStatus.NOT_SUPPORTED -> {
                    Text("This phone doesn't have NFC. Use a QR code key instead.")
                    return@Column
                }
                NfcStatus.DISABLED -> {
                    Text("NFC is turned off.")
                    Button(onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }) {
                        Text("Open NFC settings")
                    }
                    return@Column
                }
                NfcStatus.READY -> Unit
            }

            (viewModel.saveResult as? PairNfcViewModel.SaveResult.AlreadyPaired)?.let {
                Text("This card is already paired as “${it.label}”.")
                OutlinedButton(onClick = viewModel::restart) { Text("Use a different card") }
                return@Column
            }

            when (val state = viewModel.state) {
                State.WaitingForFirstTap -> Instruction(
                    "Step 1 of 2",
                    "Hold your card flat against the back of the phone until it vibrates. " +
                        "Hotel keys, transit cards, badges and NFC stickers all work.",
                )
                is State.WaitingForSecondTap -> Instruction(
                    "Step 2 of 2",
                    "Move the card away, then tap it again to confirm.",
                )
                is State.Rejected -> {
                    Instruction("Card can't be used", rejectionMessage(state.reason))
                    Text("Tap a different card to try again.")
                }
                is State.Confirmed -> {
                    Instruction("Card read ✓", "Give it a name so you can recognise it later.")
                    OutlinedTextField(
                        value = viewModel.label,
                        onValueChange = { viewModel.label = it.take(40) },
                        label = { Text("Name (e.g. Old hotel key)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = viewModel::save,
                        enabled = !viewModel.saving,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Save key") }
                }
            }
        }
    }
}

@Composable
private fun Instruction(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall)
    Text(body, style = MaterialTheme.typography.bodyLarge)
}

private fun rejectionMessage(reason: RejectReason): String = when (reason) {
    RejectReason.RANDOM_UID, RejectReason.UID_CHANGED ->
        "This card shows a different ID each time it's tapped. Bank cards and phone " +
            "wallets do this for privacy, so they can't be used as keys."
    RejectReason.EMPTY_UID -> "The phone couldn't read an ID from this card."
}
