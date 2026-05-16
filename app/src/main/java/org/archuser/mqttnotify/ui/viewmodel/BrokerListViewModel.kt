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
import org.archuser.mqttnotify.domain.repo.AppStateRepository
import org.archuser.mqttnotify.domain.repo.BrokerRepository

@HiltViewModel
class BrokerListViewModel @Inject constructor(
    private val brokerRepository: BrokerRepository,
    private val appStateRepository: AppStateRepository,
    private val coordinator: ConnectionCoordinator
) : ViewModel() {

    private val _state = MutableStateFlow(BrokerListUiState())
    val state: StateFlow<BrokerListUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            brokerRepository.observeBrokers().collect { brokers ->
                _state.value = _state.value.copy(brokers = brokers)
            }
        }

        viewModelScope.launch {
            appStateRepository.observeState().collect { appState ->
                _state.value = _state.value.copy(activeBrokerId = appState.activeBrokerId)
            }
        }
    }

    fun setActiveBroker(brokerId: Long) {
        viewModelScope.launch {
            val next = if (_state.value.activeBrokerId == brokerId) null else brokerId
            coordinator.setActiveBroker(next)
        }
    }

    fun deleteBroker(id: Long) {
        viewModelScope.launch {
            brokerRepository.deleteBroker(id)
            if (_state.value.activeBrokerId == id) {
                coordinator.setActiveBroker(null)
            }
        }
    }
}

data class BrokerListUiState(
    val brokers: List<BrokerConfig> = emptyList(),
    val activeBrokerId: Long? = null
)
