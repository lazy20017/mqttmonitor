package org.archuser.mqttnotify.data.repo

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import org.archuser.mqttnotify.core.DispatchersProvider
import org.archuser.mqttnotify.data.local.RetentionPolicyDao
import org.archuser.mqttnotify.domain.model.RetentionPolicy
import org.archuser.mqttnotify.domain.repo.RetentionRepository

@Singleton
class RetentionRepositoryImpl @Inject constructor(
    private val retentionDao: RetentionPolicyDao,
    private val dispatchers: DispatchersProvider
) : RetentionRepository {

    override suspend fun policyForTopic(brokerId: Long, topic: String): RetentionPolicy = withContext(dispatchers.io) {
        retentionDao.getTopicPolicy(brokerId, topic)?.toModel()
            ?: retentionDao.getBrokerDefault(brokerId)?.toModel()
            ?: retentionDao.getGlobalDefault()?.toModel()
            ?: defaultGlobalPolicy().also { retentionDao.upsert(it.toEntity()) }
    }

    override suspend fun upsertPolicy(policy: RetentionPolicy): Long = withContext(dispatchers.io) {
        retentionDao.upsert(policy.toEntity())
    }

    override suspend fun ensureDefaultForBroker(brokerId: Long) = withContext(dispatchers.io) {
        if (retentionDao.getBrokerDefault(brokerId) == null) {
            retentionDao.upsert(
                RetentionPolicy(
                    id = 0,
                    brokerId = brokerId,
                    topicFilter = null,
                    maxMessages = DEFAULT_MAX_MESSAGES,
                    maxAgeDays = DEFAULT_MAX_AGE_DAYS,
                    trimOnInsert = true
                ).toEntity()
            )
        }
    }

    private fun defaultGlobalPolicy(): RetentionPolicy = RetentionPolicy(
        id = 0,
        brokerId = null,
        topicFilter = null,
        maxMessages = DEFAULT_MAX_MESSAGES,
        maxAgeDays = DEFAULT_MAX_AGE_DAYS,
        trimOnInsert = true
    )

    private companion object {
        private const val DEFAULT_MAX_MESSAGES = 1000
        private const val DEFAULT_MAX_AGE_DAYS = 30
    }
}
