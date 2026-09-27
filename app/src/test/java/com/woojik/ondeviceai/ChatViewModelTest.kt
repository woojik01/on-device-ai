package com.woojik.ondeviceai

import com.woojik.ondeviceai.data.repository.ChatRepository
import com.woojik.ondeviceai.data.repository.SettingsRepository
import com.woojik.ondeviceai.domain.chat.ChatModel
import com.woojik.ondeviceai.domain.chat.ChatModelEvent
import com.woojik.ondeviceai.domain.chat.ConversationEngine
import com.woojik.ondeviceai.domain.chat.GenerationRequest
import com.woojik.ondeviceai.ui.chat.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var store: FakeChatStore
    private lateinit var repository: ChatRepository

    /**
     * 즉시 완료되는 테스트용 모델.
     * EchoModel은 청크마다 delay(50)로 스트리밍을 시뮬레이션하므로
     * 동기적 완료를 검사하는 이 테스트에서는 delay 없이
     * 토큰 → 완료를 즉시 방출하는 모델을 사용한다.
     * (스트리밍 지연/취소 동작은 ChatViewModelStreamingTest가 담당)
     */
    private class ImmediateModel : ChatModel {
        override val name = "immediate-test"
        override fun generate(request: GenerationRequest): Flow<ChatModelEvent> = flow {
            emit(ChatModelEvent.Token("응"))
            emit(ChatModelEvent.Token("답"))
            emit(ChatModelEvent.Completed("응답"))
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        store = FakeChatStore()
        repository = ChatRepository(store)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** PRD-02 생성자 시그니처(repository, settingsRepository, engine)에 맞춘 ViewModel 생성. */
    private fun createViewModel(): ChatViewModel =
        ChatViewModel(
            repository = repository,
            settingsRepository = SettingsRepository(FakeAppPreferences()),
            engine = ConversationEngine(ImmediateModel()),
        )

    @Test
    fun sendStoresMessageAndClearsInput() = runTest {
        val viewModel = createViewModel()

        viewModel.onInputChange("  안녕  ")
        viewModel.send()

        // 사용자 메시지 + 즉시 완료 모델의 응답 저장, 입력창 비움
        val messages = viewModel.messages.value
        assertEquals(2, messages.size)
        assertEquals("안녕", messages[0].text)
        assertEquals("응답", messages[1].text)
        assertEquals("", viewModel.inputText.value)
    }

    @Test
    fun sendWithBlankInputDoesNothing() = runTest {
        val viewModel = createViewModel()

        viewModel.onInputChange("   ")
        viewModel.send()

        assertTrue(viewModel.messages.value.isEmpty())
    }

    @Test
    fun idsAreAssignedSequentially() = runTest {
        val viewModel = createViewModel()

        viewModel.onInputChange("one")
        viewModel.send()
        // 즉시 완료 모델이므로 첫 생성이 끝난 뒤 두 번째 전송 가능
        viewModel.onInputChange("two")
        viewModel.send()

        val texts = viewModel.messages.value.map { it.text }
        assertTrue(texts.contains("one"))
        assertTrue(texts.contains("two"))
        val ids = viewModel.messages.value.map { it.id }
        assertEquals(ids, ids.sorted())
    }

    @Test
    fun clearConversationRemovesAllMessages() = runTest {
        val viewModel = createViewModel()
        viewModel.onInputChange("bye")
        viewModel.send()

        viewModel.clearConversation()

        assertTrue(viewModel.messages.value.isEmpty())
    }
}
