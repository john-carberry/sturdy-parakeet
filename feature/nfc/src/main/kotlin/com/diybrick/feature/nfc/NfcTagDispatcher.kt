package com.diybrick.feature.nfc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Routes tag UIDs to the most recently shown screen that listens for them. A stack
 * (rather than one listener) keeps screen transitions safe: the old screen leaving
 * can't unregister the new one.
 */
class NfcTagDispatcher {
    private val listeners = mutableListOf<(ByteArray) -> Unit>()

    fun register(listener: (ByteArray) -> Unit) {
        listeners += listener
    }

    fun unregister(listener: (ByteArray) -> Unit) {
        listeners -= listener
    }

    fun dispatch(uid: ByteArray) {
        listeners.lastOrNull()?.invoke(uid)
    }
}

val LocalNfcTagDispatcher = staticCompositionLocalOf<NfcTagDispatcher?> { null }

/** Calls [onTag] with the UID of each NFC card tapped while this composable is shown. */
@Composable
fun OnNfcTag(onTag: (ByteArray) -> Unit) {
    val dispatcher = LocalNfcTagDispatcher.current ?: return
    val currentOnTag by rememberUpdatedState(onTag)
    DisposableEffect(dispatcher) {
        val listener: (ByteArray) -> Unit = { currentOnTag(it) }
        dispatcher.register(listener)
        onDispose { dispatcher.unregister(listener) }
    }
}
