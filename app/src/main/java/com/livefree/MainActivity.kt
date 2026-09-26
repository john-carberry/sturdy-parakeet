package com.livefree

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.livefree.core.model.LockState
import com.livefree.feature.nfc.LocalNfcTagDispatcher
import com.livefree.feature.nfc.NfcReader
import com.livefree.feature.nfc.NfcTagDispatcher
import com.livefree.feature.service.LockService
import com.livefree.ui.LiveFreeNavHost
import com.livefree.core.ui.LiveFreeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val nfcDispatcher = NfcTagDispatcher()
    private lateinit var nfcReader: NfcReader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcReader = NfcReader(this, nfcDispatcher)
        startLockServiceWhileLocked()
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalNfcTagDispatcher provides nfcDispatcher) {
                LiveFreeTheme {
                    LiveFreeNavHost()
                }
            }
        }
    }

    /**
     * The service can only be started while the app is visible, so start it from here
     * whenever the phone is locked. That covers a fresh lock and recovering a killed service.
     */
    private fun startLockServiceWhileLocked() {
        val lock = (application as LiveFreeApp).container.lockRepository
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                lock.state.collect { if (it is LockState.Locked) LockService.start(this@MainActivity) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcReader.enable()
    }

    override fun onPause() {
        nfcReader.disable()
        super.onPause()
    }
}
