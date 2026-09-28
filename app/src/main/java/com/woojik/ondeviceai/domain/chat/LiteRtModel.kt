package com.woojik.ondeviceai.domain.chat

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.woojik.ondeviceai.data.model.ChatMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * LiteRT-LM 기반 로컬 모델 구현 (PRD-02 1차 후보 런타임, litertlm-android 0.16.1).
 *
 * API 흐름 (LiteRT-LM Kotlin API):
 * 1. Engine(EngineConfig(modelPath, backend, cacheDir)) + initialize() — 엔진은 재사용 캐시
 * 2. 요청마다 createConversation(ConversationConfig) — 시스템 지침/최근 대화를 컨텍스트로 전달
 * 3. conversation.sendMessageAsync(prompt): Flow<Message> — 스트리밍 응답
 * 4. 수집 취소 시 코루틴 취소로 전파되며 Conversation이 정리된다.
 *
 * - 스트림이 증분(토큰 조각) 또는 누적 텍스트 중 어느 쪽으로 방출돼도 동작하도록 방어적으로 처리한다.
 * - engine.initialize()는 수 초 이상 걸릴 수 있어 Dispatchers.IO에서 실행한다.
 */
class LiteRtModel(
    private val context: Context,
    private val modelPath: String,
) : ChatModel {

    override val name: String = "litert-" + File(modelPath).nameWithoutExtension

    private val engineLock = Any()

    @Volatile
    private var engine: Engine? = null

    override fun generate(request: GenerationRequest): Flow<ChatModelEvent> = flow {
        val llm = tryLoad { ensureEngine() }
        if (llm == null) {
            emit(ChatModelEvent.Failed(GenerationError.LOADING_FAILED))
            return@flow
        }

        val conversation = tryLoad { llm.createConversation(buildConversationConfig(request)) }
        if (conversation == null) {
            emit(ChatModelEvent.Failed(GenerationError.LOADING_FAILED))
            return@flow
        }

        conversation.use { conv ->
            val accumulated = StringBuilder()
            try {
                conv.sendMessageAsync(request.userInput).collect { message ->
                    val text = message.text
                    if (text.isNullOrEmpty()) return@collect
                    val current = accumulated.toString()
                    // 누적 텍스트로 방출되는 경우: 앞 부분이 지금까지의 누적과 같으면 새로 늘어난 부분만 방출.
                    // 증분(토큰 조각)으로 방출되는 경우: 전체를 delta로 방출한다.
                    val delta = if (text.length > current.length && text.startsWith(current)) {
                        text.substring(current.length)
                    } else if (text != current) {
                        text
                    } else {
                        ""
                    }
                    if (delta.isNotEmpty()) {
                        accumulated.append(delta)
                        emit(ChatModelEvent.Token(delta))
                    }
                }
                emit(ChatModelEvent.Completed(accumulated.toString()))
            } catch (e: OutOfMemoryError) {
                emit(ChatModelEvent.Failed(GenerationError.OUT_OF_MEMORY))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emit(ChatModelEvent.Failed(GenerationError.GENERATION_FAILED))
            }
        }
    }.flowOn(Dispatchers.IO)

    /** 로딩/생성 실패 후 재시도할 수 있도록 로드된 엔진을 정리한다. */
    fun reset() {
        synchronized(engineLock) {
            runCatching { engine?.close() }
            engine = null
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

    private fun ensureEngine(): Engine =
        engine ?: synchronized(engineLock) {
            engine ?: Engine(
                EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.CPU(),
                    cacheDir = context.cacheDir.path,
                ),
            ).also { candidate ->
                candidate.initialize()
                engine = candidate
            }
        }

    private fun buildConversationConfig(request: GenerationRequest): ConversationConfig =
        ConversationConfig(
            systemInstruction = Contents.of(
                request.systemInstruction + System.lineSeparator() + request.characterInfo,
            ),
            initialMessages = request.recentConversation.map { message ->
                when (message.role) {
                    ChatMessage.Role.USER -> Message.user(message.text)
                    ChatMessage.Role.CHARACTER -> Message.model(message.text)
                }
            },
            samplerConfig = SamplerConfig(topK = TOP_K, topP = TOP_P, temperature = TEMPERATURE),
        )

    companion object {
        const val TEMPERATURE = 0.8
        const val TOP_K = 40
        const val TOP_P = 0.95
    }
}
