package org.archuser.mqttnotify.domain.repo

import kotlinx.coroutines.flow.Flow
import org.archuser.mqttnotify.domain.model.AppState
import org.archuser.mqttnotify.domain.model.ConnectionMode

interface AppStateRepository {
    fun observeState(): Flow<AppState>
    suspend fun currentState(): AppState
    suspend fun setActiveBroker(id: Long?)
    suspend fun setConnectionMode(mode: ConnectionMode)
    suspend fun setGlobalMuteUntil(until: Long?)
    suspend fun setLastSessionStartedAt(time: Long?)
    suspend fun setMaterialYouEnabled(enabled: Boolean)
}
