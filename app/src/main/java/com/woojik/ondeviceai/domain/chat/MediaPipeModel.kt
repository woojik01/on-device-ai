package com.woojik.ondeviceai.domain.chat

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

/**
 * MediaPipe LLM Inference 기반 로컬 모델 구현 (PRD-02 1차 후보 런타임, tasks-genai 0.10.35).
 *
 * API 흐름 (0.10.35 세션 API):
 * 1. `session.addQueryChunk(prompt)` — 프롬프트 적재
 * 2. `session.generateResponseAsync(progressListener)` — 부분 결과 콜백 (누적 텍스트 전달)
 * 3. 수집 취소 시 `cancelGenerateResponseAsync()` + 세션 close
 *
 * - 콜백은 누적 부분 결과를 주므로 증분만 잘라 Token으로 방출한다.
 * - 완료(done)는 콜백으로만 처리하고 future를 blocking get 하지 않는다 (취소 즉시 반응).
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

        val llm = tryLoad { ensureInference() }
        if (llm == null) {
            trySend(ChatModelEvent.Failed(GenerationError.LOADING_FAILED))
            close()
            return@callbackFlow
        }

        val session = tryLoad {
            LlmInferenceSession.createFromOptions(
                llm,
                LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTemperature(TEMPERATURE)
                    .setTopK(TOP_K)
                    .build(),
            )
        }
        if (session == null) {
            trySend(ChatModelEvent.Failed(GenerationError.LOADING_FAILED))
            close()
            return@callbackFlow
        }

        // 진행 콜백은 누적 결과를 전달하므로 이번에 새로 늘어난 부분만 방출한다.
        var emittedLength = 0
        try {
            session.addQueryChunk(prompt)
            session.generateResponseAsync { partial, done ->
                if (partial.isNotEmpty() && partial.length > emittedLength) {
                    trySend(ChatModelEvent.Token(partial.substring(emittedLength)))
                    emittedLength = partial.length
                }
                if (done) {
                    trySend(ChatModelEvent.Completed(partial))
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

        // 완료는 콜백으로 처리한다. 여기서는 취소 대기만 한다.
        awaitClose {
            runCatching { session.cancelGenerateResponseAsync() }
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

    private fun <T : Any> tryLoad(loader: () -> T): T? =
        try {
            loader()
        } catch (_: OutOfMemoryError) {
            null
        } catch (_: Exception) {
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
