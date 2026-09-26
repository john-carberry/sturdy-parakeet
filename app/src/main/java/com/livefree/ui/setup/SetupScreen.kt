package com.livefree.ui.setup

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import com.livefree.core.ui.MonoLabel
import com.livefree.core.ui.StatusDot
import com.livefree.feature.blocker.rememberAdminActive
import com.livefree.feature.blocker.rememberBlockerEnabled

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

    SetupContent(
        steps = SetupSteps(
            hasKey = hasKey,
            hasApps = hasApps,
            blockerOn = blockerOn,
            adminOn = adminOn,
            notificationsOn = notificationsOn,
            showNotifications = Build.VERSION.SDK_INT >= 33,
        ),
        onPairKey = onPairKey,
        onChooseApps = onChooseApps,
        onProtection = onProtection,
        onAllowNotifications = {
            if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        onFinish = {
            viewModel.finish()
            onFinish()
        },
    )
}

data class SetupSteps(
    val hasKey: Boolean,
    val hasApps: Boolean,
    val blockerOn: Boolean,
    val adminOn: Boolean,
    val notificationsOn: Boolean,
    val showNotifications: Boolean,
) {
    val essentialsDone: Boolean get() = hasKey && hasApps && blockerOn
}

@Composable
fun SetupContent(
    steps: SetupSteps,
    onPairKey: () -> Unit = {},
    onChooseApps: () -> Unit = {},
    onProtection: () -> Unit = {},
    onAllowNotifications: () -> Unit = {},
    onFinish: () -> Unit = {},
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MonoLabel("Setup")
            Text("WELCOME", style = MaterialTheme.typography.displayMedium)
            Text(
                "Lock distracting apps away and unlock them only with a card or QR code you keep " +
                    "somewhere else. A few steps to set up:",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(8.dp))

            ChecklistItem(
                number = 1,
                done = steps.hasKey,
                title = "Pair a key",
                detail = "An old NFC card (hotel key, transit card, badge) or a printed QR code.",
                onClick = onPairKey,
            )
            ChecklistItem(
                number = 2,
                done = steps.hasApps,
                title = "Choose apps to block",
                detail = "Calls, messages, alarms and other essentials always keep working.",
                onClick = onChooseApps,
            )
            ChecklistItem(
                number = 3,
                done = steps.blockerOn,
                title = "Turn on app blocking",
                detail = "Switch on the Live Free app blocker in Accessibility settings.",
                onClick = onProtection,
            )
            ChecklistItem(
                number = 4,
                done = steps.adminOn,
                title = "Uninstall protection",
                detail = "Recommended. Stops Live Free being uninstalled while you're locked.",
                onClick = onProtection,
            )
            if (steps.showNotifications) {
                ChecklistItem(
                    number = 5,
                    done = steps.notificationsOn,
                    title = "Allow notifications",
                    detail = "Recommended. Shows a “Locked” notification with a timer.",
                    onClick = onAllowNotifications,
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onFinish,
                enabled = steps.essentialsDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) { Text("Start using Live Free", style = MaterialTheme.typography.labelLarge) }
            if (!steps.essentialsDone) {
                TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Text("Skip for now", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ChecklistItem(number: Int, done: Boolean, title: String, detail: String, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !done, onClick = onClick)
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MonoLabel("%02d".format(number), color = MaterialTheme.colorScheme.onSurface)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusDot(filled = done, modifier = Modifier.padding(top = 4.dp))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
