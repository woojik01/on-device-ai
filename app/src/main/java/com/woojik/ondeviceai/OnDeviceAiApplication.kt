package com.woojik.ondeviceai

import android.app.Application
import com.woojik.ondeviceai.data.local.AppPreferences
import com.woojik.ondeviceai.data.local.CrashReporter
import com.woojik.ondeviceai.data.local.DataStoreAppPreferences
import com.woojik.ondeviceai.data.local.ChatStore
import com.woojik.ondeviceai.data.local.DataStoreChatStore
import com.woojik.ondeviceai.data.local.ModelCatalog
import com.woojik.ondeviceai.data.local.ModelDownloader
import com.woojik.ondeviceai.data.local.ModelImporter
import com.woojik.ondeviceai.data.model.ModelBackend
import com.woojik.ondeviceai.data.repository.ChatRepository
import com.woojik.ondeviceai.data.repository.SettingsRepository
import com.woojik.ondeviceai.domain.chat.ChatModel
import com.woojik.ondeviceai.domain.chat.ConversationEngine
import com.woojik.ondeviceai.domain.chat.EchoModel
import com.woojik.ondeviceai.domain.chat.LiteRtModel
import com.woojik.ondeviceai.domain.chat.MediaPipeModel
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 간단한 수동 의존성 컨테이너.
 * 로컬 저장 계층과 ChatModel은 인터페이스로 추상화되어 있어
 * 이후 SQLite/다른 로컬 런타임 도입 시 구현체만 교체하면 된다.
 */
class ServiceLocator(private val application: Application) {
    val appPreferences: AppPreferences = DataStoreAppPreferences(application)
    val chatStore: ChatStore = DataStoreChatStore(application)
    val chatRepository: ChatRepository = ChatRepository(chatStore)
    val settingsRepository: SettingsRepository = SettingsRepository(appPreferences)

    /** 크래시 로그 등 앱 내부 파일 접근용. */
    fun applicationFilesDir(): File = application.filesDir

    private val modelsDir = File(application.filesDir, ModelCatalog.MODELS_DIR_NAME)
    private val modelCatalog = ModelCatalog(modelsDir)

    /** 기기 내 파일(다운로드 등)을 앱 내부 저장소로 가져온다. */
    val modelImporter = ModelImporter(application, modelsDir)

    /** 로컬 모델 자동 다운로드 (설정 화면, Wi-Fi 권장). */
    val modelDownloader = ModelDownloader(modelsDir)

    /**
     * 로컬 모델(.litertlm) 실행 백엔드. 설정 화면에서 CPU/GPU를 선택한다.
     * GPU 변형 모델과 CPU 변형 모델은 파일이 다르므로 전환 시 재다운로드가 필요하다.
     */
    @Volatile
    var modelBackend: ModelBackend = ModelBackend.CPU
        private set

    /**
     * 기기 내부 저장소의 로컬 모델을 확장자에 맞는 런타임으로 구동한다.
     * .litertlm → LiteRT-LM (기본, CPU/GPU 선택), .task/.bin/.gguf → MediaPipe (CPU).
     * 모델이 없으면 개발용 EchoModel로 폴백한다 (docs/model-setup.md).
     */
    var chatModel: ChatModel = loadModel()
        private set

    /**
     * provider로 현재 모델을 조회한다. 모델 다운로드/가져오기(reloadModel) 후
     * 다음 생성부터 새 모델이 사용된다 (생성 시점 값 고정 방지).
     */
    val conversationEngine: ConversationEngine = ConversationEngine(modelProvider = { chatModel })

    /** 모델 다운로드/가져오기 후 호출: 최신 모델 파일로 교체한다. */
    fun reloadModel() {
        chatModel = loadModel()
    }

    /** 백엔드(CPU/GPU) 변경: LiteRT-LM 모델을 새 백엔드로 다시 로드한다. */
    fun setModelBackend(backend: ModelBackend) {
        if (modelBackend == backend) return
        modelBackend = backend
        if (modelCatalog.findModelFile()?.extension?.lowercase() == ModelCatalog.EXT_LITERTLM) {
            reloadModel()
        }
    }

    private fun loadModel(): ChatModel =
        modelCatalog.findModelFile()?.let { file ->
            when (file.extension.lowercase()) {
                ModelCatalog.EXT_LITERTLM ->
                    LiteRtModel(application, file.absolutePath, useGpu = modelBackend == ModelBackend.GPU)
                else -> MediaPipeModel(application, file.absolutePath)
            }
        } ?: EchoModel()
}

class OnDeviceAiApplication : Application() {
    lateinit var locator: ServiceLocator
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // 비정상 종료 시 기기 내 로그를 남긴다 (adb 없는 실기 디버깅용).
        CrashReporter.install(filesDir)
        locator = ServiceLocator(this)
        // 저장된 백엔드 설정(CPU/GPU)을 모델 로드에 반영한다.
        appScope.launch {
            locator.settingsRepository.observeSettings()
                .map { it.modelBackend }
                .distinctUntilChanged()
                .collect { backend -> locator.setModelBackend(backend) }
        }
    }
}
