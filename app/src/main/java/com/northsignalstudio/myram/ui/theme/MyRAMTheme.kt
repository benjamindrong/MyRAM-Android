package com.northsignalstudio.myram.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    background = md_theme_light_surface,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = Color(0xFFE1E6EE),
    onSurfaceVariant = Color(0xFF3F4854),
    primaryContainer = Color(0xFFD4DEFF),
    outline = md_theme_light_outline
)

private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    background = md_theme_dark_surface,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = Color(0xFF303236),
    onSurfaceVariant = Color(0xFFD0D2D6),
    primaryContainer = Color(0xFF374374),
    outline = md_theme_dark_outline
)

private val WarmPaperColors = lightColorScheme(
    primary = warm_paper_primary,
    onPrimary = warm_paper_onPrimary,
    background = warm_paper_background,
    surface = warm_paper_surface,
    onSurface = warm_paper_onSurface,
    surfaceVariant = warm_paper_surfaceVariant,
    onSurfaceVariant = warm_paper_onSurfaceVariant,
    primaryContainer = warm_paper_primaryContainer,
    outline = warm_paper_outline
)

private val WarmPaperDarkColors = darkColorScheme(
    primary = warm_paper_dark_primary,
    onPrimary = warm_paper_dark_onPrimary,
    background = warm_paper_dark_background,
    surface = warm_paper_dark_surface,
    onSurface = warm_paper_dark_onSurface,
    surfaceVariant = warm_paper_dark_surfaceVariant,
    onSurfaceVariant = warm_paper_dark_onSurfaceVariant,
    primaryContainer = warm_paper_dark_primaryContainer,
    outline = warm_paper_dark_outline
)

@Composable
fun MyRAMTheme(
    appearanceSetting: AppearanceSetting = AppearanceSetting.System,
    editorChromeStyle: EditorChromeStyle = EditorChromeStyle.Standard,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appearanceSetting) {
        AppearanceSetting.System -> isSystemInDarkTheme()
        AppearanceSetting.Light -> false
        AppearanceSetting.Dark -> true
    }
    val colors = when {
        editorChromeStyle.isWarmPaper && darkTheme -> WarmPaperDarkColors
        editorChromeStyle.isWarmPaper -> WarmPaperColors
        darkTheme -> DarkColors
        else -> LightColors
    }

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
