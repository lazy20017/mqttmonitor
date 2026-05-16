package org.archuser.mqttnotify.domain.repo

import kotlinx.coroutines.flow.Flow
import org.archuser.mqttnotify.domain.model.BrokerConfig

interface BrokerRepository {
    fun observeBrokers(): Flow<List<BrokerConfig>>
    suspend fun getBroker(id: Long): BrokerConfig?
    suspend fun saveBroker(config: BrokerConfig): Result<Long>
    suspend fun deleteBroker(id: Long)
}
