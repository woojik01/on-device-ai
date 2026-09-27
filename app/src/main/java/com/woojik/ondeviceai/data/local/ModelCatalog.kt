package com.woojik.ondeviceai.data.local

import java.io.File

/**
 * 기기 내부 저장소에서 로컬 모델 파일을 찾는다.
 * 모델 바이너리는 용량 문제로 Git에 커밋하지 않고 adb로 배치한다 (docs/model-setup.md).
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
        val SUPPORTED_EXTENSIONS = setOf("task", "bin", "gguf")
    }
}
