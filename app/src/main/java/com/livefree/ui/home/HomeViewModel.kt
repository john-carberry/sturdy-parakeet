package com.livefree.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livefree.core.data.LockRepository
import com.livefree.core.data.KeyRepository
import com.livefree.core.data.ModeRepository
import com.livefree.core.model.LockRules.DenyReason
import com.livefree.core.model.LockRules.Outcome
import com.livefree.core.model.LockState
import com.livefree.core.model.Key
import com.livefree.core.model.KeyType
import com.livefree.core.model.Mode
import com.livefree.core.security.NfcUidPolicy
import com.livefree.core.security.QrKeyPayload
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A short message shown after a tap, scan or emergency unlock. */
data class Notice(val text: String, val isError: Boolean = false)

class HomeViewModel(
    private val keys: KeyRepository,
    private val lock: LockRepository,
    modes: ModeRepository,
) : ViewModel() {

    /** Accent state, or null while loading. */
    val state: StateFlow<LockState?> = lock.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val emergencyUnlocksRemaining: StateFlow<Int?> = lock.emergencyUnlocksRemaining
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The apps to block, or null while loading. */
    val mode: StateFlow<Mode?> = modes.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Number of paired keys, or null while loading. */
    val keyCount: StateFlow<Int?> = keys.keys
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Which kinds of key are paired, e.g. a card and/or a QR code. */
    val keyTypes: StateFlow<Set<KeyType>> = keys.keys
        .map { list -> list.map { it.type }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

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
                secret == null -> notice = Notice("That QR code isn't a Live Free key.", isError = true)
                key == null -> notice = Notice(
                    "This QR key isn't paired. It may have been removed.",
                    isError = true,
                )
                else -> present(key)
            }
        }
    }

    val settings = lock.settings

    fun requestEmergencyUnlock() {
        viewModelScope.launch { notice = describe(lock.requestEmergencyUnlock()) }
    }

    fun cancelEmergencyUnlock() {
        viewModelScope.launch {
            lock.cancelEmergencyUnlock()
            notice = null
        }
    }

    fun emergencyUnlock() {
        viewModelScope.launch { notice = describe(lock.emergencyUnlock()) }
    }

    private fun describe(outcome: Outcome): Notice? = when (outcome) {
        is Outcome.Unlock -> Notice("🟢 Unlocked with an emergency unlock.")
        Outcome.EmergencyRequested -> Notice(
            "Emergency unlock starts in ${settings.emergencyWaitMinutes} minutes. " +
                "Tapping your key still works in the meantime.",
        )
        is Outcome.Denied -> when (outcome.reason) {
            DenyReason.NO_EMERGENCY_UNLOCKS_LEFT ->
                Notice("No emergency unlocks left. You'll need your key.", isError = true)
            DenyReason.EMERGENCY_STILL_WAITING -> Notice("The emergency unlock isn't ready yet.", isError = true)
            DenyReason.EMERGENCY_NOT_REQUESTED ->
                Notice("That emergency unlock lapsed. Start a new one if you still need it.", isError = true)
            DenyReason.NOT_LOCKED -> null
        }
        is Outcome.Lock -> null
    }

    private suspend fun present(key: Key) {
        notice = when (lock.onKey()) {
            is Outcome.Lock -> Notice("🔒 Locked with “${key.label}”.")
            is Outcome.Unlock -> Notice("🟢 Unlocked with “${key.label}”.")
            is Outcome.Denied, Outcome.EmergencyRequested -> null
        }
    }

    companion object {
        const val RANDOM_UID_MESSAGE =
            "This card changes its ID on every tap (bank cards and phone wallets do this), " +
                "so it can't be used as a key."
    }
}
