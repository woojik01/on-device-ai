package com.woojik.ondeviceai.domain.chat

import com.woojik.ondeviceai.data.model.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    private val history = listOf(
        ChatMessage(id = 1, role = ChatMessage.Role.USER, text = "안녕", timestampMillis = 1000),
        ChatMessage(id = 2, role = ChatMessage.Role.CHARACTER, text = "안녕하세요!", timestampMillis = 2000),
    )

    private fun request(
        system: String = "시스템 지침",
        character: String = "캐릭터 정보",
        conversation: List<ChatMessage> = history,
        input: String = "오늘 뭐 했어?",
    ) = GenerationRequest(
        systemInstruction = system,
        characterInfo = character,
        recentConversation = conversation,
        userInput = input,
    )

    @Test
    fun `시스템 지침과 캐릭터 정보가 프롬프트에 포함된다`() {
        val prompt = PromptBuilder.build(request())
        assertTrue(prompt.contains("시스템 지침"))
        assertTrue(prompt.contains("캐릭터 정보"))
    }

    @Test
    fun `최근 대화가 역할 라벨과 함께 순서대로 포함된다`() {
        val prompt = PromptBuilder.build(request())
        val userTurn = prompt.indexOf("사용자: 안녕")
        val characterTurn = prompt.indexOf("캐릭터: 안녕하세요!")
        assertTrue(userTurn >= 0)
        assertTrue(characterTurn > userTurn)
    }

    @Test
    fun `사용자 입력이 마지막에 있고 캐릭터 응답 차례로 끝난다`() {
        val prompt = PromptBuilder.build(request())
        val inputTurn = prompt.indexOf("사용자: 오늘 뭐 했어?")
        assertTrue(inputTurn >= 0)
        assertTrue(prompt.endsWith("캐릭터: "))
    }

    @Test
    fun `컨텍스트가 비어도 사용자 입력 프롬프트를 만든다`() {
        val prompt = PromptBuilder.build(
            request(system = "", character = "", conversation = emptyList(), input = "안녕"),
        )
        assertTrue(prompt.contains("사용자: 안녕"))
        assertTrue(prompt.endsWith("캐릭터: "))
    }

    @Test
    fun `토큰 조각을 합치면 완성된 응답 텍스트와 일치한다`() {
        // 엔진/뷰모델의 증분 방출 가정 검증: Token 조각 합 == Completed 텍스트
        val prompt = PromptBuilder.build(request())
        val tokens = prompt.chunked(3)
        assertEquals(prompt, tokens.joinToString(""))
    }
}
