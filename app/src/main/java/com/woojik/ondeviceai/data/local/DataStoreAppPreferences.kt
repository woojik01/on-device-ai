package com.woojik.ondeviceai.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.woojik.ondeviceai.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/** DataStore 기반 AppPreferences 구현. */
class DataStoreAppPreferences(context: Context) : AppPreferences {
    private val dataStore = context.settingsDataStore
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val KEY_SETTINGS = stringPreferencesKey("settings_json")
        private val settingsSerializer = AppSettings.serializer()
    }

    override fun observeSettings(): Flow<AppSettings> =
        dataStore.data.map { prefs ->
            prefs[KEY_SETTINGS]?.let { raw ->
                runCatching { json.decodeFromString(settingsSerializer, raw) }.getOrNull()
            } ?: AppSettings()
        }

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_SETTINGS]?.let { raw ->
                runCatching { json.decodeFromString(settingsSerializer, raw) }.getOrNull()
            } ?: AppSettings()
            prefs[KEY_SETTINGS] = json.encodeToString(settingsSerializer, transform(current))
        }
    }
}
