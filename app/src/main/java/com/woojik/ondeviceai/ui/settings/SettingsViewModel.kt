package com.woojik.ondeviceai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.woojik.ondeviceai.ServiceLocator
import com.woojik.ondeviceai.data.model.AppSettings
import com.woojik.ondeviceai.data.model.DarkThemeMode
import com.woojik.ondeviceai.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setDarkThemeMode(mode: DarkThemeMode) {
        viewModelScope.launch { repository.setDarkThemeMode(mode) }
    }

    fun setCharacterName(name: String) {
        viewModelScope.launch { repository.setCharacterName(name) }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val locator: ServiceLocator) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(locator.settingsRepository) as T
    }

    companion object {
        fun factory(locator: ServiceLocator) = Factory(locator)
    }
}
