package com.woojik.ondeviceai.domain.chat

import com.woojik.ondeviceai.data.model.AppSettings
import com.woojik.ondeviceai.data.model.ChatMessage

/**
 * 컨텍스트 조립기.
 * 시스템 지침 / 캐릭터 정보 / 최근 대화 / 사용자 입력을 분리하여 조립한다.
 * (관련 기억·현재 상태는 PRD-04/05에서 이 지점에 추가된다.)
 */
class ContextAssembler(
    private val recentMessageLimit: Int = DEFAULT_RECENT_LIMIT,
) {

    /** 시스템 지침: 앱 전체 공통 행동 규칙. */
    fun systemInstruction(): String =
        "너는 사용자의 친근한 대화 상대다. 한국어로 자연스럽고 짧게 대답한다. " +
            "모르는 것은 모른다고 솔직하게 말한다."

    /** 캐릭터 정보: 이름 등 현재 PRD 범위의 캐릭터 정보. */
    fun characterInfo(settings: AppSettings): String =
        "캐릭터 이름은 " + settings.characterName + "이며, 다정하고 밝은 성격이다."

    /** 최근 대화: 오래된 것부터 순서대로, 최근 N개만. */
    fun recentConversation(history: List<ChatMessage>): List<ChatMessage> =
        history.takeLast(recentMessageLimit)

    /** 최종 요청 조립. */
    fun assemble(history: List<ChatMessage>, settings: AppSettings, userInput: String): GenerationRequest =
        GenerationRequest(
            systemInstruction = systemInstruction(),
            characterInfo = characterInfo(settings),
            recentConversation = recentConversation(history),
            userInput = userInput,
        )

    companion object {
        const val DEFAULT_RECENT_LIMIT = 20
    }
}
