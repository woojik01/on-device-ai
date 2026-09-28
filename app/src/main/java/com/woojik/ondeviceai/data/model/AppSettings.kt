package com.woojik.ondeviceai.data.model

import kotlinx.serialization.Serializable

/** 앱 기본 상태. 캐릭터 이름과 테마, 로컬 모델 백엔드 등을 포함. */
@Serializable
data class AppSettings(
    val characterName: String = "아라",
    val darkThemeMode: DarkThemeMode = DarkThemeMode.FOLLOW_SYSTEM,
    val modelBackend: ModelBackend = ModelBackend.CPU,
)

/** 다크 모드 설정. FOLLOW_SYSTEM이 기본값이다. */
enum class DarkThemeMode { FOLLOW_SYSTEM, LIGHT, DARK }

/** 로컬 모델(LiteRT-LM .litertlm) 실행 백엔드. */
enum class ModelBackend { CPU, GPU }
