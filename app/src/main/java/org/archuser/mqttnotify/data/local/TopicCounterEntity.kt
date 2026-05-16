package org.archuser.mqttnotify.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "topic_counters", primaryKeys = ["broker_id", "topic_filter"])
data class TopicCounterEntity(
    @ColumnInfo(name = "broker_id") val brokerId: Long,
    @ColumnInfo(name = "topic_filter") val topicFilter: String,
    @ColumnInfo(name = "unread_count") val unreadCount: Int,
    @ColumnInfo(name = "total_count") val totalCount: Int
)
