package org.archuser.mqttnotify.data.repo

import org.archuser.mqttnotify.data.local.AppStateEntity
import org.archuser.mqttnotify.data.local.BrokerEntity
import org.archuser.mqttnotify.data.local.MessageEntity
import org.archuser.mqttnotify.data.local.RetentionPolicyEntity
import org.archuser.mqttnotify.data.local.TopicSubscriptionEntity
import org.archuser.mqttnotify.domain.model.AppState
import org.archuser.mqttnotify.domain.model.BrokerConfig
import org.archuser.mqttnotify.domain.model.BrokerCredentialsRef
import org.archuser.mqttnotify.domain.model.ConnectionMode
import org.archuser.mqttnotify.domain.model.InboundMessageRecord
import org.archuser.mqttnotify.domain.model.ProtocolVersion
import org.archuser.mqttnotify.domain.model.RetentionPolicy
import org.archuser.mqttnotify.domain.model.TopicSubscriptionConfig

fun BrokerEntity.toModel(): BrokerConfig = BrokerConfig(
    id = id,
    label = label,
    host = host,
    port = port,
    tls = tls,
    protocolVersion = runCatching { ProtocolVersion.valueOf(protocolPref) }.getOrDefault(ProtocolVersion.AUTO),
    username = username,
    credentialsRef = credentialRef?.let(::BrokerCredentialsRef),
    clientId = clientId,
    keepaliveSec = keepaliveSec,
    cleanStart = cleanStart,
    sessionExpirySec = sessionExpirySec,
    lastTestPassedAt = lastTestPassedAt
)

fun BrokerConfig.toEntity(): BrokerEntity = BrokerEntity(
    id = id,
    label = label,
    host = host,
    port = port,
    tls = tls,
    protocolPref = protocolVersion.name,
    username = username,
    credentialRef = credentialsRef?.alias,
    clientId = clientId,
    keepaliveSec = keepaliveSec,
    cleanStart = cleanStart,
    sessionExpirySec = sessionExpirySec,
    lastTestPassedAt = lastTestPassedAt
)

fun TopicSubscriptionEntity.toModel(): TopicSubscriptionConfig = TopicSubscriptionConfig(
    id = id,
    brokerId = brokerId,
    topicFilter = topicFilter,
    qos = qos,
    enabled = enabled,
    notifyEnabled = notifyEnabled,
    retainedAsNew = retainedAsNew
)

fun TopicSubscriptionConfig.toEntity(): TopicSubscriptionEntity = TopicSubscriptionEntity(
    id = id,
    brokerId = brokerId,
    topicFilter = topicFilter,
    qos = qos,
    enabled = enabled,
    notifyEnabled = notifyEnabled,
    retainedAsNew = retainedAsNew
)

fun MessageEntity.toModel(): InboundMessageRecord = InboundMessageRecord(
    id = id,
    brokerId = brokerId,
    topic = topicFilter,
    receivedAt = receivedAt,
    payload = payloadBlob,
    payloadPreview = payloadTextPreview,
    qos = qos,
    retained = retained,
    duplicate = duplicate,
    packetId = packetId,
    isNewActivity = isNewActivity,
    isUnread = isUnread
)

fun RetentionPolicyEntity.toModel(): RetentionPolicy = RetentionPolicy(
    id = id,
    brokerId = brokerId,
    topicFilter = topicFilter,
    maxMessages = maxMessages,
    maxAgeDays = maxAgeDays,
    trimOnInsert = trimOnInsert
)

fun RetentionPolicy.toEntity(): RetentionPolicyEntity = RetentionPolicyEntity(
    id = id,
    brokerId = brokerId,
    topicFilter = topicFilter,
    maxMessages = maxMessages,
    maxAgeDays = maxAgeDays,
    trimOnInsert = trimOnInsert
)

fun AppStateEntity.toModel(): AppState = AppState(
    activeBrokerId = activeBrokerId,
    connectionMode = runCatching { ConnectionMode.valueOf(connectionMode) }.getOrDefault(ConnectionMode.VISIBLE_ONLY),
    globalMuteUntil = globalMuteUntil,
    lastSessionStartedAt = lastSessionStartedAt,
    materialYouEnabled = materialYouEnabled
)

fun AppState.toEntity(): AppStateEntity = AppStateEntity(
    id = 0,
    activeBrokerId = activeBrokerId,
    connectionMode = connectionMode.name,
    globalMuteUntil = globalMuteUntil,
    lastSessionStartedAt = lastSessionStartedAt,
    materialYouEnabled = materialYouEnabled
)
