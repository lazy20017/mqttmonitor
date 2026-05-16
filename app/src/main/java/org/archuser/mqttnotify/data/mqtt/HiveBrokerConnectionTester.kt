package org.archuser.mqttnotify.data.mqtt

import com.hivemq.client.mqtt.MqttClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.archuser.mqttnotify.domain.model.BrokerConfig
import org.archuser.mqttnotify.domain.model.ProtocolVersion

@Singleton
class HiveBrokerConnectionTester @Inject constructor() : BrokerConnectionTester {

    override suspend fun test(config: BrokerConfig, password: String?): Result<Unit> = withContext(Dispatchers.IO) {
        when (config.protocolVersion) {
            ProtocolVersion.MQTT_5_0 -> testV5(config, password)
            ProtocolVersion.MQTT_3_1_1 -> testV3(config, password)
            ProtocolVersion.AUTO -> testV5(config, password).recoverCatching { testV3(config, password).getOrThrow() }
        }
    }

    private fun testV5(config: BrokerConfig, password: String?): Result<Unit> = runCatching {
        val builder = MqttClient.builder().useMqttVersion5().serverHost(config.host).serverPort(config.port)
        if (!config.clientId.isNullOrBlank()) {
            builder.identifier(config.clientId)
        }
        if (config.tls) {
            builder.sslWithDefaultConfig()
        }

        val client = builder.buildAsync()
        val connectBuilder = client.connectWith().cleanStart(config.cleanStart).keepAlive(config.keepaliveSec)
        if (!config.username.isNullOrBlank()) {
            val authBuilder = connectBuilder.simpleAuth().username(config.username)
            if (!password.isNullOrBlank()) {
                authBuilder.password(password.toByteArray())
            }
            authBuilder.applySimpleAuth()
        }
        connectBuilder.send().join()
        client.disconnectWith().send().join()
    }

    private fun testV3(config: BrokerConfig, password: String?): Result<Unit> = runCatching {
        val builder = MqttClient.builder().useMqttVersion3().serverHost(config.host).serverPort(config.port)
        if (!config.clientId.isNullOrBlank()) {
            builder.identifier(config.clientId)
        }
        if (config.tls) {
            builder.sslWithDefaultConfig()
        }

        val client = builder.buildAsync()
        val connectBuilder = client.connectWith().cleanSession(config.cleanStart).keepAlive(config.keepaliveSec)
        if (!config.username.isNullOrBlank()) {
            val authBuilder = connectBuilder.simpleAuth().username(config.username)
            if (!password.isNullOrBlank()) {
                authBuilder.password(password.toByteArray())
            }
            authBuilder.applySimpleAuth()
        }
        connectBuilder.send().join()
        client.disconnect().join()
    }
}
