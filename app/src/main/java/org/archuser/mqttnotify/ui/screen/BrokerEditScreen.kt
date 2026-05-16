package org.archuser.mqttnotify.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import org.archuser.mqttnotify.domain.model.ProtocolVersion
import org.archuser.mqttnotify.ui.viewmodel.BrokerEditUiState

@Composable
fun BrokerEditScreen(
    state: BrokerEditUiState,
    onChange: (BrokerEditUiState) -> Unit,
    onTest: () -> Unit,
    onSave: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val doneKeyboardActions = KeyboardActions(
        onDone = {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    )
    val doneKeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = state.label,
            onValueChange = { onChange(state.copy(label = it)) },
            label = { Text("Label") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = doneKeyboardOptions,
            keyboardActions = doneKeyboardActions
        )
        OutlinedTextField(
            value = state.host,
            onValueChange = { onChange(state.copy(host = it)) },
            label = { Text("Host") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = doneKeyboardOptions,
            keyboardActions = doneKeyboardActions
        )
        OutlinedTextField(
            value = state.port,
            onValueChange = { onChange(state.copy(port = it)) },
            label = { Text("Port") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = doneKeyboardOptions,
            keyboardActions = doneKeyboardActions
        )

        Text("Protocol")
        ProtocolVersion.entries.forEach { option ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(
                    selected = state.protocol == option,
                    onClick = { onChange(state.copy(protocol = option)) }
                )
                Text(option.name)
            }
        }
        Text(
            "Tip: AUTO tries MQTT 5 first, then falls back to 3.1.1 for compatibility.",
            style = MaterialTheme.typography.bodySmall
        )

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked = state.tls,
                onCheckedChange = {
                    val nextPort = when {
                        it && state.port == "1883" -> "8883"
                        !it && state.port == "8883" -> "1883"
                        else -> state.port
                    }
                    onChange(state.copy(tls = it, port = nextPort))
                }
            )
            Text("Use TLS")
        }
        Text(
            "Tip: TLS encrypts broker traffic. Default secure MQTT port is 8883.",
            style = MaterialTheme.typography.bodySmall
        )

        OutlinedTextField(
            value = state.username,
            onValueChange = { onChange(state.copy(username = it)) },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = doneKeyboardOptions,
            keyboardActions = doneKeyboardActions
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = { onChange(state.copy(password = it)) },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = doneKeyboardOptions,
            keyboardActions = doneKeyboardActions
        )
        OutlinedTextField(
            value = state.clientId,
            onValueChange = { onChange(state.copy(clientId = it)) },
            label = { Text("Client ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = doneKeyboardOptions,
            keyboardActions = doneKeyboardActions
        )
        OutlinedTextField(
            value = state.keepaliveSec,
            onValueChange = { onChange(state.copy(keepaliveSec = it)) },
            label = { Text("Keepalive seconds") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = doneKeyboardOptions,
            keyboardActions = doneKeyboardActions
        )

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked = state.cleanStart,
                onCheckedChange = { onChange(state.copy(cleanStart = it)) }
            )
            Text("Clean start")
        }
        Text(
            "Tip: disable clean start to resume an existing broker session when supported.",
            style = MaterialTheme.typography.bodySmall
        )

        OutlinedTextField(
            value = state.sessionExpirySec,
            onValueChange = { onChange(state.copy(sessionExpirySec = it)) },
            label = { Text("Session expiry seconds") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = doneKeyboardOptions,
            keyboardActions = doneKeyboardActions
        )
        Text(
            "Tip: session expiry controls how long the broker keeps your session after disconnect.",
            style = MaterialTheme.typography.bodySmall
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onTest, enabled = !state.isTesting) {
                Text(if (state.isTesting) "Testing..." else "Test Connection")
            }
            Button(onClick = onSave) { Text("Save") }
        }

        state.status?.let {
            Text(
                text = it,
                color = if (it.contains("failed", true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}
