package com.woojik.ondeviceai

import com.woojik.ondeviceai.data.repository.ChatRepository
import com.woojik.ondeviceai.data.repository.SettingsRepository
import com.woojik.ondeviceai.domain.chat.ConversationEngine
import com.woojik.ondeviceai.domain.chat.EchoModel
import com.woojik.ondeviceai.ui.chat.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
            engine = ConversationEngine(EchoModel()),
        )

    @Test
    fun sendStoresMessageAndClearsInput() = runTest {
        val viewModel = createViewModel()

        viewModel.onInputChange("  안녕  ")
        viewModel.send()

        // 사용자 메시지 저장 + EchoModel 응답 저장
        val messages = viewModel.messages.value
        assertEquals(2, messages.size)
        assertEquals("안녕", messages[0].text)
        assertEquals("", viewModel.inputText.value)
        assertTrue(messages[1].text.contains("안녕"))
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
        // EchoModel 스트리밍은 delay가 있어 runTest 가상 시간으로 진행됨
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
