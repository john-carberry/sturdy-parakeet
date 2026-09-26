package com.livefree.feature.blocker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.livefree.core.model.ListType
import com.livefree.core.ui.LiveFreeTheme
import com.livefree.core.ui.MonoLabel
import com.livefree.core.ui.dotGrid
import java.text.DateFormat
import java.util.Date

/**
 * The "closed" page, always in the dark theme so it reads as a hard stop: dot-matrix
 * CLOSED in the signal colour over graph-paper dots.
 */
@Composable
fun BlockedScreen(
    title: String,
    explanation: String,
    nextStep: String,
    onGoHome: () -> Unit = {},
    onOpenApp: () -> Unit = {},
) {
    LiveFreeTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .dotGrid(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), spacing = 18.dp)
                    .padding(horizontal = 28.dp, vertical = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.weight(1f))
                MonoLabel("Live Free")
                Text("CLOSED", style = MaterialTheme.typography.displayLarge, color = LiveFreeTheme.signal)
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text(explanation, style = MaterialTheme.typography.bodyLarge)
                Text(
                    nextStep,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onGoHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) { Text("Go home", style = MaterialTheme.typography.labelLarge) }
                OutlinedButton(
                    onClick = onOpenApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                ) { Text("Open Live Free", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

/** Full-screen page shown in place of a blocked app, explaining why it was closed. */
class BlockedActivity : ComponentActivity() {
    private var info by mutableStateOf(BlockInfo("This app", ListType.BLOCK, null, tamper = false))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        info = infoFrom(intent)
        setContent {
            BackHandler { goHome() }
            BlockedScreen(
                title = info.title(),
                explanation = info.explanation(),
                nextStep = info.nextStep(),
                onGoHome = ::goHome,
                onOpenApp = ::openLiveFree,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        info = infoFrom(intent)
    }

    private fun infoFrom(intent: Intent): BlockInfo {
        val packageName = intent.getStringExtra(EXTRA_PACKAGE)
        return BlockInfo(
            appLabel = packageName?.let { AppLabels.of(this, it) } ?: "This app",
            listType = intent.getStringExtra(EXTRA_LIST_TYPE)?.let(ListType::valueOf) ?: ListType.BLOCK,
            since = intent.getLongExtra(EXTRA_SINCE, -1L).takeIf { it >= 0 },
            tamper = intent.getBooleanExtra(EXTRA_TAMPER, false),
        )
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    private fun openLiveFree() {
        packageManager.getLaunchIntentForPackage(packageName)?.let(::startActivity)
        finish()
    }

    private data class BlockInfo(
        val appLabel: String,
        val listType: ListType,
        val since: Long?,
        /** Closed a Settings screen that could switch Live Free off, rather than an app. */
        val tamper: Boolean,
    ) {
        fun title(): String = if (tamper) "Live Free settings are locked" else "$appLabel was closed"

        fun nextStep(): String = if (tamper) {
            "Tap your key to unlock first, then you can change these settings."
        } else {
            "To use it again, tap your key to unlock your phone."
        }

        fun explanation(): String {
            val locked = since
                ?.let { "Your phone has been locked since ${timeFormat.format(Date(it))}" }
                ?: "Your phone is locked"
            if (tamper) {
                return "$locked, so Live Free can't be switched off, force stopped or uninstalled."
            }
            return when (listType) {
                ListType.BLOCK -> "$locked, and $appLabel is on your list of apps to block."
                ListType.ALLOW -> "$locked, and only the apps you allowed (plus calls, messages, " +
                    "alarms and other essentials) can open."
            }
        }
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"
        private const val EXTRA_LIST_TYPE = "list_type"
        private const val EXTRA_SINCE = "since"
        private const val EXTRA_TAMPER = "tamper"
        private val timeFormat: DateFormat get() = DateFormat.getTimeInstance(DateFormat.SHORT)

        fun intent(context: Context, packageName: String, listType: ListType, since: Long): Intent =
            Intent(context, BlockedActivity::class.java)
                .putExtra(EXTRA_PACKAGE, packageName)
                .putExtra(EXTRA_LIST_TYPE, listType.name)
                .putExtra(EXTRA_SINCE, since)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        fun tamperIntent(context: Context, since: Long): Intent =
            Intent(context, BlockedActivity::class.java)
                .putExtra(EXTRA_TAMPER, true)
                .putExtra(EXTRA_SINCE, since)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
