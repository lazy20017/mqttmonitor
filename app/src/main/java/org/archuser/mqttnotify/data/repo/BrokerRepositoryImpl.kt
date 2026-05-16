package org.archuser.mqttnotify.data.repo

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.archuser.mqttnotify.core.DispatchersProvider
import org.archuser.mqttnotify.core.TimeProvider
import org.archuser.mqttnotify.data.local.BrokerDao
import org.archuser.mqttnotify.domain.model.BrokerConfig
import org.archuser.mqttnotify.domain.repo.BrokerRepository
import org.archuser.mqttnotify.domain.repo.RetentionRepository

@Singleton
class BrokerRepositoryImpl @Inject constructor(
    private val brokerDao: BrokerDao,
    private val retentionRepository: RetentionRepository,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatchersProvider
) : BrokerRepository {

    override fun observeBrokers(): Flow<List<BrokerConfig>> =
        brokerDao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun getBroker(id: Long): BrokerConfig? = withContext(dispatchers.io) {
        brokerDao.getById(id)?.toModel()
    }

    override suspend fun saveBroker(config: BrokerConfig): Result<Long> = withContext(dispatchers.io) {
        val now = timeProvider.nowMillis()
        val testedAt = config.lastTestPassedAt
            ?: return@withContext Result.failure(IllegalStateException("Broker must pass connection test before save"))

        if (now - testedAt > TEST_WINDOW_MS) {
            return@withContext Result.failure(IllegalStateException("Connection test expired. Re-test before save."))
        }

        runCatching {
            val id = if (config.id == 0L) {
                brokerDao.insert(config.toEntity())
            } else {
                brokerDao.update(config.toEntity())
                config.id
            }
            retentionRepository.ensureDefaultForBroker(id)
            id
        }
    }

    override suspend fun deleteBroker(id: Long) = withContext(dispatchers.io) {
        val entity = brokerDao.getById(id)
        if (entity != null) {
            brokerDao.delete(entity)
        }
    }

    private companion object {
        private const val TEST_WINDOW_MS = 5 * 60 * 1000L
    }
}
