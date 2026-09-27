package com.woojik.ondeviceai

import com.woojik.ondeviceai.data.model.ChatMessage
import com.woojik.ondeviceai.data.repository.ChatRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRepositoryTest {

    @Test
    fun sendUserMessageTrimsTextAndStoresIt() = runTest {
        val store = FakeChatStore()
        val repository = ChatRepository(store)

        val saved = repository.sendUserMessage(
            text = "  안녕하세요  ",
            now = 1000L,
            nextId = 0L,
        )

        assertEquals("안녕하세요", saved.text)
        assertEquals(ChatMessage.Role.USER, saved.role)
        assertEquals(listOf(saved), store.observeMessages().first())
    }

    @Test
    fun messagesAreAppendedInOrder() = runTest {
        val store = FakeChatStore()
        val repository = ChatRepository(store)

        repository.sendUserMessage("first", now = 1L, nextId = 0)
        repository.sendUserMessage("second", now = 2L, nextId = 1)

        val messages = store.observeMessages().first()
        assertEquals(listOf("first", "second"), messages.map { it.text })
        assertTrue(messages.zipWithNext().all { (a, b) -> a.id < b.id })
    }

    @Test
    fun clearConversationRemovesAllMessages() = runTest {
        val store = FakeChatStore()
        val repository = ChatRepository(store)

        repository.sendUserMessage("hello", now = 1L, nextId = 0)
        repository.clearConversation()

        assertTrue(store.observeMessages().first().isEmpty())
    }
}
