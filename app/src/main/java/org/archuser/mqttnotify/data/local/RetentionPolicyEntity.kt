package org.archuser.mqttnotify.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "retention_policies")
data class RetentionPolicyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "broker_id") val brokerId: Long?,
    @ColumnInfo(name = "topic_filter") val topicFilter: String?,
    @ColumnInfo(name = "max_messages") val maxMessages: Int,
    @ColumnInfo(name = "max_age_days") val maxAgeDays: Int,
    @ColumnInfo(name = "trim_on_insert") val trimOnInsert: Boolean
)
