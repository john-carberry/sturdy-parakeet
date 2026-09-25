package com.diybrick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.diybrick.ui.HomeScreen
import com.diybrick.ui.theme.DiyBrickTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiyBrickTheme {
                HomeScreen()
            }
        }
    }
}
