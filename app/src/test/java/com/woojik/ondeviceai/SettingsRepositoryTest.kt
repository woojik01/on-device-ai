package com.woojik.ondeviceai

import com.woojik.ondeviceai.data.model.DarkThemeMode
import com.woojik.ondeviceai.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {

    @Test
    fun setDarkThemeModeUpdatesSettings() = runTest {
        val preferences = FakeAppPreferences()
        val repository = SettingsRepository(preferences)

        repository.setDarkThemeMode(DarkThemeMode.DARK)

        assertEquals(DarkThemeMode.DARK, repository.observeSettings().first().darkThemeMode)
    }

    @Test
    fun setCharacterNameTrimsAndIgnoresBlankNames() = runTest {
        val preferences = FakeAppPreferences()
        val repository = SettingsRepository(preferences)

        repository.setCharacterName("  미소  ")
        assertEquals("미소", repository.observeSettings().first().characterName)

        repository.setCharacterName("   ")
        assertEquals("미소", repository.observeSettings().first().characterName)
    }

    @Test
    fun defaultSettingsAreProvided() = runTest {
        val preferences = FakeAppPreferences()
        val repository = SettingsRepository(preferences)
        val settings = repository.observeSettings().first()
        assertEquals(DarkThemeMode.FOLLOW_SYSTEM, settings.darkThemeMode)
        assertTrue(settings.characterName.isNotBlank())
    }
}
