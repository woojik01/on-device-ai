package com.woojik.ondeviceai.domain.chat

import com.woojik.ondeviceai.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * 대화 모델 추상화. UI는 이 인터페이스에만 의존한다.
 * 교체 가능 대상: 로컬 LLM, 더 작은 양자화 모델, 다른 로컬 런타임, 개발용 클라우드 모델.
 */
interface ChatModel {
    /** 디버/표시용 모델 이름 */
    val name: String

    /** 요청을 받아 토큰 스트림을 방출한다. 취소는 Flow 수집 취소로 처리한다. */
    fun generate(request: GenerationRequest): Flow<ChatModelEvent>
}

/** 생성 요청: 컨텍스트 조립 결과와 사용자 입력. */
data class GenerationRequest(
    val systemInstruction: String,
    val characterInfo: String,
    val recentConversation: List<ChatMessage>,
    val userInput: String,
)

/** 모델 생성 중 발생하는 이벤트. */
sealed interface ChatModelEvent {
    /** 스트리밍 토큰 (조각). */
    data class Token(val text: String) : ChatModelEvent

    /** 생성 완료. 전체 텍스트를 담는다. */
    data class Completed(val fullText: String) : ChatModelEvent

    /** 복구 가능한 오류. 앱은 종료되지 않는다. */
    data class Failed(val error: GenerationError) : ChatModelEvent
}

/** 오류 종류 (PRD-02 오류 처리 요구사항). */
enum class GenerationError(val userMessage: String) {
    LOADING_FAILED("모델을 불러오지 못했어요. 다시 시도해 주세요."),
    OUT_OF_MEMORY("메모리가 부족해요. 잠시 후 다시 시도해 주세요."),
    GENERATION_FAILED("응답을 만들지 못했어요. 다시 시도해 주세요."),
    CANCELLED("생성이 취소되었어요."),
    MODEL_MISSING("모델 파일이 없어요. 설정에서 모델을 확인해 주세요."),
}
