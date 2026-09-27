package com.woojik.ondeviceai.data.local

import com.woojik.ondeviceai.data.model.AppSettings
import kotlinx.coroutines.flow.Flow

/** 앱 상태 저장 계층 추상화. 설정/테마 등 기본 상태를 보관한다. */
interface AppPreferences {
    fun observeSettings(): Flow<AppSettings>
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings)
}
