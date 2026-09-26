package com.livefree.ui.modes

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livefree.core.data.LockRepository
import com.livefree.core.data.ModeRepository
import com.livefree.core.model.LockState
import com.livefree.core.model.EssentialApps
import com.livefree.core.model.EssentialReason
import com.livefree.core.model.ListType
import com.livefree.core.model.Mode
import com.livefree.feature.blocker.ExemptApps
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
    lock: LockRepository,
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

    val locked: StateFlow<Boolean> = lock.state
        .map { it is LockState.Locked }
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
