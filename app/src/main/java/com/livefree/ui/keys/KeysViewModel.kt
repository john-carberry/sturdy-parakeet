package com.livefree.ui.keys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livefree.core.data.LockRepository
import com.livefree.core.data.KeyRepository
import com.livefree.core.model.LockState
import com.livefree.core.model.Key
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KeysViewModel(
    private val keys: KeyRepository,
    lock: LockRepository,
) : ViewModel() {

    /** Paired keys, or null while loading. */
    val all: StateFlow<List<Key>?> = keys.keys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Keys can't be added or removed while locked. */
    val locked: StateFlow<Boolean> = lock.state
        .map { it is LockState.Locked }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun remove(key: Key) {
        viewModelScope.launch { keys.remove(key.id) }
    }
}
