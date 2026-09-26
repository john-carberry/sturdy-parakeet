package com.diybrick.ui.keys

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diybrick.core.model.Key
import com.diybrick.core.model.KeyType
import com.diybrick.ui.components.BackTopBar
import java.text.DateFormat
import java.util.Date

@Composable
fun KeysScreen(
    viewModel: KeysViewModel,
    onAddNfc: () -> Unit,
    onAddQr: () -> Unit,
    onBack: () -> Unit,
) {
    val keys by viewModel.all.collectAsStateWithLifecycle()
    var pendingRemoval by remember { mutableStateOf<Key?>(null) }

    Scaffold(topBar = { BackTopBar("Keys", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                val list = keys.orEmpty()
                if (keys != null && list.isEmpty()) {
                    item {
                        Text(
                            "No keys yet. Add an old NFC card (hotel key, transit card, badge) " +
                                "or print a QR code.",
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
                items(list, key = { it.id }) { key ->
                    KeyRow(key, onRemove = { pendingRemoval = key })
                    HorizontalDivider()
                }
            }
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = onAddNfc, modifier = Modifier.fillMaxWidth()) {
                    Text("Add NFC card")
                }
                OutlinedButton(onClick = onAddQr, modifier = Modifier.fillMaxWidth()) {
                    Text("Add QR code")
                }
            }
        }
    }

    pendingRemoval?.let { key ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove “${key.label}”?") },
            text = { Text("It will no longer work as a key. You can pair it again later.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.remove(key)
                    pendingRemoval = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun KeyRow(key: Key, onRemove: () -> Unit) {
    val kind = when (key.type) {
        KeyType.NFC_UID, KeyType.NFC_NDEF -> "📇 NFC card"
        KeyType.QR -> "🔳 QR code"
    }
    val added = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(key.createdAt))
    ListItem(
        headlineContent = { Text(key.label) },
        supportingContent = { Text("$kind · added $added") },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove ${key.label}")
            }
        },
    )
}
