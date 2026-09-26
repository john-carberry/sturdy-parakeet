package com.livefree.feature.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.os.Handler
import android.os.Looper

/**
 * Keeps NFC reader mode on while the activity is resumed and hands each tag's UID
 * to the [NfcTagDispatcher]. Reader mode stops Android from opening other apps
 * when a card is tapped while Live Free is open.
 */
class NfcReader(
    private val activity: Activity,
    private val dispatcher: NfcTagDispatcher,
) {
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Call from onResume. */
    fun enable() {
        adapter?.enableReaderMode(
            activity,
            { tag -> tag.id?.let { uid -> mainHandler.post { dispatcher.dispatch(uid) } } },
            READER_FLAGS,
            null,
        )
    }

    /** Call from onPause. */
    fun disable() {
        adapter?.disableReaderMode(activity)
    }

    private companion object {
        const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
    }
}
