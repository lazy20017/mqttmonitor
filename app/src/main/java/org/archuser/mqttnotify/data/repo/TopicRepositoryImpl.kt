package org.archuser.mqttnotify.data.repo

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.archuser.mqttnotify.core.DispatchersProvider
import org.archuser.mqttnotify.data.local.TopicSubscriptionDao
import org.archuser.mqttnotify.domain.model.TopicSubscriptionConfig
import org.archuser.mqttnotify.domain.repo.TopicRepository

@Singleton
class TopicRepositoryImpl @Inject constructor(
    private val topicDao: TopicSubscriptionDao,
    private val dispatchers: DispatchersProvider
) : TopicRepository {

    override fun observeTopicsForBroker(brokerId: Long): Flow<List<TopicSubscriptionConfig>> =
        topicDao.observeForBroker(brokerId).map { list -> list.map { it.toModel() } }

    override suspend fun getEnabledTopicsForBroker(brokerId: Long): List<TopicSubscriptionConfig> =
        withContext(dispatchers.io) { topicDao.getEnabledForBroker(brokerId).map { it.toModel() } }

    override suspend fun upsertTopic(config: TopicSubscriptionConfig): Long = withContext(dispatchers.io) {
        topicDao.upsert(config.toEntity())
    }

    override suspend fun deleteTopic(id: Long) = withContext(dispatchers.io) {
        topicDao.deleteById(id)
    }
}
