package com.woojik.ondeviceai

import com.woojik.ondeviceai.data.repository.ChatRepository
import com.woojik.ondeviceai.data.repository.SettingsRepository
import com.woojik.ondeviceai.domain.chat.ChatModel
import com.woojik.ondeviceai.domain.chat.ChatModelEvent
import com.woojik.ondeviceai.domain.chat.ConversationEngine
import com.woojik.ondeviceai.domain.chat.GenerationError
import com.woojik.ondeviceai.domain.chat.GenerationRequest
import com.woojik.ondeviceai.ui.chat.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelStreamingTest {

    private val dispatcher = UnconfinedTestDispatcher()

    /** 느린 토큰 방출 모델 — 취소 시뮬레이션용 */
    private class SlowModel : ChatModel {
        override val name = "slow"
        var completed = false
        override fun generate(request: GenerationRequest): Flow<ChatModelEvent> = flow {
            emit(ChatModelEvent.Token("부"))
            delay(10000)
            completed = true
            emit(ChatModelEvent.Completed("불가"))
        }
    }

    /** 즉시 실패 모델 */
    private class FailingModel : ChatModel {
        override val name = "failing"
        override fun generate(request: GenerationRequest): Flow<ChatModelEvent> = flow {
            emit(ChatModelEvent.Failed(GenerationError.GENERATION_FAILED))
        }
    }

    private lateinit var store: FakeChatStore
    private lateinit var preferences: FakeAppPreferences

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        store = FakeChatStore()
        preferences = FakeAppPreferences()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelWith(model: ChatModel): ChatViewModel =
        ChatViewModel(
            repository = ChatRepository(store),
            settingsRepository = SettingsRepository(preferences),
            engine = ConversationEngine(model),
        )

    @Test
    fun sendStreamsAndStoresCompletedReply() = runTest {
        val model = object : ChatModel {
            override val name = "echo"
            override fun generate(request: GenerationRequest): Flow<ChatModelEvent> = flow {
                emit(ChatModelEvent.Token("안녕"))
                emit(ChatModelEvent.Completed("안녕!"))
            }
        }
        val viewModel = viewModelWith(model)

        viewModel.onInputChange("안녕")
        viewModel.send()

        // 사용자 메시지 + 캐릭터 응답 저장 확인
        assertEquals(2, viewModel.messages.value.size)
        assertEquals("안녕!", viewModel.messages.value[1].text)
        assertEquals("", viewModel.inputText.value)
        assertFalse(viewModel.isGenerating.value)
    }

    @Test
    fun blankInputIsBlocked() = runTest {
        val viewModel = viewModelWith(SlowModel())
        viewModel.onInputChange("   ")
        viewModel.send()
        assertTrue(viewModel.messages.value.isEmpty())
        assertFalse(viewModel.isGenerating.value)
    }

    @Test
    fun duplicateSendIsBlockedWhileGenerating() = runTest {
        val slow = SlowModel()
        val viewModel = viewModelWith(slow)

        viewModel.onInputChange("하나")
        viewModel.send()
        assertTrue(viewModel.isGenerating.value)

        // 생성 중 두 번째 전송 시도
        viewModel.onInputChange("둘")
        viewModel.send()

        // 두 번째 메시지는 저장되지 않는다
        assertEquals(1, viewModel.messages.value.size)
        assertEquals("하나", viewModel.messages.value[0].text)

        viewModel.cancelGeneration()
    }

    @Test
    fun cancelStopsGenerationAndShowsCancelledError() = runTest {
        val slow = SlowModel()
        val viewModel = viewModelWith(slow)

        viewModel.onInputChange("취소 테스트")
        viewModel.send()
        assertTrue(viewModel.isGenerating.value)

        viewModel.cancelGeneration()

        assertFalse(viewModel.isGenerating.value)
        assertEquals("", viewModel.streamingText.value)
        assertEquals(GenerationError.CANCELLED, viewModel.error.value)
        assertFalse(slow.completed) // flow가 취소되어 Completed 미방출
    }

    @Test
    fun modelFailureShowsErrorAndKeepsUserMessage() = runTest {
        val viewModel = viewModelWith(FailingModel())

        viewModel.onInputChange("오류 테스트")
        viewModel.send()

        assertEquals(1, viewModel.messages.value.size) // 사용자 메시지는 유지
        assertEquals(GenerationError.GENERATION_FAILED, viewModel.error.value)
        assertFalse(viewModel.isGenerating.value)
        assertEquals("", viewModel.streamingText.value)

        viewModel.dismissError()
        assertEquals(null, viewModel.error.value)
    }
}
