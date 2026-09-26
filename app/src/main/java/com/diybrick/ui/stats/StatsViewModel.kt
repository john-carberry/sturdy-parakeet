package com.diybrick.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diybrick.core.data.StatsRepository
import com.diybrick.core.model.BrickStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(stats: StatsRepository) : ViewModel() {
    /** Stats for the last 7 days, or null while loading. */
    val stats: StateFlow<BrickStats?> = stats.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
