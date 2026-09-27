package com.woojik.ondeviceai.domain.chat

import com.woojik.ondeviceai.data.model.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 개발용 임시 모델.
 * S22 실측 벤치마크 후 로컬 양자화 모델로 교체한다 (PRD-02 온디바이스 모델 항목).
 * 인터페이스 계약(토큰 스트리밍, 취소 가능, 오류 이벤트)을 그대로 만족한다.
 */
class EchoModel : ChatModel {
    override val name: String = "echo-dev"

    override fun generate(request: GenerationRequest): Flow<ChatModelEvent> = flow {
        val lastUser = request.recentConversation
            .lastOrNull { it.role == ChatMessage.Role.USER }
        val reply = buildString {
            append(request.userInput)
            append("라고 하셨군요! 저는 아직 개발용 임시 모델이에요. 곧 진짜 로컬 모델로 대체될 예정이에요.")
            if (lastUser != null && lastUser.text != request.userInput) {
                append(" (이전에도 ")
                append(lastUser.text)
                append("라고 말씀하셨죠)")
            }
        }
        // 스트리밍 시뮬레이션: 조각 단위 방출, 취소 가능
        reply.chunked(4).forEach { chunk ->
            delay(STREAM_DELAY_MS)
            emit(ChatModelEvent.Token(chunk))
        }
        emit(ChatModelEvent.Completed(reply))
    }

    companion object {
        const val STREAM_DELAY_MS = 50L
    }
}
