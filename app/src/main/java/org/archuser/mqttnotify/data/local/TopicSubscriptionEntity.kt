package org.archuser.mqttnotify.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "topic_subscriptions",
    foreignKeys = [
        ForeignKey(
            entity = BrokerEntity::class,
            parentColumns = ["id"],
            childColumns = ["broker_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["broker_id"])]
)
data class TopicSubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "broker_id") val brokerId: Long,
    @ColumnInfo(name = "topic_filter") val topicFilter: String,
    val qos: Int,
    val enabled: Boolean,
    @ColumnInfo(name = "notify_enabled") val notifyEnabled: Boolean,
    @ColumnInfo(name = "retained_as_new") val retainedAsNew: Boolean
)
