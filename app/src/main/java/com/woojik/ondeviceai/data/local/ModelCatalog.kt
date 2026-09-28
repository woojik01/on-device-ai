package com.woojik.ondeviceai.data.local

import java.io.File

/**
 * 기기 내부 저장소에서 로컬 모델 파일을 찾는다.
 * - .litertlm : LiteRT-LM (기본 런타임, CPU/GPU 백엔드 선택)
 * - .task / .bin / .gguf : MediaPipe LLM Inference 0.10.35 (대체 경로, CPU)
 */
class ModelCatalog(
    private val modelsDir: File,
) {

    /** 지원하는 확장자 중 가장 최근에 추가된 모델 파일. 없으면 null. */
    fun findModelFile(): File? =
        modelsDir
            .listFiles { file -> file.isFile && SUPPORTED_EXTENSIONS.contains(file.extension.lowercase()) }
            ?.maxByOrNull { it.lastModified() }

    companion object {
        const val MODELS_DIR_NAME = "models"
        const val EXT_LITERTLM = "litertlm"
        val SUPPORTED_EXTENSIONS = setOf(EXT_LITERTLM, "task", "bin", "gguf")
    }
}
