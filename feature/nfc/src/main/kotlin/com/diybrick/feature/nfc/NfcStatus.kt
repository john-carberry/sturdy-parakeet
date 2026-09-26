package com.diybrick.feature.nfc

import android.content.Context
import android.nfc.NfcAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

enum class NfcStatus { NOT_SUPPORTED, DISABLED, READY }

fun nfcStatus(context: Context): NfcStatus {
    val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcStatus.NOT_SUPPORTED
    return if (adapter.isEnabled) NfcStatus.READY else NfcStatus.DISABLED
}

/** NFC status, re-checked whenever the screen resumes (e.g. back from NFC settings). */
@Composable
fun rememberNfcStatus(): NfcStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(nfcStatus(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { status = nfcStatus(context) }
    return status
}
