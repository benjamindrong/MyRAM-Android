package com.northsignalstudio.myram

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.northsignalstudio.myram.ui.screens.PinnedHighlightPalette
import org.junit.Assert.assertTrue
import org.junit.Test

class PinnedHighlightPaletteTest {

    @Test
    fun pinnedHighlightTextColor_hasReadableContrast() {
        assertTrue(
            contrastRatio(PinnedHighlightPalette.Text, PinnedHighlightPalette.Highlight) > 4.5f
        )
    }

    private fun contrastRatio(foreground: Color, background: Color): Float {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
