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
import org.archuser.mqttnotify.domain.model.BrokerConfig
import org.archuser.mqttnotify.domain.model.ConnectionMode
import org.archuser.mqttnotify.domain.model.ConnectionSnapshot
import org.archuser.mqttnotify.domain.model.ConnectionStatus
import org.archuser.mqttnotify.domain.repo.AppStateRepository
import org.archuser.mqttnotify.domain.repo.BrokerRepository
import org.archuser.mqttnotify.domain.repo.MessageRepository

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val brokerRepository: BrokerRepository,
    private val appStateRepository: AppStateRepository,
    private val messageRepository: MessageRepository,
    private val coordinator: ConnectionCoordinator
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            brokerRepository.observeBrokers().collect { brokers ->
                _state.value = _state.value.copy(brokers = brokers)
            }
        }

        viewModelScope.launch {
            appStateRepository.observeState().collect { appState ->
                _state.value = _state.value.copy(
                    mode = appState.connectionMode,
                    activeBrokerId = appState.activeBrokerId
                )

                val unread = appState.activeBrokerId?.let { messageRepository.unreadCountForBroker(it) } ?: 0
                _state.value = _state.value.copy(unreadCount = unread)
            }
        }

        viewModelScope.launch {
            coordinator.snapshot.collect { snap ->
                _state.value = _state.value.copy(snapshot = snap)
                val brokerId = _state.value.activeBrokerId
                val unread = brokerId?.let { messageRepository.unreadCountForBroker(it) } ?: 0
                _state.value = _state.value.copy(unreadCount = unread)
            }
        }
    }

    fun selectBroker(brokerId: Long?) {
        viewModelScope.launch {
            val next = if (brokerId != null && _state.value.activeBrokerId == brokerId) null else brokerId
            coordinator.setActiveBroker(next)
        }
    }

    fun setMode(mode: ConnectionMode) {
        viewModelScope.launch {
            coordinator.setMode(mode)
        }
    }
}

data class DashboardUiState(
    val brokers: List<BrokerConfig> = emptyList(),
    val activeBrokerId: Long? = null,
    val mode: ConnectionMode = ConnectionMode.VISIBLE_ONLY,
    val snapshot: ConnectionSnapshot = ConnectionSnapshot(
        status = ConnectionStatus.DISCONNECTED,
        brokerLabel = null,
        connectedSince = null,
        messageCount = 0,
        lastError = null
    ),
    val unreadCount: Int = 0
)
