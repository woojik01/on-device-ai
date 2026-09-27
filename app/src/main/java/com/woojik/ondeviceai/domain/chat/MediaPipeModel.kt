package com.woojik.ondeviceai.domain.chat

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

/**
 * MediaPipe LLM Inference 기반 로컬 모델 구현 (PRD-02 1차 후보 런타임).
 *
 * MediaPipe 0.10.35 세션 API 구조:
 * 1. `session.addQueryChunk(prompt)` — 프롬프트를 세션 큐에 적재
 * 2. `session.generateResponseAsync(progressListener)` — 생성 시작, 부분 결과 콜백 수신
 *    (프롬프트를 generateResponseAsync 인자로 넘기지 않는다)
 *
 * - 요청마다 세션을 새로 만들어 전체 컨텍스트를 프롬프트로 전달한다 (세션 히스토리 중복 방지).
 * - 진행 콜백은 누적 부분 결과를 전달하므로, 증분만 잘라 Token으로 방출한다.
 * - 수집 취소 시 세션을 닫아 생성을 중단한다.
 */
class MediaPipeModel(
    private val context: Context,
    private val modelPath: String,
) : ChatModel {

    override val name: String = "mediapipe-" + File(modelPath).nameWithoutExtension

    private val inferenceLock = Any()

    @Volatile
    private var inference: LlmInference? = null

    override fun generate(request: GenerationRequest): Flow<ChatModelEvent> = callbackFlow {
        val prompt = PromptBuilder.build(request)

        val llm = tryLoad(GenerationError.LOADING_FAILED, ::trySend) { ensureInference() }
        if (llm == null) {
            close()
            return@callbackFlow
        }

        val session = tryLoad(GenerationError.LOADING_FAILED, ::trySend) {
            LlmInferenceSession.createFromOptions(
                llm,
                LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTemperature(TEMPERATURE)
                    .setTopK(TOP_K)
                    .build(),
            )
        }
        if (session == null) {
            close()
            return@callbackFlow
        }

        var emittedLength = 0

        try {
            // 프롬프트 적재 후 생성 시작 (MediaPipe 0.10.35 API)
            session.addQueryChunk(prompt)
            session.generateResponseAsync { partial, done ->
                if (partial != null && partial.length > emittedLength) {
                    trySend(ChatModelEvent.Token(partial.substring(emittedLength)))
                    emittedLength = partial.length
                }
                if (done) {
                    trySend(ChatModelEvent.Completed(partial ?: ""))
                    close()
                }
            }
        } catch (e: OutOfMemoryError) {
            trySend(ChatModelEvent.Failed(GenerationError.OUT_OF_MEMORY))
            close()
        } catch (e: Exception) {
            trySend(ChatModelEvent.Failed(GenerationError.GENERATION_FAILED))
            close()
        }

        awaitClose {
            runCatching { session.close() }
        }
    }

    /** 로딩/생성 실패 후 재시도할 수 있도록 로드된 런타임을 정리한다. */
    fun reset() {
        synchronized(inferenceLock) {
            runCatching { inference?.close() }
            inference = null
        }
    }

    private fun <T : Any> tryLoad(
        failure: GenerationError,
        sendEvent: (ChatModelEvent) -> Unit,
        loader: () -> T,
    ): T? = try {
        loader()
    } catch (e: OutOfMemoryError) {
        sendEvent(ChatModelEvent.Failed(GenerationError.OUT_OF_MEMORY))
        null
    } catch (e: Exception) {
        sendEvent(ChatModelEvent.Failed(failure))
        null
    }

    private fun ensureInference(): LlmInference =
        inference ?: synchronized(inferenceLock) {
            inference ?: LlmInference.createFromOptions(
                context,
                LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(MAX_TOKENS)
                    .build(),
            ).also { inference = it }
        }

    companion object {
        const val MAX_TOKENS = 1024
        const val TEMPERATURE = 0.8f
        const val TOP_K = 40
    }
}
