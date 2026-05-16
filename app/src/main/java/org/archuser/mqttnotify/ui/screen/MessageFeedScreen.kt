package org.archuser.mqttnotify.ui.screen

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.archuser.mqttnotify.domain.model.InboundMessageRecord

@Composable
fun MessageFeedScreen(
    messages: List<InboundMessageRecord>,
    onReadAll: () -> Unit,
    onMarkMessageRead: (Long) -> Unit,
    onMarkMessageUnread: (Long) -> Unit,
    onDeleteMessage: (Long) -> Unit
) {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    val hasUnread = messages.any { it.isUnread }

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onReadAll, enabled = hasUnread) {
                    Text("Read all")
                }
            }
        }

        items(messages, key = { it.id }) { msg ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(msg.topic, style = MaterialTheme.typography.titleSmall)
                            if (msg.retained) {
                                Text("retained", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text(formatter.format(Date(msg.receivedAt)))
                        Text(msg.payloadPreview)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("QoS ${msg.qos}")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = { onMarkMessageRead(msg.id) }, enabled = msg.isUnread) {
                                Text("Mark read")
                            }
                            Button(onClick = { onMarkMessageUnread(msg.id) }, enabled = !msg.isUnread) {
                                Text("Mark unread")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = { onDeleteMessage(msg.id) }) { Text("Delete message") }
                        }
                    }

                    if (msg.isUnread) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 10.dp, end = 10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .size(10.dp)
                        )
                    }
                }
            }
        }
    }
}
