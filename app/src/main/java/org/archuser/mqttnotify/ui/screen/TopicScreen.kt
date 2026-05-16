package org.archuser.mqttnotify.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.archuser.mqttnotify.domain.model.TopicSubscriptionConfig

@Composable
fun TopicScreen(
    topics: List<TopicSubscriptionConfig>,
    onAddTopic: (String, Int, Boolean, Boolean) -> Unit,
    onToggleEnabled: (TopicSubscriptionConfig, Boolean) -> Unit,
    onDelete: (Long) -> Unit
) {
    var filter by remember { mutableStateOf("") }
    var qos by remember { mutableStateOf("1") }
    var notifyEnabled by remember { mutableStateOf(true) }
    var retainedAsNew by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val doneKeyboardActions = KeyboardActions(
        onDone = {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    )
    val doneKeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = filter,
            onValueChange = { filter = it },
            label = { Text("Topic filter") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = doneKeyboardOptions,
            keyboardActions = doneKeyboardActions
        )
        OutlinedTextField(
            value = qos,
            onValueChange = { qos = it },
            label = { Text("QoS (0-2)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = doneKeyboardOptions,
            keyboardActions = doneKeyboardActions
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = notifyEnabled, onCheckedChange = { notifyEnabled = it })
            Text("Notifications enabled")
        }
        Text(
            "Tip: notifications are optional. Messages still ingest when alerts are disabled.",
            style = MaterialTheme.typography.bodySmall
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = retainedAsNew, onCheckedChange = { retainedAsNew = it })
            Text("Count retained as new")
        }
        Text(
            "Tip: retained MQTT messages are historical state by default and usually not counted as new activity.",
            style = MaterialTheme.typography.bodySmall
        )

        Button(onClick = {
            onAddTopic(filter, qos.toIntOrNull() ?: 1, notifyEnabled, retainedAsNew)
            filter = ""
        }) {
            Text("Add topic")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(topics, key = { it.id }) { topic ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(topic.topicFilter)
                        Text("QoS ${topic.qos} | notify=${topic.notifyEnabled} | retainedAsNew=${topic.retainedAsNew}")

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onToggleEnabled(topic, !topic.enabled) }) {
                                Text(if (topic.enabled) "Disable" else "Enable")
                            }
                            Button(onClick = { onDelete(topic.id) }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
