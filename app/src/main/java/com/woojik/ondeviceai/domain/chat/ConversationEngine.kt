package com.woojik.ondeviceai.domain.chat

import com.woojik.ondeviceai.data.model.AppSettings
import com.woojik.ondeviceai.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 대화 엔진: 컨텍스트 구성 → 로컬 모델 추론 → 스트리밍 응답.
 * UI는 이 엔진에만 의존하며, 모델 구현은 언제든 교체 가능하다.
 */
class ConversationEngine(
    private val model: ChatModel,
    private val contextAssembler: ContextAssembler = ContextAssembler(),
) {

    /** 사용자 입력에 대한 응답 생성. 수집 취소 시 즉시 중단된다. */
    fun generateReply(
        history: List<ChatMessage>,
        settings: AppSettings,
        userInput: String,
    ): Flow<ChatModelEvent> = flow {
        if (userInput.isBlank()) {
            emit(ChatModelEvent.Failed(GenerationError.GENERATION_FAILED))
            return@flow
        }
        val request = contextAssembler.assemble(history, settings, userInput)
        model.generate(request).collect { event ->
            emit(event)
        }
    }

    /** 컨텍스트 조립 결과 노출(테스트/디버그용). */
    fun assembleRequest(history: List<ChatMessage>, settings: AppSettings, userInput: String): GenerationRequest =
        contextAssembler.assemble(history, settings, userInput)
}
