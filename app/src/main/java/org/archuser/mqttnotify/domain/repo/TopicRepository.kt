package org.archuser.mqttnotify.domain.repo

import kotlinx.coroutines.flow.Flow
import org.archuser.mqttnotify.domain.model.TopicSubscriptionConfig

interface TopicRepository {
    fun observeTopicsForBroker(brokerId: Long): Flow<List<TopicSubscriptionConfig>>
    suspend fun getEnabledTopicsForBroker(brokerId: Long): List<TopicSubscriptionConfig>
    suspend fun upsertTopic(config: TopicSubscriptionConfig): Long
    suspend fun deleteTopic(id: Long)
}
