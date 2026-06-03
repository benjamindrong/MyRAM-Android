package com.northsignalstudio.myram.ui.theme

enum class EditorChromeStyle(val preferenceValue: String, val label: String) {
    Standard("standard", "Standard"),
    ChromeAccent("chrome_accent", "Chrome Accent");

    companion object {
        fun fromPreferenceValue(value: String?): EditorChromeStyle =
            entries.firstOrNull { it.preferenceValue == value } ?: Standard
    }
}
