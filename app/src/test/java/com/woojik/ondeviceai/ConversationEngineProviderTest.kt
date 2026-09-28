package com.woojik.ondeviceai

import app.cash.turbine.test
import com.woojik.ondeviceai.data.model.AppSettings
import com.woojik.ondeviceai.domain.chat.ChatModel
import com.woojik.ondeviceai.domain.chat.ChatModelEvent
import com.woojik.ondeviceai.domain.chat.ConversationEngine
import com.woojik.ondeviceai.domain.chat.GenerationRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationEngineProviderTest {

    private class CompletedModel(private val output: String) : ChatModel {
        override val name = output
        override fun generate(request: GenerationRequest): Flow<ChatModelEvent> = flow {
            emit(ChatModelEvent.Completed(output))
        }
    }

    @Test
    fun fixedModelConstructorBehavesLikeProvider() = runTest {
        val engine = ConversationEngine(CompletedModel("fixed"))
        engine.generateReply(emptyList(), AppSettings(), "hi").test {
            assertEquals("fixed", (awaitItem() as ChatModelEvent.Completed).fullText)
            awaitComplete()
        }
    }

    @Test
    fun swappedModelIsUsedOnNextGeneration() = runTest {
        var model: ChatModel = CompletedModel("first")
        val engine = ConversationEngine(modelProvider = { model })

        engine.generateReply(emptyList(), AppSettings(), "hi").test {
            assertEquals("first", (awaitItem() as ChatModelEvent.Completed).fullText)
            awaitComplete()
        }

        model = CompletedModel("second")

        engine.generateReply(emptyList(), AppSettings(), "hi").test {
            assertEquals("second", (awaitItem() as ChatModelEvent.Completed).fullText)
            awaitComplete()
        }
    }
}
