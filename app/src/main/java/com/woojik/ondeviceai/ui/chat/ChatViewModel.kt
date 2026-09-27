package com.woojik.ondeviceai.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.woojik.ondeviceai.ServiceLocator
import com.woojik.ondeviceai.data.model.ChatMessage
import com.woojik.ondeviceai.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 채팅 화면 UI 상태. */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
) {
    val canSend: Boolean get() = inputText.isNotBlank() && !isSending
}

/**
 * 채팅 화면 ViewModel.
 * PRD-01에서는 AI 응답 없이 입력 처리·저장만 담당한다.
 */
class ChatViewModel(
    private val repository: ChatRepository,
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = repository.observeMessages()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun onInputChange(text: String) {
        _inputText.value = text
    }

    /** 전송 버튼 동작: 입력을 저장하고 입력창을 비운다. (PRD-01 테스트 4~5) */
    fun send(onDone: () -> Unit = {}) {
        val text = _inputText.value
        if (text.isBlank()) return
        viewModelScope.launch {
            val nextId = (messages.value.maxOfOrNull { it.id } ?: -1L) + 1
            repository.sendUserMessage(
                text = text,
                now = System.currentTimeMillis(),
                nextId = nextId,
            )
            _inputText.value = ""
            onDone()
        }
    }

    fun clearConversation() {
        viewModelScope.launch { repository.clearConversation() }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val locator: ServiceLocator) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChatViewModel(locator.chatRepository) as T
    }

    companion object {
        fun factory(locator: ServiceLocator) = Factory(locator)
    }
}
