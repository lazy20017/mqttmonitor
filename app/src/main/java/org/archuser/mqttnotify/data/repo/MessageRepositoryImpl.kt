package org.archuser.mqttnotify.data.repo

import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.archuser.mqttnotify.core.DispatchersProvider
import org.archuser.mqttnotify.core.TimeProvider
import org.archuser.mqttnotify.core.TopicMatcher
import org.archuser.mqttnotify.data.local.MessageDao
import org.archuser.mqttnotify.data.local.MessageEntity
import org.archuser.mqttnotify.data.local.TopicCounterDao
import org.archuser.mqttnotify.data.local.TopicCounterEntity
import org.archuser.mqttnotify.data.local.TopicSubscriptionDao
import org.archuser.mqttnotify.data.mqtt.MqttEvent
import org.archuser.mqttnotify.domain.model.InboundMessageRecord
import org.archuser.mqttnotify.domain.repo.MessageRepository
import org.archuser.mqttnotify.domain.repo.RetentionRepository

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val topicDao: TopicSubscriptionDao,
    private val topicCounterDao: TopicCounterDao,
    private val retentionRepository: RetentionRepository,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatchersProvider
) : MessageRepository {

    override fun observeMessagesForBroker(brokerId: Long): Flow<List<InboundMessageRecord>> =
        messageDao.observeForBroker(brokerId).map { list -> list.map { it.toModel() } }

    override suspend fun ingestMessage(
        brokerId: Long,
        event: MqttEvent.MessageReceived
    ): InboundMessageRecord = withContext(dispatchers.io) {
        val matchingConfig = topicDao.getAllForBroker(brokerId)
            .filter { it.enabled && TopicMatcher.matches(it.topicFilter, event.topic) }
            .maxByOrNull { it.topicFilter.length }

        val retainedAsNew = matchingConfig?.retainedAsNew ?: false
        val isNewActivity = !event.retained || retainedAsNew

        val preview = runCatching {
            String(event.payload, StandardCharsets.UTF_8).take(PREVIEW_MAX_LEN)
        }.getOrDefault("<binary payload>")

        val entity = MessageEntity(
            brokerId = brokerId,
            topicFilter = event.topic,
            receivedAt = timeProvider.nowMillis(),
            payloadBlob = event.payload,
            payloadTextPreview = preview,
            qos = event.qos,
            retained = event.retained,
            duplicate = event.duplicate,
            packetId = event.packetId,
            isNewActivity = isNewActivity,
            isUnread = isNewActivity
        )

        val insertedId = messageDao.insert(entity)

        val currentCounter = topicCounterDao.getCounter(brokerId, event.topic)
        val nextCounter = TopicCounterEntity(
            brokerId = brokerId,
            topicFilter = event.topic,
            unreadCount = (currentCounter?.unreadCount ?: 0) + if (isNewActivity) 1 else 0,
            totalCount = (currentCounter?.totalCount ?: 0) + 1
        )
        topicCounterDao.upsert(nextCounter)

        val policy = retentionRepository.policyForTopic(brokerId, event.topic)
        if (policy.trimOnInsert) {
            val cutoff = timeProvider.nowMillis() - policy.maxAgeDays * DAY_MS
            messageDao.deleteOlderThan(brokerId, event.topic, cutoff)

            val currentCount = messageDao.countForTopic(brokerId, event.topic)
            if (currentCount > policy.maxMessages) {
                messageDao.deleteOverflowForTopic(brokerId, event.topic, policy.maxMessages)
            }
        }

        entity.copy(id = insertedId).toModel()
    }

    override suspend fun resetUnreadForTopic(brokerId: Long, topic: String) = withContext(dispatchers.io) {
        messageDao.markAllReadForTopic(brokerId, topic)
        topicCounterDao.resetUnreadForTopic(brokerId, topic)
    }

    override suspend fun resetUnreadForBroker(brokerId: Long) = withContext(dispatchers.io) {
        messageDao.markAllReadForBroker(brokerId)
        topicCounterDao.resetUnreadForBroker(brokerId)
    }

    override suspend fun markMessageRead(messageId: Long) = withContext(dispatchers.io) {
        val message = messageDao.getById(messageId) ?: return@withContext
        if (!message.isUnread) return@withContext
        messageDao.markReadById(messageId)
        topicCounterDao.decrementUnreadForTopic(message.brokerId, message.topicFilter)
    }

    override suspend fun markMessageUnread(messageId: Long) = withContext(dispatchers.io) {
        val message = messageDao.getById(messageId) ?: return@withContext
        if (message.isUnread) return@withContext
        messageDao.markUnreadById(messageId)
        topicCounterDao.incrementUnreadForTopic(message.brokerId, message.topicFilter)
    }

    override suspend fun unreadCountForBroker(brokerId: Long): Int = withContext(dispatchers.io) {
        topicCounterDao.unreadCountForBroker(brokerId)
    }

    override suspend fun deleteMessage(messageId: Long) = withContext(dispatchers.io) {
        val message = messageDao.getById(messageId) ?: return@withContext
        messageDao.deleteById(messageId)
        topicCounterDao.decrementCountsForDeletedMessage(
            brokerId = message.brokerId,
            topic = message.topicFilter,
            unreadDelta = if (message.isUnread) 1 else 0
        )
    }

    private companion object {
        private const val PREVIEW_MAX_LEN = 500
        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
