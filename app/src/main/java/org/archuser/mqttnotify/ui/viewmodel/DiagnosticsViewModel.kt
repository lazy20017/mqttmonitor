package org.archuser.mqttnotify.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.archuser.mqttnotify.connection.ConnectionCoordinator
import org.archuser.mqttnotify.domain.model.ConnectionSnapshot
import org.archuser.mqttnotify.domain.model.ConnectionStatus
import org.archuser.mqttnotify.domain.repo.DiagnosticsRepository

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    diagnosticsRepository: DiagnosticsRepository,
    connectionCoordinator: ConnectionCoordinator
) : ViewModel() {

    private val _events = MutableStateFlow<List<String>>(emptyList())
    val events: StateFlow<List<String>> = _events.asStateFlow()

    private val _snapshot = MutableStateFlow(
        ConnectionSnapshot(
            status = ConnectionStatus.DISCONNECTED,
            brokerLabel = null,
            connectedSince = null,
            messageCount = 0,
            lastError = null
        )
    )
    val snapshot: StateFlow<ConnectionSnapshot> = _snapshot.asStateFlow()

    init {
        viewModelScope.launch {
            diagnosticsRepository.observeEvents().collect {
                _events.value = it
            }
        }

        viewModelScope.launch {
            connectionCoordinator.snapshot.collect {
                _snapshot.value = it
            }
        }
    }
}
