package com.woojik.ondeviceai

import app.cash.turbine.test
import com.woojik.ondeviceai.data.model.AppSettings
import com.woojik.ondeviceai.data.model.ChatMessage
import com.woojik.ondeviceai.domain.chat.ChatModel
import com.woojik.ondeviceai.domain.chat.ChatModelEvent
import com.woojik.ondeviceai.domain.chat.ConversationEngine
import com.woojik.ondeviceai.domain.chat.GenerationError
import com.woojik.ondeviceai.domain.chat.GenerationRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationEngineTest {

    private class FakeModel(private val events: List<ChatModelEvent>) : ChatModel {
        override val name = "fake"
        lateinit var lastRequest: GenerationRequest
        override fun generate(request: GenerationRequest): Flow<ChatModelEvent> = flow {
            lastRequest = request
            events.forEach { emit(it) }
        }
    }

    @Test
    fun generateReplyStreamsTokensAndCompletes() = runTest {
        val model = FakeModel(
            listOf(
                ChatModelEvent.Token("안녕"),
                ChatModelEvent.Token("하세요"),
                ChatModelEvent.Completed("안녕하세요"),
            )
        )
        val engine = ConversationEngine(model)

        engine.generateReply(emptyList(), AppSettings(), "안녕").test {
            assertEquals("안녕", (awaitItem() as ChatModelEvent.Token).text)
            assertEquals("하세요", (awaitItem() as ChatModelEvent.Token).text)
            assertEquals("안녕하세요", (awaitItem() as ChatModelEvent.Completed).fullText)
            awaitComplete()
        }
    }

    @Test
    fun contextContainsSystemCharacterAndInput() = runTest {
        val model = FakeModel(listOf(ChatModelEvent.Completed("ok")))
        val engine = ConversationEngine(model)
        val history = listOf(
            ChatMessage(0, ChatMessage.Role.USER, "이전 질문", 1),
        )

        engine.generateReply(history, AppSettings(characterName = "미소"), "새 질문").test {
            awaitItem()
            awaitComplete()
        }

        val request = model.lastRequest
        assertTrue(request.systemInstruction.isNotBlank())
        assertTrue(request.characterInfo.contains("미소"))
        assertEquals("새 질문", request.userInput)
        assertEquals(1, request.recentConversation.size)
    }

    @Test
    fun blankInputFailsImmediately() = runTest {
        val model = FakeModel(listOf(ChatModelEvent.Completed("never")))
        val engine = ConversationEngine(model)

        engine.generateReply(emptyList(), AppSettings(), "   ").test {
            assertEquals(GenerationError.GENERATION_FAILED, (awaitItem() as ChatModelEvent.Failed).error)
            awaitComplete()
        }
    }

    @Test
    fun modelFailureIsForwarded() = runTest {
        val model = FakeModel(listOf(ChatModelEvent.Failed(GenerationError.MODEL_MISSING)))
        val engine = ConversationEngine(model)

        engine.generateReply(emptyList(), AppSettings(), "hi").test {
            assertEquals(GenerationError.MODEL_MISSING, (awaitItem() as ChatModelEvent.Failed).error)
            awaitComplete()
        }
    }
}
