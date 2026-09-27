package com.woojik.ondeviceai.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.woojik.ondeviceai.ServiceLocator
import com.woojik.ondeviceai.data.local.ModelImporter
import com.woojik.ondeviceai.data.model.AppSettings
import com.woojik.ondeviceai.data.model.DarkThemeMode
import com.woojik.ondeviceai.data.repository.SettingsRepository
import com.woojik.ondeviceai.domain.chat.EchoModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val locator: ServiceLocator? = null,
) : ViewModel() {

    /** 로컬 모델 상태: 현재 사용 중인 모델 / 가져오는 중 / 실패. */
    sealed interface ModelState {
        data class Active(val modelName: String, val isLocal: Boolean) : ModelState
        data object Importing : ModelState
        data class Failed(val message: String) : ModelState
    }

    val settings: StateFlow<AppSettings> = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _modelState = MutableStateFlow<ModelState>(currentModelState())
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    fun setDarkThemeMode(mode: DarkThemeMode) {
        viewModelScope.launch { repository.setDarkThemeMode(mode) }
    }

    fun setCharacterName(name: String) {
        viewModelScope.launch { repository.setCharacterName(name) }
    }

    /** 선택한 모델 파일을 앱 내부로 복사하고 모델을 교체한다. */
    fun importModel(uri: Uri) {
        val loc = locator ?: return
        viewModelScope.launch {
            _modelState.value = ModelState.Importing
            when (val result = loc.modelImporter.import(uri)) {
                is ModelImporter.ImportResult.Success -> {
                    loc.reloadModel()
                    _modelState.value = currentModelState()
                }
                is ModelImporter.ImportResult.Failed -> {
                    _modelState.value = ModelState.Failed(
                        when (result.reason) {
                            ModelImporter.Reason.UNSUPPORTED_EXTENSION ->
                                "지원하지 않는 파일 형식이에요. .task / .bin / .gguf 파일을 선택해 주세요."
                            ModelImporter.Reason.IO_ERROR ->
                                "모델 파일을 복사하지 못했어요. 저장 공간이 충분한지 확인해 주세요."
                        },
                    )
                }
            }
        }
    }

    private fun currentModelState(): ModelState {
        val model = locator?.chatModel ?: return ModelState.Active("unknown", false)
        return ModelState.Active(modelName = model.name, isLocal = model !is EchoModel)
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val locator: ServiceLocator) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(locator.settingsRepository, locator) as T
    }

    companion object {
        fun factory(locator: ServiceLocator) = Factory(locator)
    }
}
