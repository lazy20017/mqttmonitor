package org.archuser.mqttnotify.domain.repo

import kotlinx.coroutines.flow.Flow
import org.archuser.mqttnotify.data.mqtt.MqttEvent
import org.archuser.mqttnotify.domain.model.InboundMessageRecord

interface MessageRepository {
    fun observeMessagesForBroker(brokerId: Long): Flow<List<InboundMessageRecord>>
    suspend fun ingestMessage(brokerId: Long, event: MqttEvent.MessageReceived): InboundMessageRecord
    suspend fun resetUnreadForTopic(brokerId: Long, topic: String)
    suspend fun resetUnreadForBroker(brokerId: Long)
    suspend fun markMessageRead(messageId: Long)
    suspend fun markMessageUnread(messageId: Long)
    suspend fun unreadCountForBroker(brokerId: Long): Int
    suspend fun deleteMessage(messageId: Long)
}
