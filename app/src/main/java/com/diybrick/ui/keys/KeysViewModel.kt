package com.diybrick.ui.keys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diybrick.core.data.KeyRepository
import com.diybrick.core.model.Key
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KeysViewModel(private val keys: KeyRepository) : ViewModel() {

    /** Paired keys, or null while loading. */
    val all: StateFlow<List<Key>?> = keys.keys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun remove(key: Key) {
        viewModelScope.launch { keys.remove(key.id) }
    }
}
