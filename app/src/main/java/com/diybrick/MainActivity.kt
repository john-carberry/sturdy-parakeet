package com.diybrick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.diybrick.core.model.BrickState
import com.diybrick.feature.nfc.LocalNfcTagDispatcher
import com.diybrick.feature.nfc.NfcReader
import com.diybrick.feature.nfc.NfcTagDispatcher
import com.diybrick.feature.service.BrickService
import com.diybrick.ui.DiyBrickNavHost
import com.diybrick.core.ui.DiyBrickTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val nfcDispatcher = NfcTagDispatcher()
    private lateinit var nfcReader: NfcReader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcReader = NfcReader(this, nfcDispatcher)
        startBrickServiceWhileBricked()
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalNfcTagDispatcher provides nfcDispatcher) {
                DiyBrickTheme {
                    DiyBrickNavHost()
                }
            }
        }
    }

    /**
     * The service can only be started while the app is visible, so start it from here
     * whenever the phone is bricked. That covers a fresh brick and recovering a killed service.
     */
    private fun startBrickServiceWhileBricked() {
        val brick = (application as DiyBrickApp).container.brickRepository
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                brick.state.collect { if (it is BrickState.Bricked) BrickService.start(this@MainActivity) }
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
