package com.woojik.ondeviceai.domain.chat

import com.woojik.ondeviceai.data.model.AppSettings
import com.woojik.ondeviceai.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 대화 엔진: 컨텍스트 구성 → 로컬 모델 추론 → 스트리밍 응답.
 * UI는 이 엔진에만 의존하며, 모델 구현은 언제든 교체 가능하다.
 *
 * 모델을 생성 시점마다 provider에서 조회하므로,
 * 설정 화면에서 모델을 가져온 뒤 교체해도 엔진 재생성이 필요 없다.
 */
class ConversationEngine(
    private val modelProvider: () -> ChatModel,
    private val contextAssembler: ContextAssembler = ContextAssembler(),
) {

    /** 고정 모델 사용 시 (테스트/단순 구성). */
    constructor(model: ChatModel) : this({ model })

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
        modelProvider().generate(request).collect { event ->
            emit(event)
        }
    }

    /** 컨텍스트 조립 결과 노출(테스트/디버그용). */
    fun assembleRequest(history: List<ChatMessage>, settings: AppSettings, userInput: String): GenerationRequest =
        contextAssembler.assemble(history, settings, userInput)
}
