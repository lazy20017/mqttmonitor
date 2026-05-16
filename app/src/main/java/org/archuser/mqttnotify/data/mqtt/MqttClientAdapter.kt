package org.archuser.mqttnotify.data.mqtt

import kotlinx.coroutines.flow.Flow
import org.archuser.mqttnotify.domain.model.BrokerConfig

interface MqttClientAdapter {
    suspend fun connect(config: BrokerConfig, password: String?): Result<Unit>
    suspend fun disconnect()
    suspend fun subscribe(topic: String, qos: Int): Result<Unit>
    suspend fun unsubscribe(topic: String): Result<Unit>
    fun events(): Flow<MqttEvent>
}
