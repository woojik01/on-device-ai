package com.woojik.ondeviceai.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.woojik.ondeviceai.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.chatDataStore: DataStore<Preferences> by preferencesDataStore(name = "chat_store")

/** DataStore 기반 ChatStore 구현. */
class DataStoreChatStore(context: Context) : ChatStore {
    private val dataStore = context.chatDataStore
    private val json = Json { ignoreUnknownKeys = true }
    private val messageListSerializer = ListSerializer(ChatMessage.serializer())

    companion object {
        private val KEY_MESSAGES = stringPreferencesKey("messages_json")
        private val KEY_NEXT_ID = longPreferencesKey("next_id")
    }

    override fun observeMessages(): Flow<List<ChatMessage>> =
        dataStore.data.map { prefs -> decode(prefs[KEY_MESSAGES]) }

    override suspend fun appendMessage(message: ChatMessage) {
        dataStore.edit { prefs ->
            val current = decode(prefs[KEY_MESSAGES])
            prefs[KEY_MESSAGES] = json.encodeToString(messageListSerializer, current + message)
            prefs[KEY_NEXT_ID] = (prefs[KEY_NEXT_ID] ?: 0L).coerceAtLeast(message.id + 1)
        }
    }

    override suspend fun clearMessages() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_MESSAGES)
            prefs[KEY_NEXT_ID] = 0L
        }
    }

    private fun decode(raw: String?): List<ChatMessage> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(messageListSerializer, raw) }
            .getOrDefault(emptyList())
    }
}
