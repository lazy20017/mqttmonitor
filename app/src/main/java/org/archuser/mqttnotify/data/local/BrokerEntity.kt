package org.archuser.mqttnotify.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "brokers")
data class BrokerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val host: String,
    val port: Int,
    val tls: Boolean,
    @ColumnInfo(name = "protocol_pref") val protocolPref: String,
    val username: String?,
    @ColumnInfo(name = "credential_ref") val credentialRef: String?,
    @ColumnInfo(name = "client_id") val clientId: String?,
    @ColumnInfo(name = "keepalive_sec") val keepaliveSec: Int,
    @ColumnInfo(name = "clean_start") val cleanStart: Boolean,
    @ColumnInfo(name = "session_expiry_sec") val sessionExpirySec: Int,
    @ColumnInfo(name = "last_test_passed_at") val lastTestPassedAt: Long?
)
