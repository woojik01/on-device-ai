package com.woojik.ondeviceai.domain.chat

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

/**
 * LiteRT-LM 기반 로컬 모델 구현 (.litertlm, 기본 런타임).
 * 백엔드로 CPU/GPU를 선택할 수 있다 (GPU는 OpenCL 기반, 기기 미지원 시 로드 실패 → 설정에서 CPU로 전환).
 *
 * API 흐름 (litertlm-android, 공식 Kotlin API):
 * 1. Engine(EngineConfig(modelPath, backend, cacheDir)) + initialize() — 첫 로드에 수 초 소요
 * 2. engine.createConversation() — 요청마다 새 대화 (프롬프트에 컨텍스트 포함)
 * 3. conversation.sendMessageAsync(prompt): Flow<Message> — 증분 텍스트 스트리밍
 *
 * - 모델 파일 다운로드만 외부 네트워크를 쓰고 대화·기억은 절대 전송하지 않는다 (PRD 데이터 원칙).
 */
class LiteRtModel(
    private val context: Context,
    private val modelPath: String,
    private val useGpu: Boolean = false,
) : ChatModel {

    override val name: String =
        (if (useGpu) "litertlm-gpu-" else "litertlm-cpu-") + File(modelPath).nameWithoutExtension

    private val engineLock = Any()

    @Volatile
    private var engine: Engine? = null

    override fun generate(request: GenerationRequest): Flow<ChatModelEvent> = callbackFlow {
        val prompt = PromptBuilder.build(request)

        val eng = tryLoad { ensureEngine() }
        if (eng == null) {
            trySend(ChatModelEvent.Failed(GenerationError.LOADING_FAILED))
            close()
            return@callbackFlow
        }

        val full = StringBuilder()
        try {
            eng.createConversation().use { conversation ->
                conversation.sendMessageAsync(prompt).collect { message ->
                    val chunk = message.toString()
                    if (chunk.isNotEmpty()) {
                        full.append(chunk)
                        trySend(ChatModelEvent.Token(chunk))
                    }
                }
            }
            trySend(ChatModelEvent.Completed(full.toString()))
        } catch (e: CancellationException) {
            // 수집 취소: Conversation은 use 블록에서 정리된다.
            close()
            return@callbackFlow
        } catch (e: OutOfMemoryError) {
            reset()
            trySend(ChatModelEvent.Failed(GenerationError.OUT_OF_MEMORY))
        } catch (e: Exception) {
            trySend(ChatModelEvent.Failed(GenerationError.GENERATION_FAILED))
        }
        close()
    }

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
                    backend = if (useGpu) Backend.GPU() else Backend.CPU(),
                    // 캐시 디렉터리를 지정하면 두 번째 로드가 빨라진다 (공식 문서 권장).
                    cacheDir = File(context.cacheDir, CACHE_DIR_NAME).let { dir ->
                        dir.mkdirs()
                        dir.absolutePath
                    },
                ),
            ).also { created ->
                created.initialize()
                engine = created
            }
        }

    companion object {
        private const val CACHE_DIR_NAME = "litertlm"
    }
}
