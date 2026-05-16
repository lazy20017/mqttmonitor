package org.archuser.mqttnotify.domain.model

data class TopicSubscriptionConfig(
    val id: Long,
    val brokerId: Long,
    val topicFilter: String,
    val qos: Int,
    val enabled: Boolean,
    val notifyEnabled: Boolean,
    val retainedAsNew: Boolean
)
