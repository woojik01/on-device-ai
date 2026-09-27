package com.woojik.ondeviceai.domain.chat

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

/** MediaPipe LLM Inference 기반 로컬 모델 구현. */
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

        var emittedLength = 0

        try {
            // MediaPipe Session API: query chunk를 먼저 추가한 뒤 async generation을 시작한다.
            session.addQueryChunk(prompt)
            session.generateResponseAsync { partial, done ->
                if (partial != null && partial.isNotEmpty()) {
                    trySend(ChatModelEvent.Token(partial))
                }
                if (done) {
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
