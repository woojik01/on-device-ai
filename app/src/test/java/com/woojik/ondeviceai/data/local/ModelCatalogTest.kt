package com.woojik.ondeviceai.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModelCatalogTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    @Test
    fun `모델이 없으면 null을 반환한다`() {
        val catalog = ModelCatalog(tempDir.root)
        assertNull(catalog.findModelFile())
    }

    @Test
    fun `지원하는 확장자의 모델 파일만 인식한다`() {
        tempDir.newFile("gemma.task")
        tempDir.newFile("notes.txt")
        val catalog = ModelCatalog(tempDir.root)
        assertEquals("gemma.task", catalog.findModelFile()?.name)
    }

    @Test
    fun `bin 모델 파일을 인식한다`() {
        tempDir.newFile("gemma-2b-it-cpu-int4.bin")
        val catalog = ModelCatalog(tempDir.root)
        assertEquals("gemma-2b-it-cpu-int4.bin", catalog.findModelFile()?.name)
    }

    @Test
    fun `여러 모델이 있으면 가장 최근 파일을 선택한다`() {
        val older = tempDir.newFile("old-model.task")
        val newer = tempDir.newFile("new-model.bin")
        older.setLastModified(1_000L)
        newer.setLastModified(2_000L)
        val catalog = ModelCatalog(tempDir.root)
        assertEquals("new-model.bin", catalog.findModelFile()?.name)
    }

    @Test
    fun `대문자 확장자도 인식한다`() {
        tempDir.newFile("MODEL.BIN")
        val catalog = ModelCatalog(tempDir.root)
        assertEquals("MODEL.BIN", catalog.findModelFile()?.name)
    }
}
