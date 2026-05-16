package org.archuser.mqttnotify.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = BrokerEntity::class,
            parentColumns = ["id"],
            childColumns = ["broker_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["broker_id"]), Index(value = ["topic_filter"]), Index(value = ["received_at"])]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "broker_id") val brokerId: Long,
    @ColumnInfo(name = "topic_filter") val topicFilter: String,
    @ColumnInfo(name = "received_at") val receivedAt: Long,
    @ColumnInfo(name = "payload_blob") val payloadBlob: ByteArray,
    @ColumnInfo(name = "payload_text_preview") val payloadTextPreview: String,
    val qos: Int,
    val retained: Boolean,
    val duplicate: Boolean,
    @ColumnInfo(name = "packet_id") val packetId: Int?,
    @ColumnInfo(name = "is_new_activity") val isNewActivity: Boolean,
    @ColumnInfo(name = "is_unread") val isUnread: Boolean
)
