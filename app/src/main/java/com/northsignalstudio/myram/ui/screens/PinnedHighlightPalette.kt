package com.northsignalstudio.myram.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance

enum class PinnedHighlightColor(
    val preferenceValue: String,
    val label: String,
    val highlight: Color
) {
    Yellow("yellow", "Yellow", Color(0xFFFAB942)),
    Mint("mint", "Mint", Color(0xFFAEE8C5)),
    Blue("blue", "Blue", Color(0xFF9DC7F5)),
    Purple("purple", "Purple", Color(0xFFC9B5F4)),
    Slate("slate", "Slate", Color(0xFF4C566A));

    companion object {
        fun fromPreferenceValue(value: String?): PinnedHighlightColor =
            entries.firstOrNull { it.preferenceValue == value } ?: Yellow
    }
}

internal object PinnedHighlightPalette {
    val Highlight = Color(0xFFFAB942)
    val Text = textFor(PinnedHighlightColor.Yellow)
    val PlaceholderText = placeholderTextFor(PinnedHighlightColor.Yellow)

    fun highlightFor(color: PinnedHighlightColor): Color = color.highlight

    fun textFor(color: PinnedHighlightColor): Color {
        val darkText = Color(0xFF1C1C1E)
        val lightText = Color(0xFFFFFFFF)
        return if (contrastRatio(darkText, color.highlight) >= contrastRatio(lightText, color.highlight)) {
            darkText
        } else {
            lightText
        }
    }

    fun placeholderTextFor(color: PinnedHighlightColor): Color = textFor(color).copy(alpha = 0.68f)

    fun shineBrush(highlight: Color): Brush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.18f),
            highlight.copy(alpha = 0.04f),
            Color.White.copy(alpha = 0.10f),
            Color.Black.copy(alpha = 0.04f)
        )
    )

    private fun contrastRatio(foreground: Color, background: Color): Float {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
