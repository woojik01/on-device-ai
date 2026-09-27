package com.woojik.ondeviceai.data.model

import kotlinx.serialization.Serializable

/** 앱 기본 상태. 캐릭터 이름과 테마 설정 등 PRD-01 범위의 기본 상태만 포함. */
@Serializable
data class AppSettings(
    val characterName: String = "아라",
    val darkThemeMode: DarkThemeMode = DarkThemeMode.FOLLOW_SYSTEM,
)

/** 다크 모드 설정. FOLLOW_SYSTEM이 기본값이다. */
enum class DarkThemeMode { FOLLOW_SYSTEM, LIGHT, DARK }
