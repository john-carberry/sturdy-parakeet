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
import com.diybrick.core.model.EssentialApps
import com.diybrick.core.model.EssentialReason
import com.diybrick.core.model.ListType
import com.diybrick.core.model.Mode
import com.diybrick.feature.blocker.ExemptApps
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

    /** Apps the phone needs, which can never be blocked, with the reason why. */
    var essential by mutableStateOf<Map<String, EssentialReason>>(EssentialApps.KNOWN)
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
            val (essentialApps, installed) = withContext(Dispatchers.Default) {
                ExemptApps.load(appContext) to InstalledApps.load(appContext)
            }
            essential = essentialApps
            apps = installed
        }
    }

    private val matchingApps: List<InstalledApp>
        get() {
            val all = apps.orEmpty()
            if (query.isBlank()) return all
            return all.filter { it.label.contains(query.trim(), ignoreCase = true) }
        }

    /** Apps the user can choose. */
    val choosableApps: List<InstalledApp>
        get() = matchingApps.filter { it.packageName !in essential }

    /** Installed essential apps, shown locked with their reason. */
    val essentialApps: List<InstalledApp>
        get() = matchingApps.filter { it.packageName in essential }

    fun setListType(type: ListType) {
        draft = draft?.copy(listType = type)
    }

    fun toggle(packageName: String) {
        if (packageName in essential) return
        val mode = draft ?: return
        val packages = if (packageName in mode.packages) mode.packages - packageName else mode.packages + packageName
        draft = mode.copy(packages = packages)
    }

    fun save() {
        val mode = draft?.let { EssentialApps.sanitize(it, essential.keys) } ?: return
        viewModelScope.launch { saved = modes.save(mode) }
    }
}
