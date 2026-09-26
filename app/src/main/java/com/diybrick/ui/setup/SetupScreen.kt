package com.diybrick.ui.setup

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diybrick.feature.blocker.rememberAdminActive
import com.diybrick.feature.blocker.rememberBlockerEnabled

/**
 * First-run checklist (PLAN.md §4.1, screen 1). Each item ticks itself off as it's
 * done, and it can be reopened from the home screen later.
 */
@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onPairKey: () -> Unit,
    onChooseApps: () -> Unit,
    onProtection: () -> Unit,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val hasKey by viewModel.hasKey.collectAsStateWithLifecycle()
    val hasApps by viewModel.hasApps.collectAsStateWithLifecycle()
    val blockerOn = rememberBlockerEnabled()
    val adminOn = rememberAdminActive()

    fun notificationsAllowed() = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    var notificationsOn by remember { mutableStateOf(notificationsAllowed()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { notificationsOn = notificationsAllowed() }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsOn = it
    }

    val essentialsDone = hasKey && hasApps && blockerOn

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Welcome to Live Free", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Lock distracting apps away and unlock them only with a card or QR code you keep " +
                    "somewhere else. A few steps to set up:",
                style = MaterialTheme.typography.bodyLarge,
            )

            ChecklistItem(
                done = hasKey,
                title = "Pair a key",
                detail = "An old NFC card (hotel key, transit card, badge) or a printed QR code.",
                onClick = onPairKey,
            )
            ChecklistItem(
                done = hasApps,
                title = "Choose apps to block",
                detail = "Calls, messages, alarms and other essentials always keep working.",
                onClick = onChooseApps,
            )
            ChecklistItem(
                done = blockerOn,
                title = "Turn on app blocking",
                detail = "Switch on the Live Free app blocker in Accessibility settings.",
                onClick = onProtection,
            )
            ChecklistItem(
                done = adminOn,
                title = "Turn on uninstall protection (recommended)",
                detail = "Stops Live Free being uninstalled while you're bricked.",
                onClick = onProtection,
            )
            if (Build.VERSION.SDK_INT >= 33) {
                ChecklistItem(
                    done = notificationsOn,
                    title = "Allow notifications (recommended)",
                    detail = "Shows a “Bricked” notification with a timer while you're bricked.",
                    onClick = { askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) },
                )
            }

            Button(
                onClick = {
                    viewModel.finish()
                    onFinish()
                },
                enabled = essentialsDone,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Start using Live Free") }
            if (!essentialsDone) {
                TextButton(
                    onClick = {
                        viewModel.finish()
                        onFinish()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Skip for now") }
            }
        }
    }
}

@Composable
private fun ChecklistItem(done: Boolean, title: String, detail: String, onClick: () -> Unit) {
    ListItem(
        leadingContent = { Text(if (done) "✅" else "⬜", style = MaterialTheme.typography.titleLarge) },
        headlineContent = { Text(title) },
        supportingContent = { Text(detail) },
        modifier = Modifier.clickable(enabled = !done, onClick = onClick),
    )
}
