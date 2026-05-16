package org.archuser.mqttnotify.connection

import kotlinx.coroutines.flow.StateFlow
import org.archuser.mqttnotify.domain.model.ConnectionMode
import org.archuser.mqttnotify.domain.model.ConnectionSnapshot

interface ConnectionCoordinator {
    val snapshot: StateFlow<ConnectionSnapshot>
    suspend fun setMode(mode: ConnectionMode)
    suspend fun setActiveBroker(brokerId: Long?)
    suspend fun onUiVisibilityChanged(visible: Boolean)
    suspend fun startPersistent()
    suspend fun stopPersistent()
}
