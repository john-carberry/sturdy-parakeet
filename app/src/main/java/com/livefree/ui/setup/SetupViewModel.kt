package com.livefree.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livefree.core.data.KeyRepository
import com.livefree.core.data.ModeRepository
import com.livefree.core.model.ListType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SetupViewModel(
    keys: KeyRepository,
    modes: ModeRepository,
    private val prefs: OnboardingPrefs,
) : ViewModel() {

    val hasKey: StateFlow<Boolean> = keys.keys
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** An allow list always blocks something; a block list needs at least one app. */
    val hasApps: StateFlow<Boolean> = modes.observe()
        .map { it.listType == ListType.ALLOW || it.packages.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun finish() {
        prefs.done = true
    }
}
