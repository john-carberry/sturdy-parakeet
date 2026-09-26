package com.diybrick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.diybrick.feature.nfc.LocalNfcTagDispatcher
import com.diybrick.feature.nfc.NfcReader
import com.diybrick.feature.nfc.NfcTagDispatcher
import com.diybrick.ui.DiyBrickNavHost
import com.diybrick.ui.theme.DiyBrickTheme

class MainActivity : ComponentActivity() {
    private val nfcDispatcher = NfcTagDispatcher()
    private lateinit var nfcReader: NfcReader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcReader = NfcReader(this, nfcDispatcher)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalNfcTagDispatcher provides nfcDispatcher) {
                DiyBrickTheme {
                    DiyBrickNavHost()
                }
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
