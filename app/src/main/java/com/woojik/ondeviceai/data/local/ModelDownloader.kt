package com.woojik.ondeviceai.data.local

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** 백엔드별 기본 다운로드 대상. */
data class ModelDownloadTarget(
    val url: String,
    val fileName: String,
    val sizeGb: String,
)

/**
 * 로컬 모델을 기기 내부 저장소로 직접 다운로드한다.
 * PC/adb 없이 기기만으로 모델을 배치하기 위한 기본 경로 (PRD-02 실기 제약 대응).
 *
 * - 진행률은 백분율 콜백으로 전달한다 (서버가 전체 크기를 알려주지 않으면 null).
 * - 코루틴 취소 시 임시 파일을 정리한다.
 * - 다운로드는 임시 파일로 진행되므로 중단돼도 기존 모델을 덮어쓰지 않는다.
 * - 모델 파일만 외부에서 받고 대화·기억·상태는 절대 전송하지 않는다 (PRD 데이터 원칙).
 */
class ModelDownloader(
    private val modelsDir: File,
) {

    sealed interface DownloadResult {
        data class Success(val file: File) : DownloadResult
        data class Failed(val reason: Reason) : DownloadResult
    }

    enum class Reason { NETWORK_ERROR, IO_ERROR }

    suspend fun download(
        url: String,
        fileName: String,
        onProgress: (percent: Int?) -> Unit,
    ): DownloadResult = withContext(Dispatchers.IO) {
        val temp = File(modelsDir, fileName + TEMP_SUFFIX)
        val target = File(modelsDir, fileName)
        var connection: HttpURLConnection? = null
        try {
            modelsDir.mkdirs()

            val conn = URL(url).openConnection() as HttpURLConnection
            connection = conn
            conn.connectTimeout = CONNECT_TIMEOUT_MILLIS
            conn.readTimeout = READ_TIMEOUT_MILLIS
            conn.instanceFollowRedirects = true

            val code = conn.responseCode
            if (code !in 200..299) {
                return@withContext DownloadResult.Failed(Reason.NETWORK_ERROR)
            }

            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE_BYTES)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        onProgress(if (total > 0) (copied * 100 / total).toInt() else null)
                    }
                }
            }
            conn.disconnect()
            connection = null

            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) {
                temp.delete()
                return@withContext DownloadResult.Failed(Reason.IO_ERROR)
            }
            // 이전 모델들과 섞이지 않도록 새 모델이 가장 최근 파일이 된다
            target.setLastModified(System.currentTimeMillis())
            DownloadResult.Success(target)
        } catch (e: CancellationException) {
            temp.delete()
            throw e
        } catch (e: IOException) {
            temp.delete()
            DownloadResult.Failed(Reason.NETWORK_ERROR)
        } catch (e: Exception) {
            temp.delete()
            DownloadResult.Failed(Reason.IO_ERROR)
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        /**
         * 기본 다운로드 대상: Gemma 4 E2B IT (LiteRT-LM .litertlm).
         * litert-community 공식 저장소 (게이트 없음). 백엔드별로 다른 변형 파일을 쓴다.
         * - CPU: gemma-4-E2B-it.litertlm (약 2.5GB)
         * - GPU: gemma-4-E2B-it-gpu.litertlm (약 1.9GB)
         */
        const val CPU_MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
        const val CPU_MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"
        const val GPU_MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it-gpu.litertlm"
        const val GPU_MODEL_FILE_NAME = "gemma-4-E2B-it-gpu.litertlm"

        /** 선택한 백엔드(CPU/GPU)에 맞는 기본 다운로드 대상을 반환한다. */
        fun defaultModelFor(useGpu: Boolean): ModelDownloadTarget =
            if (useGpu) {
                ModelDownloadTarget(url = GPU_MODEL_URL, fileName = GPU_MODEL_FILE_NAME, sizeGb = "약 1.9GB")
            } else {
                ModelDownloadTarget(url = CPU_MODEL_URL, fileName = CPU_MODEL_FILE_NAME, sizeGb = "약 2.5GB")
            }

        private const val TEMP_SUFFIX = ".downloading"
        private const val CONNECT_TIMEOUT_MILLIS = 30_000
        private const val READ_TIMEOUT_MILLIS = 60_000
        private const val BUFFER_SIZE_BYTES = 64 * 1024
    }
}
