package org.archuser.mqttnotify.domain.model

data class InboundMessageRecord(
    val id: Long,
    val brokerId: Long,
    val topic: String,
    val receivedAt: Long,
    val payload: ByteArray,
    val payloadPreview: String,
    val qos: Int,
    val retained: Boolean,
    val duplicate: Boolean,
    val packetId: Int?,
    val isNewActivity: Boolean,
    val isUnread: Boolean
)
