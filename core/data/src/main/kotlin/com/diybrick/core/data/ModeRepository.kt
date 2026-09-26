package com.diybrick.core.data

import com.diybrick.core.model.ListType
import com.diybrick.core.model.Mode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** Which apps to block. For now there's a single mode, [Mode.DEFAULT_ID]. */
class ModeRepository internal constructor(
    private val dao: ModeDao,
    /** True while bricked; the app list can't change then (no unblocking your way out). */
    private val isLocked: suspend () -> Boolean = { false },
) {
    fun observe(id: Long = Mode.DEFAULT_ID): Flow<Mode> =
        combine(dao.observeMode(id), dao.observeApps(id)) { mode, apps ->
            Mode(
                id = id,
                name = mode?.name ?: DEFAULT_NAME,
                listType = mode?.listType?.let(ListType::valueOf) ?: ListType.BLOCK,
                packages = apps.toSet(),
            )
        }.distinctUntilChanged()

    /** Saves [mode]. Returns false if the phone is bricked. */
    suspend fun save(mode: Mode): Boolean {
        if (isLocked()) return false
        dao.replace(ModeEntity(mode.id, mode.name, mode.listType.name), mode.packages)
        return true
    }

    private companion object {
        const val DEFAULT_NAME = "Focus"
    }
}
