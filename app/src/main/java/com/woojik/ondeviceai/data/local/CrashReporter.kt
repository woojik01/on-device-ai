package com.woojik.ondeviceai.data.local

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 처리되지 않은 예외(비정상 종료)를 기기 내 파일로 남긴다.
 * PC/adb 없이 기기만 쓰는 PRD-02 제약에서 크래시 원인을 확인하기 위한 용도.
 * 로그는 기기 안에만 저장되고 외부로 전송되지 않는다 (PRD 데이터 원칙).
 */
object CrashReporter {

    private const val FILE_NAME = "last_crash.txt"
    private const val MAX_BYTES = 64 * 1024
    private const val MAX_LOG_LINES = 40

    /** 앱 프로세스에 기본 예외 핸들러를 설치한다. */
    fun install(filesDir: File) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { write(filesDir, thread, throwable) }
            // 시스템 기본 동작(프로세스 종료)은 그대로 유지한다.
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** 마지막 비정상 종료 로그. 없으면 null. */
    fun readLast(filesDir: File): String? {
        val file = File(filesDir, FILE_NAME)
        if (!file.exists()) return null
        return runCatching { file.readText() }.getOrNull()
    }

    /** 로그를 비운다 (원인 해결 후). */
    fun clear(filesDir: File) {
        runCatching { File(filesDir, FILE_NAME).delete() }
    }

    private fun write(filesDir: File, thread: Thread, throwable: Throwable) {
        val file = File(filesDir, FILE_NAME)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val stack = throwable.stackTraceToString()
            .lineSequence()
            .take(MAX_LOG_LINES)
            .joinToString("\n")
        val content = timestamp + " [thread=" + thread.name + "]\n" +
            throwable.javaClass.name + ": " + throwable.message + "\n" + stack
        file.parentFile?.mkdirs()
        // 너무 커지지 않도록 잘라낸다.
        file.writeText(if (content.length > MAX_BYTES) content.take(MAX_BYTES) else content)
    }
}
