package org.archuser.mqttnotify.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archuser.mqttnotify.ui.viewmodel.BrokerListUiState

@Composable
fun BrokerListScreen(
    state: BrokerListUiState,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onSetActive: (Long) -> Unit,
    onOpenTopics: (Long) -> Unit,
    onOpenMessages: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAdd) { Text("Add Broker") }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.brokers, key = { it.id }) { broker ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = broker.label,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("${broker.host}:${broker.port} | ${broker.protocolVersion}")
                        Text(if (state.activeBrokerId == broker.id) "Active broker" else "Saved broker")

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { onSetActive(broker.id) }) { Text("Use") }
                            OutlinedButton(onClick = { onEdit(broker.id) }) { Text("Edit") }
                            OutlinedButton(onClick = { onOpenTopics(broker.id) }) { Text("Topics") }
                            OutlinedButton(onClick = { onOpenMessages(broker.id) }) { Text("Messages") }
                            OutlinedButton(onClick = { onDelete(broker.id) }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}
