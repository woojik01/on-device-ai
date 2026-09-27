package com.woojik.ondeviceai.data.model

import kotlinx.serialization.Serializable

/** 대화 메시지. 발신자와 시각, 내용만 보관한다. */
@Serializable
data class ChatMessage(
    val id: Long,
    val role: Role,
    val text: String,
    val timestampMillis: Long,
) {
    enum class Role { USER, CHARACTER }

    companion object {
        fun newUserMessage(text: String, timestampMillis: Long, nextId: Long): ChatMessage =
            ChatMessage(id = nextId, role = Role.USER, text = text, timestampMillis = timestampMillis)
    }
}
