package com.apexcoretechs.myram.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    outline = md_theme_light_outline
)

private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    outline = md_theme_dark_outline
)

@Composable
fun MyRAMTheme(
    appearanceSetting: AppearanceSetting = AppearanceSetting.System,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appearanceSetting) {
        AppearanceSetting.System -> isSystemInDarkTheme()
        AppearanceSetting.Light -> false
        AppearanceSetting.Dark -> true
    }
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}

enum class AppearanceSetting(val preferenceValue: String, val label: String) {
    System("system", "System"),
    Light("light", "Light"),
    Dark("dark", "Dark");

    companion object {
        fun fromPreferenceValue(value: String?): AppearanceSetting =
            entries.firstOrNull { it.preferenceValue == value } ?: System
    }
}
