package com.diybrick.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diybrick.core.data.BrickRepository
import com.diybrick.core.data.KeyRepository
import com.diybrick.core.data.ModeRepository
import com.diybrick.core.model.BrickRules.DenyReason
import com.diybrick.core.model.BrickRules.Outcome
import com.diybrick.core.model.BrickState
import com.diybrick.core.model.Key
import com.diybrick.core.model.KeyType
import com.diybrick.core.model.Mode
import com.diybrick.core.security.NfcUidPolicy
import com.diybrick.core.security.QrKeyPayload
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A short message shown after a tap, scan or emergency unbrick. */
data class Notice(val text: String, val isError: Boolean = false)

class HomeViewModel(
    private val keys: KeyRepository,
    private val brick: BrickRepository,
    modes: ModeRepository,
) : ViewModel() {

    /** Brick state, or null while loading. */
    val state: StateFlow<BrickState?> = brick.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val emergencyUnbricksRemaining: StateFlow<Int?> = brick.emergencyUnbricksRemaining
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The apps to block, or null while loading. */
    val mode: StateFlow<Mode?> = modes.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Number of paired keys, or null while loading. */
    val keyCount: StateFlow<Int?> = keys.keys
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var notice by mutableStateOf<Notice?>(null)
        private set

    fun onNfcTag(uid: ByteArray) {
        viewModelScope.launch {
            if (NfcUidPolicy.isRandomUid(uid)) {
                notice = Notice(RANDOM_UID_MESSAGE, isError = true)
                return@launch
            }
            val key = keys.find(KeyType.NFC_UID, uid)
            if (key == null) notice = Notice("This card isn't paired.", isError = true) else present(key)
        }
    }

    fun onQrScanned(text: String) {
        viewModelScope.launch {
            val secret = QrKeyPayload.decode(text)
            val key = secret?.let { keys.find(KeyType.QR, it) }
            when {
                secret == null -> notice = Notice("That QR code isn't a DIY Brick key.", isError = true)
                key == null -> notice = Notice(
                    "This QR key isn't paired. It may have been removed.",
                    isError = true,
                )
                else -> present(key)
            }
        }
    }

    fun emergencyUnbrick() {
        viewModelScope.launch {
            notice = when (val outcome = brick.emergencyUnbrick()) {
                is Outcome.Unbrick -> Notice("🟢 Unbricked with an emergency unbrick.")
                is Outcome.Denied -> when (outcome.reason) {
                    DenyReason.NO_EMERGENCY_UNBRICKS_LEFT ->
                        Notice("No emergency unbricks left. You'll need your key.", isError = true)
                    DenyReason.NOT_BRICKED -> null
                }
                is Outcome.Brick -> null
            }
        }
    }

    private suspend fun present(key: Key) {
        notice = when (brick.onKey()) {
            is Outcome.Brick -> Notice("🧱 Bricked with “${key.label}”.")
            is Outcome.Unbrick -> Notice("🟢 Unbricked with “${key.label}”.")
            is Outcome.Denied -> null
        }
    }

    companion object {
        const val RANDOM_UID_MESSAGE =
            "This card changes its ID on every tap (bank cards and phone wallets do this), " +
                "so it can't be used as a key."
    }
}
