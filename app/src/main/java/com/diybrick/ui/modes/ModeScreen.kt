package com.diybrick.ui.modes

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diybrick.core.model.ListType
import com.diybrick.ui.components.BackTopBar

@Composable
fun ModeScreen(viewModel: ModeViewModel, onDone: () -> Unit) {
    val locked by viewModel.locked.collectAsStateWithLifecycle()
    val draft = viewModel.draft

    LaunchedEffect(viewModel.saved) {
        if (viewModel.saved) onDone()
    }

    Scaffold(topBar = { BackTopBar("Apps to block", onDone) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (locked) {
                Text(
                    "You can't change blocked apps while your phone is bricked.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (draft == null) return@Column

            ListTypeChoice(
                selected = draft.listType,
                enabled = !locked,
                onSelect = viewModel::setListType,
            )
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = { viewModel.query = it },
                label = { Text("Search apps") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Box(modifier = Modifier.weight(1f)) {
                if (viewModel.apps == null) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn {
                        items(viewModel.visibleApps, key = { it.packageName }) { app ->
                            val checked = app.packageName in draft.packages
                            ListItem(
                                headlineContent = { Text(app.label) },
                                leadingContent = {
                                    Image(app.icon, contentDescription = null, modifier = Modifier.size(40.dp))
                                },
                                trailingContent = {
                                    Checkbox(checked = checked, onCheckedChange = null, enabled = !locked)
                                },
                                modifier = Modifier.clickable(enabled = !locked) {
                                    viewModel.toggle(app.packageName)
                                },
                            )
                        }
                    }
                }
            }

            val count = draft.packages.size
            Text(
                when (draft.listType) {
                    ListType.BLOCK -> "$count app${if (count == 1) "" else "s"} will be blocked while bricked."
                    ListType.ALLOW -> "Only $count app${if (count == 1) "" else "s"} (plus calls, the " +
                        "keyboard and Settings) will work while bricked."
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = viewModel::save,
                enabled = !locked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) { Text("Save") }
        }
    }
}

@Composable
private fun ListTypeChoice(selected: ListType, enabled: Boolean, onSelect: (ListType) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
        listOf(
            ListType.BLOCK to "Block the apps I pick",
            ListType.ALLOW to "Block everything except the apps I pick",
        ).forEach { (type, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected == type,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onSelect(type) },
                    ),
            ) {
                RadioButton(selected = selected == type, onClick = null, enabled = enabled)
                Text(label)
            }
        }
    }
}
