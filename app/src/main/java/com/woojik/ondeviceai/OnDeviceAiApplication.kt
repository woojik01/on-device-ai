package com.woojik.ondeviceai

import android.app.Application
import com.woojik.ondeviceai.data.local.AppPreferences
import com.woojik.ondeviceai.data.local.DataStoreAppPreferences
import com.woojik.ondeviceai.data.local.ChatStore
import com.woojik.ondeviceai.data.local.DataStoreChatStore
import com.woojik.ondeviceai.data.repository.ChatRepository
import com.woojik.ondeviceai.data.repository.SettingsRepository
import com.woojik.ondeviceai.domain.chat.ChatModel
import com.woojik.ondeviceai.domain.chat.ConversationEngine
import com.woojik.ondeviceai.domain.chat.EchoModel

/**
 * 간단한 수동 의존성 컨테이너.
 * 로컬 저장 계층과 ChatModel은 인터페이스로 추상화되어 있어
 * 이후 SQLite/로컬 LLM 도입 시 구현체만 교체하면 된다.
 */
class ServiceLocator(application: Application) {
    val appPreferences: AppPreferences = DataStoreAppPreferences(application)
    val chatStore: ChatStore = DataStoreChatStore(application)
    val chatRepository: ChatRepository = ChatRepository(chatStore)
    val settingsRepository: SettingsRepository = SettingsRepository(appPreferences)

    /** 개발용 임시 모델 — 벤치마크 후 로컬 LLM 구현체로 교체한다. */
    val chatModel: ChatModel = EchoModel()
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
