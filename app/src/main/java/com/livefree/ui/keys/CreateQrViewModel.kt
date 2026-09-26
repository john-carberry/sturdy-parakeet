package com.livefree.ui.keys

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livefree.core.data.KeyRepository
import com.livefree.core.model.KeyType
import com.livefree.core.security.QrKeyPayload
import kotlinx.coroutines.launch

/**
 * Makes a new QR key. The secret lives only in memory until the user confirms they've
 * printed it; then only its hash is saved and this screen's copy is wiped.
 */
class CreateQrViewModel(private val keys: KeyRepository) : ViewModel() {
    private val secret = QrKeyPayload.newSecret()
    val payload: String = QrKeyPayload.encode(secret)

    var label by mutableStateOf("")
    var printed by mutableStateOf(false)
    var saving by mutableStateOf(false)
        private set
    var saved by mutableStateOf(false)
        private set

    val caption: String
        get() = "Live Free key · ${label.ifBlank { DEFAULT_LABEL }}"

    fun save() {
        if (saving || saved) return
        saving = true
        viewModelScope.launch {
            keys.add(KeyType.QR, label.ifBlank { DEFAULT_LABEL }, secret)
            saved = true
        }
    }

    override fun onCleared() {
        secret.fill(0)
    }

    private companion object {
        const val DEFAULT_LABEL = "QR key"
    }
}
