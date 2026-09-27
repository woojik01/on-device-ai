package com.woojik.ondeviceai.data.repository

import com.woojik.ondeviceai.data.local.ChatStore
import com.woojik.ondeviceai.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * 대화 도메인 로직.
 * PRD-02부터 사용자 입력과 AI 응답을 모두 저장한다.
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

    /** AI(캐릭터) 응답 메시지를 저장한다. */
    suspend fun appendCharacterMessage(text: String, now: Long, nextId: Long): ChatMessage {
        val message = ChatMessage(
            id = nextId,
            role = ChatMessage.Role.CHARACTER,
            text = text,
            timestampMillis = now,
        )
        store.appendMessage(message)
        return message
    }

    suspend fun clearConversation() = store.clearMessages()
}
