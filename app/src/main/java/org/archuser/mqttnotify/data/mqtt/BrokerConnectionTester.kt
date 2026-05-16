package org.archuser.mqttnotify.data.mqtt

import org.archuser.mqttnotify.domain.model.BrokerConfig

interface BrokerConnectionTester {
    suspend fun test(config: BrokerConfig, password: String?): Result<Unit>
}
