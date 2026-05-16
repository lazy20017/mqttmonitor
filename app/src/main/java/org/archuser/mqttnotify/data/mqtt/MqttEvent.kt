package org.archuser.mqttnotify.data.mqtt

import org.archuser.mqttnotify.domain.model.ConnectionStatus

sealed class MqttEvent {
    data class ConnectionChanged(val status: ConnectionStatus) : MqttEvent()
    data class MessageReceived(
        val topic: String,
        val payload: ByteArray,
        val qos: Int,
        val retained: Boolean,
        val duplicate: Boolean,
        val packetId: Int?
    ) : MqttEvent()

    data class SubscriptionAck(val topic: String) : MqttEvent()
    data class Error(val message: String, val throwable: Throwable? = null) : MqttEvent()
}
