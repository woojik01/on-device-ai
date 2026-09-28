package com.woojik.ondeviceai.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.woojik.ondeviceai.ServiceLocator
import com.woojik.ondeviceai.data.local.ModelDownloader
import com.woojik.ondeviceai.data.local.ModelImporter
import com.woojik.ondeviceai.data.model.AppSettings
import com.woojik.ondeviceai.data.model.DarkThemeMode
import com.woojik.ondeviceai.data.repository.SettingsRepository
import com.woojik.ondeviceai.domain.chat.EchoModel
import kotlinx.coroutines.Job
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

    /** 로컬 모델 상태: 현재 사용 중인 모델 / 복사 중 / 다운로드 중 / 실패. */
    sealed interface ModelState {
        data class Active(val modelName: String, val isLocal: Boolean) : ModelState
        data object Importing : ModelState
        data class Downloading(val progressPercent: Int?) : ModelState
        data class Failed(val message: String) : ModelState
    }

    val settings: StateFlow<AppSettings> = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _modelState = MutableStateFlow<ModelState>(currentModelState())
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private var downloadJob: Job? = null

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

    /** 기본 후보 모델(Gemma 2B int4, 약 1.3GB)을 기기로 직접 다운로드하고 교체한다. */
    fun downloadModel() {
        val loc = locator ?: return
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch {
            _modelState.value = ModelState.Downloading(null)
            try {
                when (val result = loc.modelDownloader.download(
                    url = ModelDownloader.DEFAULT_MODEL_URL,
                    fileName = ModelDownloader.DEFAULT_MODEL_FILE_NAME,
                ) { percent -> _modelState.value = ModelState.Downloading(percent) }) {
                    is ModelDownloader.DownloadResult.Success -> {
                        loc.reloadModel()
                        _modelState.value = currentModelState()
                    }
                    is ModelDownloader.DownloadResult.Failed -> {
                        _modelState.value = ModelState.Failed(
                            when (result.reason) {
                                ModelDownloader.Reason.NETWORK_ERROR ->
                                    "모델 다운로드에 실패했어요. 네트워크 연결(Wi-Fi 권장)을 확인해 주세요."
                                ModelDownloader.Reason.IO_ERROR ->
                                    "모델을 저장하지 못했어요. 저장 공간이 충분한지(약 1.5GB 이상) 확인해 주세요."
                            },
                        )
                    }
                }
            } finally {
                // 취소 시 다운로드 상태가 화면에 남지 않도록 한다.
                if (_modelState.value is ModelState.Downloading) {
                    _modelState.value = currentModelState()
                }
            }
        }
    }

    /** 진행 중인 모델 다운로드를 취소한다. */
    fun cancelDownload() {
        downloadJob?.cancel()
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
