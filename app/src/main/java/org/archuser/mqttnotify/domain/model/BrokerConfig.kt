package org.archuser.mqttnotify.domain.model

data class BrokerCredentialsRef(
    val alias: String
)

data class BrokerConfig(
    val id: Long,
    val label: String,
    val host: String,
    val port: Int,
    val tls: Boolean,
    val protocolVersion: ProtocolVersion,
    val username: String?,
    val credentialsRef: BrokerCredentialsRef?,
    val clientId: String?,
    val keepaliveSec: Int,
    val cleanStart: Boolean,
    val sessionExpirySec: Int,
    val lastTestPassedAt: Long?
)
