package com.diybrick.ui.modes

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diybrick.core.data.BrickRepository
import com.diybrick.core.data.ModeRepository
import com.diybrick.core.model.BrickState
import com.diybrick.core.model.ListType
import com.diybrick.core.model.Mode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Edits a draft of the mode; nothing changes until [save]. */
class ModeViewModel(
    /** The application context, used to list installed apps. */
    private val appContext: Context,
    private val modes: ModeRepository,
    brick: BrickRepository,
) : ViewModel() {

    var apps by mutableStateOf<List<InstalledApp>?>(null)
        private set
    var draft by mutableStateOf<Mode?>(null)
        private set
    var query by mutableStateOf("")
    var saved by mutableStateOf(false)
        private set

    val locked: StateFlow<Boolean> = brick.state
        .map { it is BrickState.Bricked }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch {
            draft = modes.observe().first()
            apps = withContext(Dispatchers.Default) { InstalledApps.load(appContext) }
        }
    }

    val visibleApps: List<InstalledApp>
        get() {
            val all = apps.orEmpty()
            if (query.isBlank()) return all
            return all.filter { it.label.contains(query.trim(), ignoreCase = true) }
        }

    fun setListType(type: ListType) {
        draft = draft?.copy(listType = type)
    }

    fun toggle(packageName: String) {
        val mode = draft ?: return
        val packages = if (packageName in mode.packages) mode.packages - packageName else mode.packages + packageName
        draft = mode.copy(packages = packages)
    }

    fun save() {
        val mode = draft ?: return
        viewModelScope.launch { saved = modes.save(mode) }
    }
}
