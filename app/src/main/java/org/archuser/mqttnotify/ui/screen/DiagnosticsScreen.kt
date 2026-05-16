package org.archuser.mqttnotify.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archuser.mqttnotify.domain.model.ConnectionSnapshot

@Composable
fun DiagnosticsScreen(snapshot: ConnectionSnapshot, events: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Connection status: ${snapshot.status}")
                Text("Broker: ${snapshot.brokerLabel ?: "None"}")
                Text("Connected since: ${snapshot.connectedSince ?: 0}")
                Text("Message count: ${snapshot.messageCount}")
                Text("Last error: ${snapshot.lastError ?: "None"}")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(events) { item ->
                Text(item)
            }
        }
    }
}
