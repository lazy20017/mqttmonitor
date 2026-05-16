package org.archuser.mqttnotify.domain.model

data class RetentionPolicy(
    val id: Long,
    val brokerId: Long?,
    val topicFilter: String?,
    val maxMessages: Int,
    val maxAgeDays: Int,
    val trimOnInsert: Boolean
)
