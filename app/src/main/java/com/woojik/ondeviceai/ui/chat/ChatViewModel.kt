package com.woojik.ondeviceai.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.woojik.ondeviceai.ServiceLocator
import com.woojik.ondeviceai.data.model.AppSettings
import com.woojik.ondeviceai.data.model.ChatMessage
import com.woojik.ondeviceai.data.repository.ChatRepository
import com.woojik.ondeviceai.data.repository.SettingsRepository
import com.woojik.ondeviceai.domain.chat.ChatModelEvent
import com.woojik.ondeviceai.domain.chat.ConversationEngine
import com.woojik.ondeviceai.domain.chat.GenerationError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 채팅 화면 ViewModel.
 * PRD-02: 스트리밍 응답, 생성 중 상태, 생성 취소, 중복 전송 방지, 오류 표시.
 */
class ChatViewModel(
    private val repository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val engine: ConversationEngine,
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    /** 생성 중 스트리밍 텍스트 (부분 응답). */
    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    /** 복구 가능한 오류 (null이면 없음). */
    private val _error = MutableStateFlow<GenerationError?>(null)
    val error: StateFlow<GenerationError?> = _error.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = repository.observeMessages()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val settings: StateFlow<AppSettings> = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private var generationJob: Job? = null

    fun onInputChange(text: String) {
        _inputText.value = text
    }

    /** 전송: 빈 입력 차단, 중복 전송 방지, 저장 후 스트리밍 생성. */
    fun send() {
        val text = _inputText.value
        if (text.isBlank()) return
        if (_isGenerating.value) return

        _error.value = null
        _isGenerating.value = true
        _streamingText.value = ""

        viewModelScope.launch {
            val nextId = (messages.value.maxOfOrNull { it.id } ?: -1L) + 1
            repository.sendUserMessage(
                text = text,
                now = System.currentTimeMillis(),
                nextId = nextId,
            )
            _inputText.value = ""

            val historyBeforeReply = messages.value
            val userText = text.trim()
            generationJob = launch {
                engine.generateReply(
                    history = historyBeforeReply,
                    settings = settings.value,
                    userInput = userText,
                ).collect { event ->
                    when (event) {
                        is ChatModelEvent.Token -> _streamingText.value += event.text
                        is ChatModelEvent.Completed -> {
                            val replyId = (messages.value.maxOfOrNull { it.id } ?: -1L) + 1
                            repository.appendCharacterMessage(
                                text = event.fullText,
                                now = System.currentTimeMillis(),
                                nextId = replyId,
                            )
                            _streamingText.value = ""
                        }
                        is ChatModelEvent.Failed -> {
                            _error.value = event.error
                            _streamingText.value = ""
                        }
                    }
                }
            }
            generationJob?.join()
            generationJob = null
            _isGenerating.value = false
        }
    }

    /** 생성 취소: 진행 중 스트리밍을 버리고 상태를 복구한다. */
    fun cancelGeneration() {
        generationJob?.cancel()
        generationJob = null
        _isGenerating.value = false
        _streamingText.value = ""
        _error.value = GenerationError.CANCELLED
    }

    fun dismissError() {
        _error.value = null
    }

    fun clearConversation() {
        if (_isGenerating.value) cancelGeneration()
        viewModelScope.launch { repository.clearConversation() }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val locator: ServiceLocator) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChatViewModel(
                repository = locator.chatRepository,
                settingsRepository = locator.settingsRepository,
                engine = locator.conversationEngine,
            ) as T
    }

    companion object {
        fun factory(locator: ServiceLocator) = Factory(locator)
    }
}
