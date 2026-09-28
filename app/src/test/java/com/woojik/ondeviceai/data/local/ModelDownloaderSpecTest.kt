package com.woojik.ondeviceai.data.local

import org.junit.Assert.assertTrue
import org.junit.Test

class ModelDownloaderSpecTest {

    @Test
    fun `기본 다운로드 대상은 지원하는 확장자를 가진다`() {
        val spec = ModelDownloader.DEFAULT_SPEC
        val extension = spec.fileName.substringAfterLast('.', "").lowercase()
        assertTrue(
            "기본 모델 확장자($extension)는 ModelCatalog가 인식해야 한다",
            extension in ModelCatalog.SUPPORTED_EXTENSIONS,
        )
    }

    @Test
    fun `기본 다운로드 대상은 https URL과 파일명을 가진다`() {
        val spec = ModelDownloader.DEFAULT_SPEC
        assertTrue(spec.url.startsWith("https://"))
        assertTrue(spec.fileName.isNotBlank())
        assertTrue(spec.displayName.isNotBlank())
        assertTrue(spec.sizeBytes > 0)
    }

    @Test
    fun `임시 파일 확장자는 모델 후보로 오인되지 않는다`() {
        // 다운로드 중 임시 파일(.downloading)이 ModelCatalog에 노출되지 않아야 한다
        val tempName = ModelDownloader.DEFAULT_SPEC.fileName + ".downloading"
        val extension = tempName.substringAfterLast('.', "").lowercase()
        assertTrue(extension !in ModelCatalog.SUPPORTED_EXTENSIONS)
    }
}
