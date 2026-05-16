package org.archuser.mqttnotify.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.archuser.mqttnotify.core.TimeProvider
import org.archuser.mqttnotify.domain.repo.AppStateRepository

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            appStateRepository.observeState().collect { appState ->
                val muted = appState.globalMuteUntil?.let { it > timeProvider.nowMillis() } ?: false
                _state.value = SettingsUiState(
                    muted = muted,
                    muteUntil = appState.globalMuteUntil,
                    materialYouEnabled = appState.materialYouEnabled
                )
            }
        }
    }

    fun clearMute() {
        viewModelScope.launch {
            appStateRepository.setGlobalMuteUntil(null)
        }
    }

    fun muteFor(minutes: Int) {
        viewModelScope.launch {
            val until = timeProvider.nowMillis() + minutes.coerceAtLeast(1) * 60_000L
            appStateRepository.setGlobalMuteUntil(until)
        }
    }

    fun setMaterialYouEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appStateRepository.setMaterialYouEnabled(enabled)
        }
    }
}

data class SettingsUiState(
    val muted: Boolean = false,
    val muteUntil: Long? = null,
    val materialYouEnabled: Boolean = true
)
