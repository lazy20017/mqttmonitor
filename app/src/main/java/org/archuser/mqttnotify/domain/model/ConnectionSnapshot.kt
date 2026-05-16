package org.archuser.mqttnotify.domain.model

data class ConnectionSnapshot(
    val status: ConnectionStatus,
    val brokerLabel: String?,
    val connectedSince: Long?,
    val messageCount: Long,
    val lastError: String?
)
