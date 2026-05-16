package org.archuser.mqttnotify.domain.repo

import org.archuser.mqttnotify.domain.model.RetentionPolicy

interface RetentionRepository {
    suspend fun policyForTopic(brokerId: Long, topic: String): RetentionPolicy
    suspend fun upsertPolicy(policy: RetentionPolicy): Long
    suspend fun ensureDefaultForBroker(brokerId: Long)
}
