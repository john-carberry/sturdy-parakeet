package com.livefree.ui.keys

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.livefree.feature.qr.QrCodeImage
import com.livefree.feature.qr.SecureScreen
import com.livefree.feature.qr.printQrKey
import com.livefree.ui.components.BackTopBar

@Composable
fun CreateQrScreen(viewModel: CreateQrViewModel, onDone: () -> Unit) {
    val context = LocalContext.current
    SecureScreen()

    LaunchedEffect(viewModel.saved) {
        if (viewModel.saved) onDone()
    }

    Scaffold(topBar = { BackTopBar("Add QR code", onDone) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Print this code and stick it somewhere you'd have to walk to, like the " +
                    "fridge or another room. The app won't keep a copy, so print it now.",
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedTextField(
                value = viewModel.label,
                onValueChange = { viewModel.label = it.take(40) },
                label = { Text("Name (e.g. Fridge)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            QrCodeImage(
                content = viewModel.payload,
                contentDescription = "Your new QR key",
                modifier = Modifier.size(240.dp),
            )
            Button(
                onClick = {
                    printQrKey(context, viewModel.payload, viewModel.caption)
                    viewModel.printed = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Print or save as PDF") }
            OutlinedButton(
                onClick = viewModel::save,
                enabled = viewModel.printed && !viewModel.saving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("I've printed it. Save key") }
        }
    }
}
