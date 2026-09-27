package com.woojik.ondeviceai

import android.app.Application
import com.woojik.ondeviceai.data.local.AppPreferences
import com.woojik.ondeviceai.data.local.DataStoreAppPreferences
import com.woojik.ondeviceai.data.local.ChatStore
import com.woojik.ondeviceai.data.local.DataStoreChatStore
import com.woojik.ondeviceai.data.local.ModelCatalog
import com.woojik.ondeviceai.data.repository.ChatRepository
import com.woojik.ondeviceai.data.repository.SettingsRepository
import com.woojik.ondeviceai.domain.chat.ChatModel
import com.woojik.ondeviceai.domain.chat.ConversationEngine
import com.woojik.ondeviceai.domain.chat.EchoModel
import com.woojik.ondeviceai.domain.chat.MediaPipeModel
import java.io.File

/**
 * 간단한 수동 의존성 컨테이너.
 * 로컬 저장 계층과 ChatModel은 인터페이스로 추상화되어 있어
 * 이후 SQLite/다른 로컬 런타임 도입 시 구현체만 교체하면 된다.
 */
class ServiceLocator(application: Application) {
    val appPreferences: AppPreferences = DataStoreAppPreferences(application)
    val chatStore: ChatStore = DataStoreChatStore(application)
    val chatRepository: ChatRepository = ChatRepository(chatStore)
    val settingsRepository: SettingsRepository = SettingsRepository(appPreferences)

    private val modelCatalog = ModelCatalog(
        File(application.filesDir, ModelCatalog.MODELS_DIR_NAME),
    )

    /**
     * 기기에 배치된 로컬 모델(.task/.bin/.gguf)이 있으면 MediaPipe LLM Inference로 구동한다.
     * 없으면 개발용 EchoModel로 폴백한다 (docs/model-setup.md).
     */
    val chatModel: ChatModel = modelCatalog.findModelFile()
        ?.let { MediaPipeModel(application, it.absolutePath) }
        ?: EchoModel()

    val conversationEngine: ConversationEngine = ConversationEngine(chatModel)
}

class OnDeviceAiApplication : Application() {
    lateinit var locator: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        locator = ServiceLocator(this)
    }
}
