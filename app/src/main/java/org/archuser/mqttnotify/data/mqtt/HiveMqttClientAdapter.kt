package org.archuser.mqttnotify.data.mqtt

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.archuser.mqttnotify.domain.model.BrokerConfig
import org.archuser.mqttnotify.domain.model.ConnectionStatus
import org.archuser.mqttnotify.domain.model.ProtocolVersion

@Singleton
class HiveMqttClientAdapter @Inject constructor() : MqttClientAdapter {

    private val eventsFlow = MutableSharedFlow<MqttEvent>(extraBufferCapacity = 256)
    private val lock = Mutex()
    private val callbackScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var activeClient: ConnectedClient? = null

    override suspend fun connect(config: BrokerConfig, password: String?): Result<Unit> = lock.withLock {
        disconnectInternal()
        eventsFlow.tryEmit(MqttEvent.ConnectionChanged(ConnectionStatus.CONNECTING))

        return when (config.protocolVersion) {
            ProtocolVersion.MQTT_5_0 -> connectV5(config, password)
            ProtocolVersion.MQTT_3_1_1 -> connectV3(config, password)
            ProtocolVersion.AUTO -> {
                connectV5(config, password).recoverCatching {
                    connectV3(config, password).getOrThrow()
                }
            }
        }
    }

    override suspend fun disconnect() {
        lock.withLock { disconnectInternal() }
    }

    override suspend fun subscribe(topic: String, qos: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            when (val client = activeClient) {
                is ConnectedClient.V5 -> {
                    client.client.subscribeWith()
                        .topicFilter(topic)
                        .qos(qos.toMqttQos())
                        .callback { publish -> onV5Message(publish) }
                        .send()
                        .join()
                }
                is ConnectedClient.V3 -> {
                    client.client.subscribeWith()
                        .topicFilter(topic)
                        .qos(qos.toMqttQos())
                        .callback { publish -> onV3Message(publish) }
                        .send()
                        .join()
                }
                null -> error("Cannot subscribe before connection")
            }
            eventsFlow.tryEmit(MqttEvent.SubscriptionAck(topic))
            Unit
        }.onFailure {
            eventsFlow.tryEmit(MqttEvent.Error("Subscription failed for $topic", it))
        }
    }

    override suspend fun unsubscribe(topic: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            when (val client = activeClient) {
                is ConnectedClient.V5 -> client.client.unsubscribeWith().topicFilter(topic).send().join()
                is ConnectedClient.V3 -> client.client.unsubscribeWith().topicFilter(topic).send().join()
                null -> error("Cannot unsubscribe before connection")
            }
            Unit
        }.onFailure {
            eventsFlow.tryEmit(MqttEvent.Error("Unsubscribe failed for $topic", it))
        }
    }

    override fun events(): Flow<MqttEvent> = eventsFlow.asSharedFlow()

    private suspend fun connectV5(config: BrokerConfig, password: String?): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val builder = MqttClient.builder()
                .useMqttVersion5()
                .serverHost(config.host)
                .serverPort(config.port)

            if (!config.clientId.isNullOrBlank()) {
                builder.identifier(config.clientId)
            }
            if (config.tls) {
                builder.sslWithDefaultConfig()
            }

            val client = builder.buildAsync()
            val connectBuilder = client.connectWith()
                .keepAlive(config.keepaliveSec)
                .cleanStart(config.cleanStart)

            if (!config.username.isNullOrBlank()) {
                val authBuilder = connectBuilder.simpleAuth().username(config.username)
                if (!password.isNullOrBlank()) {
                    authBuilder.password(password.toByteArray())
                }
                authBuilder.applySimpleAuth()
            }

            connectBuilder.send().join()
            activeClient = ConnectedClient.V5(client)
            eventsFlow.tryEmit(MqttEvent.ConnectionChanged(ConnectionStatus.CONNECTED))
            Unit
        }.onFailure {
            eventsFlow.tryEmit(MqttEvent.ConnectionChanged(ConnectionStatus.ERROR))
            eventsFlow.tryEmit(MqttEvent.Error("MQTT v5 connection failed", it))
        }
    }

    private suspend fun connectV3(config: BrokerConfig, password: String?): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val builder = MqttClient.builder()
                .useMqttVersion3()
                .serverHost(config.host)
                .serverPort(config.port)

            if (!config.clientId.isNullOrBlank()) {
                builder.identifier(config.clientId)
            }
            if (config.tls) {
                builder.sslWithDefaultConfig()
            }

            val client = builder.buildAsync()
            val connectBuilder = client.connectWith()
                .keepAlive(config.keepaliveSec)
                .cleanSession(config.cleanStart)

            if (!config.username.isNullOrBlank()) {
                val authBuilder = connectBuilder.simpleAuth().username(config.username)
                if (!password.isNullOrBlank()) {
                    authBuilder.password(password.toByteArray())
                }
                authBuilder.applySimpleAuth()
            }

            connectBuilder.send().join()
            activeClient = ConnectedClient.V3(client)
            eventsFlow.tryEmit(MqttEvent.ConnectionChanged(ConnectionStatus.CONNECTED))
            Unit
        }.onFailure {
            eventsFlow.tryEmit(MqttEvent.ConnectionChanged(ConnectionStatus.ERROR))
            eventsFlow.tryEmit(MqttEvent.Error("MQTT v3.1.1 connection failed", it))
        }
    }

    private suspend fun disconnectInternal() = withContext(Dispatchers.IO) {
        runCatching {
            when (val client = activeClient) {
                is ConnectedClient.V5 -> client.client.disconnectWith().send().join()
                is ConnectedClient.V3 -> client.client.disconnect().join()
                null -> Unit
            }
        }
        activeClient = null
        eventsFlow.tryEmit(MqttEvent.ConnectionChanged(ConnectionStatus.DISCONNECTED))
    }

    private fun onV5Message(publish: Mqtt5Publish) {
        callbackScope.launch {
            eventsFlow.emit(
                MqttEvent.MessageReceived(
                    topic = publish.topic.toString(),
                    payload = publish.payloadAsBytes,
                    qos = publish.qos.code,
                    retained = publish.isRetain,
                    duplicate = false,
                    packetId = null
                )
            )
        }
    }

    private fun onV3Message(publish: Mqtt3Publish) {
        callbackScope.launch {
            eventsFlow.emit(
                MqttEvent.MessageReceived(
                    topic = publish.topic.toString(),
                    payload = publish.payloadAsBytes,
                    qos = publish.qos.code,
                    retained = publish.isRetain,
                    duplicate = false,
                    packetId = null
                )
            )
        }
    }

    private fun Int.toMqttQos(): MqttQos = when (this) {
        0 -> MqttQos.AT_MOST_ONCE
        2 -> MqttQos.EXACTLY_ONCE
        else -> MqttQos.AT_LEAST_ONCE
    }

    private sealed interface ConnectedClient {
        data class V5(val client: Mqtt5AsyncClient) : ConnectedClient
        data class V3(val client: Mqtt3AsyncClient) : ConnectedClient
    }
}
