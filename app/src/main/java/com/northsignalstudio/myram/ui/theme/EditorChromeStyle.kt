package com.northsignalstudio.myram.ui.theme

import androidx.compose.ui.graphics.Color

enum class EditorChromeStyle(val preferenceValue: String, val label: String) {
    Standard("standard", "None"),
    ChromeAccent("chrome_accent", "Chrome Accent"),
    WarmPaper("warm_paper", "Warm Paper");

    val isWarmPaper: Boolean
        get() = this == WarmPaper

    val toolbarColor: Color
        get() = if (isWarmPaper) warm_paper_toolbar else Color(0xFFFFFFFF)

    val toolbarStrokeColor: Color
        get() = if (isWarmPaper) warm_paper_outline.copy(alpha = 0.34f) else md_theme_light_outline.copy(alpha = 0.82f)

    companion object {
        fun fromPreferenceValue(value: String?): EditorChromeStyle =
            entries.firstOrNull { it.preferenceValue == value } ?: Standard
    }
}
