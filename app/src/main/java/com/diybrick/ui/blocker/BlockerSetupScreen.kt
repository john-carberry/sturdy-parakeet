package com.diybrick.ui.blocker

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.diybrick.feature.blocker.BlockerStatus
import com.diybrick.feature.blocker.rememberAdminActive
import com.diybrick.feature.blocker.rememberBlockerEnabled
import com.diybrick.ui.components.BackTopBar

/** Walks the user through turning on app blocking and uninstall protection. */
@Composable
fun BlockerSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val blockerOn = rememberBlockerEnabled()
    val adminOn = rememberAdminActive()

    Scaffold(topBar = { BackTopBar("Protection setup", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("App blocking", style = MaterialTheme.typography.headlineSmall)
            if (blockerOn) {
                Text("✓ On")
            } else {
                Text(
                    "DIY Brick uses an accessibility service to spot when a blocked app opens. " +
                        "While you're bricked it also reads Settings screens, so the blocker can't " +
                        "be switched off. Nothing leaves your phone.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (Build.VERSION.SDK_INT >= 33) {
                    Step(
                        "1. Allow restricted settings",
                        "Android hides this option for apps installed from a file. Open App info, " +
                            "tap the ⋮ menu (top right), then “Allow restricted settings”. If you " +
                            "don't see it, skip to step 2.",
                    )
                    OutlinedButton(
                        onClick = { context.startActivity(BlockerStatus.appInfoIntent(context)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Open App info") }
                }
                Step(
                    if (Build.VERSION.SDK_INT >= 33) "2. Turn on the blocker" else "Turn on the blocker",
                    "In Accessibility, open “Installed apps” or “Downloaded apps”, choose " +
                        "“DIY Brick app blocker” and switch it on.",
                )
                Button(
                    onClick = { context.startActivity(BlockerStatus.accessibilitySettingsIntent()) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Open Accessibility settings") }
            }

            HorizontalDivider()

            Text("Uninstall protection", style = MaterialTheme.typography.headlineSmall)
            if (adminOn) {
                Text("✓ On. DIY Brick can't be uninstalled while you're bricked.")
            } else {
                Text(
                    "Recommended. Makes DIY Brick a “device admin” app, which Android won't " +
                        "uninstall until it's turned off, and that switch is locked while you're " +
                        "bricked. It can't see or change anything else.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(
                    onClick = { context.startActivity(BlockerStatus.addAdminIntent(context)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Turn on uninstall protection") }
            }
            Text(
                "To remove DIY Brick later: unbrick, turn off “DIY Brick uninstall protection” " +
                    "under Settings › Security › Device admin apps, then uninstall as usual.",
                style = MaterialTheme.typography.bodySmall,
            )

            if (blockerOn && adminOn) {
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
        }
    }
}

@Composable
private fun Step(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    Text(body, style = MaterialTheme.typography.bodyMedium)
}
