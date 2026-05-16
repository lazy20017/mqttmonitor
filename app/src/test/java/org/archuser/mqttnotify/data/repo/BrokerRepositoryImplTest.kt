package org.archuser.mqttnotify.data.repo

import kotlinx.coroutines.runBlocking
import org.archuser.mqttnotify.core.DispatchersProvider
import org.archuser.mqttnotify.core.TimeProvider
import org.archuser.mqttnotify.data.local.BrokerDao
import org.archuser.mqttnotify.data.local.BrokerEntity
import org.archuser.mqttnotify.domain.model.BrokerConfig
import org.archuser.mqttnotify.domain.model.ProtocolVersion
import org.archuser.mqttnotify.domain.repo.RetentionRepository
import org.junit.Assert.assertTrue
import org.junit.Test

class BrokerRepositoryImplTest {

    @Test
    fun `rejects broker save without recent successful test`() = runBlocking {
        val repo = BrokerRepositoryImpl(
            brokerDao = FakeBrokerDao(),
            retentionRepository = FakeRetentionRepository(),
            timeProvider = object : TimeProvider { override fun nowMillis(): Long = 500_000L },
            dispatchers = TestDispatchers
        )

        val stale = BrokerConfig(
            id = 0,
            label = "test",
            host = "localhost",
            port = 1883,
            tls = false,
            protocolVersion = ProtocolVersion.AUTO,
            username = null,
            credentialsRef = null,
            clientId = null,
            keepaliveSec = 60,
            cleanStart = true,
            sessionExpirySec = 0,
            lastTestPassedAt = 0L
        )

        assertTrue(repo.saveBroker(stale).isFailure)
    }

    private class FakeBrokerDao : BrokerDao {
        override fun observeAll() = kotlinx.coroutines.flow.flowOf(emptyList<BrokerEntity>())
        override suspend fun getById(id: Long): BrokerEntity? = null
        override suspend fun insert(entity: BrokerEntity): Long = 1L
        override suspend fun update(entity: BrokerEntity) {}
        override suspend fun delete(entity: BrokerEntity) {}
    }

    private class FakeRetentionRepository : RetentionRepository {
        override suspend fun policyForTopic(brokerId: Long, topic: String) = throw NotImplementedError()
        override suspend fun upsertPolicy(policy: org.archuser.mqttnotify.domain.model.RetentionPolicy): Long = 1L
        override suspend fun ensureDefaultForBroker(brokerId: Long) {}
    }

    private object TestDispatchers : DispatchersProvider {
        override val io = kotlinx.coroutines.Dispatchers.Unconfined
        override val default = kotlinx.coroutines.Dispatchers.Unconfined
        override val main = kotlinx.coroutines.Dispatchers.Unconfined
    }
}
