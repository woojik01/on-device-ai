package com.woojik.ondeviceai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.woojik.ondeviceai.data.model.DarkThemeMode

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    error = Error,
    onError = OnError,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
)

val LocalDarkThemeMode = staticCompositionLocalOf { DarkThemeMode.FOLLOW_SYSTEM }

@Composable
fun OnDeviceAiTheme(
    darkThemeMode: DarkThemeMode = DarkThemeMode.FOLLOW_SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDark = when (darkThemeMode) {
        DarkThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        DarkThemeMode.LIGHT -> false
        DarkThemeMode.DARK -> true
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalDarkThemeMode provides darkThemeMode) {
        MaterialTheme(
            colorScheme = if (useDark) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}
