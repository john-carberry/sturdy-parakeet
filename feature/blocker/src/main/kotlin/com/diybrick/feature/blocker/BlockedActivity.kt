package com.diybrick.feature.blocker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.diybrick.core.model.ListType
import com.diybrick.core.ui.DiyBrickTheme
import java.text.DateFormat
import java.util.Date

/** Full-screen page shown in place of a blocked app, explaining why it was closed. */
class BlockedActivity : ComponentActivity() {
    private var info by mutableStateOf(BlockInfo("This app", ListType.BLOCK, null))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        info = infoFrom(intent)
        setContent {
            DiyBrickTheme {
                BackHandler { goHome() }
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("🧱", style = MaterialTheme.typography.displayLarge)
                        Text(
                            "${info.appLabel} was closed",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            info.explanation(),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "To use it again, tap your key to unbrick your phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = ::goHome, modifier = Modifier.fillMaxWidth()) {
                            Text("Go home")
                        }
                        OutlinedButton(onClick = ::openDiyBrick, modifier = Modifier.fillMaxWidth()) {
                            Text("Open DIY Brick")
                        }
                    }
                }
            }
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
        )
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    private fun openDiyBrick() {
        packageManager.getLaunchIntentForPackage(packageName)?.let(::startActivity)
        finish()
    }

    private data class BlockInfo(val appLabel: String, val listType: ListType, val since: Long?) {
        fun explanation(): String {
            val bricked = since
                ?.let { "Your phone has been bricked since ${timeFormat.format(Date(it))}" }
                ?: "Your phone is bricked"
            return when (listType) {
                ListType.BLOCK -> "$bricked, and $appLabel is on your list of apps to block."
                ListType.ALLOW -> "$bricked, and only the apps you allowed (plus calls, messages, " +
                    "alarms and other essentials) can open."
            }
        }
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"
        private const val EXTRA_LIST_TYPE = "list_type"
        private const val EXTRA_SINCE = "since"
        private val timeFormat: DateFormat get() = DateFormat.getTimeInstance(DateFormat.SHORT)

        fun intent(context: Context, packageName: String, listType: ListType, since: Long): Intent =
            Intent(context, BlockedActivity::class.java)
                .putExtra(EXTRA_PACKAGE, packageName)
                .putExtra(EXTRA_LIST_TYPE, listType.name)
                .putExtra(EXTRA_SINCE, since)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
