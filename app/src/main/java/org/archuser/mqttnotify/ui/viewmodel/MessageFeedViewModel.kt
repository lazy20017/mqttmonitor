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
import org.archuser.mqttnotify.domain.model.InboundMessageRecord
import org.archuser.mqttnotify.domain.repo.MessageRepository

@HiltViewModel
class MessageFeedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val brokerId: Long = savedStateHandle.get<String>("brokerId")?.toLongOrNull() ?: 0L

    private val _messages = MutableStateFlow<List<InboundMessageRecord>>(emptyList())
    val messages: StateFlow<List<InboundMessageRecord>> = _messages.asStateFlow()

    init {
        viewModelScope.launch {
            messageRepository.observeMessagesForBroker(brokerId).collect {
                _messages.value = it
            }
        }
    }

    fun markMessageRead(messageId: Long) {
        viewModelScope.launch {
            messageRepository.markMessageRead(messageId)
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            messageRepository.resetUnreadForBroker(brokerId)
        }
    }

    fun markMessageUnread(messageId: Long) {
        viewModelScope.launch {
            messageRepository.markMessageUnread(messageId)
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
        }
    }
}
