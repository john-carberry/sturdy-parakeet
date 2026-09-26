package com.diybrick.ui.keys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diybrick.core.data.BrickRepository
import com.diybrick.core.data.KeyRepository
import com.diybrick.core.model.BrickState
import com.diybrick.core.model.Key
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KeysViewModel(
    private val keys: KeyRepository,
    brick: BrickRepository,
) : ViewModel() {

    /** Paired keys, or null while loading. */
    val all: StateFlow<List<Key>?> = keys.keys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Keys can't be added or removed while bricked. */
    val locked: StateFlow<Boolean> = brick.state
        .map { it is BrickState.Bricked }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun remove(key: Key) {
        viewModelScope.launch { keys.remove(key.id) }
    }
}
