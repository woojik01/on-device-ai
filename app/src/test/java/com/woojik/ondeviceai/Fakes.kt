package com.woojik.ondeviceai

import com.woojik.ondeviceai.data.local.AppPreferences
import com.woojik.ondeviceai.data.local.ChatStore
import com.woojik.ondeviceai.data.model.AppSettings
import com.woojik.ondeviceai.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 로컬 저장 계층 추상화의 인메모리 가짜 구현 (테스트용) */
class FakeChatStore(initial: List<ChatMessage> = emptyList()) : ChatStore {
    private val state = MutableStateFlow(initial)
    override fun observeMessages(): Flow<List<ChatMessage>> = state.asStateFlow()
    override suspend fun appendMessage(message: ChatMessage) {
        state.value = state.value + message
    }
    override suspend fun clearMessages() {
        state.value = emptyList()
    }
}

class FakeAppPreferences : AppPreferences {
    private val state = MutableStateFlow(AppSettings())
    override fun observeSettings(): Flow<AppSettings> = state.asStateFlow()
    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        state.value = transform(state.value)
    }
}
