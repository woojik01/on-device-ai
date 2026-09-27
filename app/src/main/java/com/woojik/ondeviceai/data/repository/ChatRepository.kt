package com.woojik.ondeviceai.data.repository

import com.woojik.ondeviceai.data.local.ChatStore
import com.woojik.ondeviceai.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * 대화 도메인 로직.
 * PRD-01에서는 AI 응답 없이 사용자 입력을 저장·조회만 한다.
 * 이후 단계에서 응답 생성(로컬 LLM)을 여기에 연결한다.
 */
class ChatRepository(private val store: ChatStore) {

    fun observeMessages(): Flow<List<ChatMessage>> = store.observeMessages()

    /** 사용자 메시지를 저장하고 저장된 객체를 반환한다. */
    suspend fun sendUserMessage(text: String, now: Long, nextId: Long): ChatMessage {
        val message = ChatMessage.newUserMessage(
            text = text.trim(),
            timestampMillis = now,
            nextId = nextId,
        )
        store.appendMessage(message)
        return message
    }

    suspend fun clearConversation() = store.clearMessages()
}
