package com.livefree.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livefree.core.data.StatsRepository
import com.livefree.core.model.LockStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(stats: StatsRepository) : ViewModel() {
    /** Stats for the last 7 days, or null while loading. */
    val stats: StateFlow<LockStats?> = stats.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
