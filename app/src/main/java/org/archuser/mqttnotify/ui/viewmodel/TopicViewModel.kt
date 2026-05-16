package org.archuser.mqttnotify.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.archuser.mqttnotify.domain.model.TopicSubscriptionConfig
import org.archuser.mqttnotify.domain.repo.TopicRepository

@HiltViewModel
class TopicViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val topicRepository: TopicRepository
) : ViewModel() {

    private val brokerId: Long = savedStateHandle.get<String>("brokerId")?.toLongOrNull() ?: 0L

    private val _topics = MutableStateFlow<List<TopicSubscriptionConfig>>(emptyList())
    val topics: StateFlow<List<TopicSubscriptionConfig>> = _topics.asStateFlow()

    init {
        viewModelScope.launch {
            topicRepository.observeTopicsForBroker(brokerId).collect {
                _topics.value = it
            }
        }
    }

    fun addTopic(filter: String, qos: Int, notifyEnabled: Boolean, retainedAsNew: Boolean) {
        if (filter.isBlank()) return
        viewModelScope.launch {
            topicRepository.upsertTopic(
                TopicSubscriptionConfig(
                    id = 0,
                    brokerId = brokerId,
                    topicFilter = filter.trim(),
                    qos = qos.coerceIn(0, 2),
                    enabled = true,
                    notifyEnabled = notifyEnabled,
                    retainedAsNew = retainedAsNew
                )
            )
        }
    }

    fun toggleEnabled(item: TopicSubscriptionConfig, enabled: Boolean) {
        viewModelScope.launch {
            topicRepository.upsertTopic(item.copy(enabled = enabled))
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            topicRepository.deleteTopic(id)
        }
    }
}
