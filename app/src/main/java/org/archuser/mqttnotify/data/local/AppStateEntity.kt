package org.archuser.mqttnotify.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val id: Int = 0,
    @ColumnInfo(name = "active_broker_id") val activeBrokerId: Long?,
    @ColumnInfo(name = "connection_mode") val connectionMode: String,
    @ColumnInfo(name = "global_mute_until") val globalMuteUntil: Long?,
    @ColumnInfo(name = "last_session_started_at") val lastSessionStartedAt: Long?,
    @ColumnInfo(name = "material_you_enabled", defaultValue = "1") val materialYouEnabled: Boolean
)
