package com.woojik.ondeviceai.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.woojik.ondeviceai.ServiceLocator
import com.woojik.ondeviceai.data.model.AppSettings
import com.woojik.ondeviceai.ui.chat.ChatScreen
import com.woojik.ondeviceai.ui.settings.SettingsScreen
import com.woojik.ondeviceai.ui.theme.OnDeviceAiTheme

object Routes {
    const val CHAT = "chat"
    const val SETTINGS = "settings"
}

/**
 * 앱 전체 화면 구성.
 * 테마는 설정 저장소(DarkThemeMode)에 따라 적용되고,
 * 이후 AI 기능 연결 시에도 이 라우팅 구조는 유지된다.
 */
@Composable
fun OnDeviceAiApp(locator: ServiceLocator) {
    val settings by locator.settingsRepository.observeSettings()
        .collectAsStateWithLifecycle(initialValue = AppSettings())

    OnDeviceAiTheme(darkThemeMode = settings.darkThemeMode) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = Routes.CHAT) {
            composable(Routes.CHAT) {
                ChatScreen(
                    characterName = settings.characterName,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
