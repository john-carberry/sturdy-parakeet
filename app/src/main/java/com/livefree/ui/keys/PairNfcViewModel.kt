package com.livefree.ui.keys

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livefree.core.data.KeyRepository
import com.livefree.core.model.KeyType
import com.livefree.core.security.NfcPairing
import kotlinx.coroutines.launch

class PairNfcViewModel(private val keys: KeyRepository) : ViewModel() {

    sealed interface SaveResult {
        data object Saved : SaveResult
        data class AlreadyPaired(val label: String) : SaveResult
        data object Locked : SaveResult
    }

    private val pairing = NfcPairing()

    var state by mutableStateOf(pairing.state)
        private set
    var label by mutableStateOf("")
    var saving by mutableStateOf(false)
        private set
    var saveResult by mutableStateOf<SaveResult?>(null)
        private set

    fun onTap(uid: ByteArray) {
        if (saving || saveResult != null) return
        state = pairing.onTap(uid)
    }

    fun restart() {
        pairing.reset()
        state = pairing.state
        saveResult = null
    }

    fun save() {
        val confirmed = state as? NfcPairing.State.Confirmed ?: return
        if (saving) return
        saving = true
        viewModelScope.launch {
            val result = keys.add(KeyType.NFC_UID, label.ifBlank { "NFC card" }, confirmed.uid)
            saveResult = when (result) {
                is KeyRepository.AddResult.Added -> SaveResult.Saved
                is KeyRepository.AddResult.AlreadyPaired -> SaveResult.AlreadyPaired(result.existing.label)
                KeyRepository.AddResult.Locked -> SaveResult.Locked
            }
            saving = false
        }
    }
}
