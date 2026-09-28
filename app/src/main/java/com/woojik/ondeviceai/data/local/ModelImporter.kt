package com.woojik.ondeviceai.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 기기의 다운로드 파일 등(SAF로 선택한 Uri)을 앱 내부 models 디렉터리로 복사한다.
 * PC/adb 없이 기기만으로 로컬 모델을 배치하기 위한 경로 (PRD-02 실기 제약 대응).
 */
class ModelImporter(
    private val context: Context,
    private val modelsDir: File,
) {

    sealed interface ImportResult {
        data class Success(val file: File) : ImportResult
        data class Failed(val reason: Reason) : ImportResult
    }

    enum class Reason { UNSUPPORTED_EXTENSION, IO_ERROR }

    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val fileName = queryDisplayName(uri) ?: DEFAULT_FILE_NAME
        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension !in ModelCatalog.SUPPORTED_EXTENSIONS) {
            return@withContext ImportResult.Failed(Reason.UNSUPPORTED_EXTENSION)
        }

        try {
            modelsDir.mkdirs()
            val target = File(modelsDir, fileName)
            val temp = File(modelsDir, fileName + TEMP_SUFFIX)
            val input = context.contentResolver.openInputStream(uri)
                ?: return@withContext ImportResult.Failed(Reason.IO_ERROR)
            input.use { stream ->
                temp.outputStream().use { output -> stream.copyTo(output) }
            }
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            // 이전 모델들이 섞이지 않도록, 새 모델이 가장 최근 파일이 된다
            target.setLastModified(System.currentTimeMillis())
            ImportResult.Success(target)
        } catch (e: Exception) {
            ImportResult.Failed(Reason.IO_ERROR)
        }
    }

    private fun queryDisplayName(uri: Uri): String? =
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }

    companion object {
        const val DEFAULT_FILE_NAME = "model.task"
        private const val TEMP_SUFFIX = ".importing"
    }
}
