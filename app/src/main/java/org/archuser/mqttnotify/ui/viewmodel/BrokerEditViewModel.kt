package org.archuser.mqttnotify.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.archuser.mqttnotify.core.TimeProvider
import org.archuser.mqttnotify.data.mqtt.BrokerConnectionTester
import org.archuser.mqttnotify.data.security.CredentialsStore
import org.archuser.mqttnotify.domain.model.BrokerConfig
import org.archuser.mqttnotify.domain.model.BrokerCredentialsRef
import org.archuser.mqttnotify.domain.model.ProtocolVersion
import org.archuser.mqttnotify.domain.repo.BrokerRepository

@HiltViewModel
class BrokerEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val brokerRepository: BrokerRepository,
    private val connectionTester: BrokerConnectionTester,
    private val credentialsStore: CredentialsStore,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val brokerId: Long = savedStateHandle.get<String>("brokerId")?.toLongOrNull() ?: 0L

    private val _state = MutableStateFlow(BrokerEditUiState())
    val state: StateFlow<BrokerEditUiState> = _state.asStateFlow()

    init {
        if (brokerId != 0L) {
            viewModelScope.launch {
                brokerRepository.getBroker(brokerId)?.let { broker ->
                    _state.value = _state.value.copy(
                        id = broker.id,
                        label = broker.label,
                        host = broker.host,
                        port = broker.port.toString(),
                        tls = broker.tls,
                        protocol = broker.protocolVersion,
                        username = broker.username.orEmpty(),
                        clientId = broker.clientId.orEmpty(),
                        keepaliveSec = broker.keepaliveSec.toString(),
                        cleanStart = broker.cleanStart,
                        sessionExpirySec = broker.sessionExpirySec.toString(),
                        testedAt = broker.lastTestPassedAt,
                        status = null
                    )
                }
            }
        }
    }

    fun update(transform: (BrokerEditUiState) -> BrokerEditUiState) {
        _state.value = transform(_state.value)
    }

    fun testConnection() {
        viewModelScope.launch {
            val draft = _state.value.toBrokerConfig()
            if (draft == null) {
                _state.value = _state.value.copy(status = "Invalid broker fields")
                return@launch
            }

            _state.value = _state.value.copy(isTesting = true, status = null)
            val result = connectionTester.test(draft, _state.value.password.ifBlank { null })
            _state.value = if (result.isSuccess) {
                _state.value.copy(
                    isTesting = false,
                    testedAt = timeProvider.nowMillis(),
                    status = "Connection test passed"
                )
            } else {
                _state.value.copy(
                    isTesting = false,
                    status = "Connection test failed: ${result.exceptionOrNull()?.message ?: "unknown"}"
                )
            }
        }
    }

    fun save(onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val draft = _state.value.toBrokerConfig() ?: run {
                _state.value = _state.value.copy(status = "Invalid broker fields")
                return@launch
            }

            val credentialAlias = if (_state.value.password.isNotBlank()) {
                "broker_pwd_${UUID.randomUUID()}"
            } else {
                draft.credentialsRef?.alias
            }

            val toSave = draft.copy(
                credentialsRef = credentialAlias?.let(::BrokerCredentialsRef),
                lastTestPassedAt = _state.value.testedAt
            )

            val saved = brokerRepository.saveBroker(toSave)
            if (saved.isFailure) {
                _state.value = _state.value.copy(status = saved.exceptionOrNull()?.message ?: "Save failed")
                return@launch
            }

            val id = saved.getOrThrow()
            if (_state.value.password.isNotBlank() && credentialAlias != null) {
                credentialsStore.savePassword(credentialAlias, _state.value.password)
            }
            _state.value = _state.value.copy(status = "Broker saved")
            onSaved(id)
        }
    }
}

data class BrokerEditUiState(
    val id: Long = 0,
    val label: String = "",
    val host: String = "",
    val port: String = "8883",
    val tls: Boolean = true,
    val protocol: ProtocolVersion = ProtocolVersion.AUTO,
    val username: String = "",
    val password: String = "",
    val clientId: String = "",
    val keepaliveSec: String = "60",
    val cleanStart: Boolean = true,
    val sessionExpirySec: String = "0",
    val testedAt: Long? = null,
    val isTesting: Boolean = false,
    val status: String? = null
) {
    fun toBrokerConfig(): BrokerConfig? {
        val parsedPort = port.toIntOrNull() ?: return null
        val parsedKeepalive = keepaliveSec.toIntOrNull() ?: return null
        val parsedSessionExpiry = sessionExpirySec.toIntOrNull() ?: return null
        if (label.isBlank() || host.isBlank()) return null

        return BrokerConfig(
            id = id,
            label = label.trim(),
            host = host.trim(),
            port = parsedPort,
            tls = tls,
            protocolVersion = protocol,
            username = username.trim().ifBlank { null },
            credentialsRef = null,
            clientId = clientId.trim().ifBlank { null },
            keepaliveSec = parsedKeepalive,
            cleanStart = cleanStart,
            sessionExpirySec = parsedSessionExpiry,
            lastTestPassedAt = testedAt
        )
    }
}
