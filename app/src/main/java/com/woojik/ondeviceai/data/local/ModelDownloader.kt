package com.woojik.ondeviceai.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 앱 내부에서 로컬 모델을 직접 다운로드한다.
 * 모델 파일만 외부에서 받고 대화·기억·상태는 절대 전송하지 않는다 (PRD 데이터 원칙).
 */
class ModelDownloader(
    private val modelsDir: File,
) {

    sealed interface DownloadResult {
        data class Success(val file: File) : DownloadResult
        data class Failed(val reason: Reason) : DownloadResult
    }

    enum class Reason { NETWORK_ERROR, IO_ERROR }

    /**
     * 모델을 models 디렉터리로 다운로드한다.
     * 진행률(0~100)은 onProgress로 전달되며, 임시 파일(.downloading)이 완성된 뒤 이름을 바꾼다.
     */
    suspend fun download(
        spec: DownloadSpec,
        onProgress: (Int) -> Unit = {},
    ): DownloadResult = withContext(Dispatchers.IO) {
        val temp = File(modelsDir, spec.fileName + TEMP_SUFFIX)
        try {
            modelsDir.mkdirs()
            val connection = URL(spec.url).openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true

            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (total > 0) onProgress(((copied * 100) / total).toInt())
                    }
                }
            }

            // 완전히 받았는지 확인 (서버가 길이를 알려준 경우)
            if (total > 0 && temp.length() < total) {
                temp.delete()
                return@withContext DownloadResult.Failed(Reason.NETWORK_ERROR)
            }

            val target = File(modelsDir, spec.fileName)
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            target.setLastModified(System.currentTimeMillis())
            DownloadResult.Success(target)
        } catch (e: Exception) {
            temp.delete()
            DownloadResult.Failed(Reason.IO_ERROR)
        }
    }

    companion object {
        private const val TEMP_SUFFIX = ".downloading"
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 60_000
        private const val BUFFER_SIZE = 64 * 1024

        /** 기본 다운로드 대상 (게이트 없는 공개 Hugging Face 저장소). */
        val DEFAULT_SPEC = DownloadSpec(
            url = "https://huggingface.co/ASahu16/gemma/resolve/main/gemma-2b-it-cpu-int4.bin",
            fileName = "gemma-2b-it-cpu-int4.bin",
            sizeBytes = 1_346_559_040L,
            displayName = "Gemma 2B IT (CPU int4)",
        )
    }
}

/** 다운로드 대상 모델 정의. */
data class DownloadSpec(
    val url: String,
    val fileName: String,
    val sizeBytes: Long,
    val displayName: String,
)
