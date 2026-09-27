package com.woojik.ondeviceai.data.local

import com.woojik.ondeviceai.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * 대화 저장 계층 추상화.
 * PRD-01에서는 DataStore(JSON) 구현을 사용하며, 이후 단계에서 SQLite 구현으로 교체 가능하다.
 */
interface ChatStore {
    fun observeMessages(): Flow<List<ChatMessage>>
    suspend fun appendMessage(message: ChatMessage)
    suspend fun clearMessages()
}
