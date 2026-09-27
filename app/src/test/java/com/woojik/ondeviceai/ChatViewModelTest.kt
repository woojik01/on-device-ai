package com.woojik.ondeviceai

import com.woojik.ondeviceai.data.repository.ChatRepository
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

    @Test
    fun sendStoresMessageAndClearsInput() = runTest {
        val viewModel = ChatViewModel(repository)

        viewModel.onInputChange("  안녕  ")
        viewModel.send()

        val messages = viewModel.messages.value
        assertEquals(1, messages.size)
        assertEquals("안녕", messages.first().text)
        assertEquals("", viewModel.inputText.value)
    }

    @Test
    fun sendWithBlankInputDoesNothing() = runTest {
        val viewModel = ChatViewModel(repository)

        viewModel.onInputChange("   ")
        viewModel.send()

        assertTrue(viewModel.messages.value.isEmpty())
    }

    @Test
    fun idsAreAssignedSequentially() = runTest {
        val viewModel = ChatViewModel(repository)

        viewModel.onInputChange("one")
        viewModel.send()
        viewModel.onInputChange("two")
        viewModel.send()

        val ids = viewModel.messages.value.map { it.id }
        assertEquals(listOf(0L, 1L), ids)
    }

    @Test
    fun clearConversationRemovesAllMessages() = runTest {
        val viewModel = ChatViewModel(repository)
        viewModel.onInputChange("bye")
        viewModel.send()

        viewModel.clearConversation()

        assertTrue(viewModel.messages.value.isEmpty())
    }
}
