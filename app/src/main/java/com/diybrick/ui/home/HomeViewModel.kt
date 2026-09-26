package com.diybrick.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diybrick.core.data.KeyRepository
import com.diybrick.core.model.KeyType
import com.diybrick.core.security.NfcUidPolicy
import com.diybrick.core.security.QrKeyPayload
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Result of checking a tapped card or scanned QR code against the paired keys. */
sealed interface KeyCheck {
    data class Recognised(val label: String) : KeyCheck
    data class NotRecognised(val message: String) : KeyCheck
}

class HomeViewModel(private val keys: KeyRepository) : ViewModel() {

    /** Number of paired keys, or null while loading. */
    val keyCount: StateFlow<Int?> = keys.keys
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var lastCheck by mutableStateOf<KeyCheck?>(null)
        private set

    fun onNfcTag(uid: ByteArray) {
        viewModelScope.launch {
            lastCheck = when {
                NfcUidPolicy.isRandomUid(uid) -> KeyCheck.NotRecognised(RANDOM_UID_MESSAGE)
                else -> keys.find(KeyType.NFC_UID, uid)?.let { KeyCheck.Recognised(it.label) }
                    ?: KeyCheck.NotRecognised("This card isn't paired.")
            }
        }
    }

    fun onQrScanned(text: String) {
        viewModelScope.launch {
            val secret = QrKeyPayload.decode(text)
            lastCheck = when {
                secret == null -> KeyCheck.NotRecognised("That QR code isn't a DIY Brick key.")
                else -> keys.find(KeyType.QR, secret)?.let { KeyCheck.Recognised(it.label) }
                    ?: KeyCheck.NotRecognised("This QR key isn't paired. It may have been removed.")
            }
        }
    }

    companion object {
        const val RANDOM_UID_MESSAGE =
            "This card changes its ID on every tap (bank cards and phone wallets do this), " +
                "so it can't be used as a key."
    }
}
