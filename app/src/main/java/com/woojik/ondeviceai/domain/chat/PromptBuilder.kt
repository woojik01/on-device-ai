package com.woojik.ondeviceai.domain.chat

import com.woojik.ondeviceai.data.model.ChatMessage

/**
 * 순수 프롬프트 빌더. GenerationRequest를 로컬 모델 입력 문자열로 변환한다.
 * 순수 함수라서 JVM 단위 테스트가 가능하다.
 */
object PromptBuilder {

    fun build(request: GenerationRequest): String = buildString {
        if (request.systemInstruction.isNotBlank()) {
            append(request.systemInstruction)
            append(LINE_BREAK)
        }
        if (request.characterInfo.isNotBlank()) {
            append(request.characterInfo)
            append(LINE_BREAK)
        }
        if (request.recentConversation.isNotEmpty()) {
            request.recentConversation.forEach { message ->
                append(LINE_BREAK)
                append(labelOf(message.role))
                append(SEPARATOR)
                append(message.text)
            }
            append(LINE_BREAK)
        }
        append(LINE_BREAK)
        append(labelOf(ChatMessage.Role.USER))
        append(SEPARATOR)
        append(request.userInput)
        append(LINE_BREAK)
        append(labelOf(ChatMessage.Role.CHARACTER))
        append(SEPARATOR)
    }

    private fun labelOf(role: ChatMessage.Role): String =
        if (role == ChatMessage.Role.USER) LABEL_USER else LABEL_CHARACTER

    const val LABEL_USER = "사용자"
    const val LABEL_CHARACTER = "캐릭터"
    private const val SEPARATOR = ": "
    private const val LINE_BREAK = "\n"
}
