package com.woojik.ondeviceai.data.repository

import com.woojik.ondeviceai.data.local.AppPreferences
import com.woojik.ondeviceai.data.model.AppSettings
import com.woojik.ondeviceai.data.model.DarkThemeMode
import kotlinx.coroutines.flow.Flow

/** 설정 관련 도메인 로직. */
class SettingsRepository(private val preferences: AppPreferences) {

    fun observeSettings(): Flow<AppSettings> = preferences.observeSettings()

    suspend fun setDarkThemeMode(mode: DarkThemeMode) {
        preferences.updateSettings { it.copy(darkThemeMode = mode) }
    }

    suspend fun setCharacterName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        preferences.updateSettings { it.copy(characterName = trimmed) }
    }
}
