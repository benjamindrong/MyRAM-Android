package com.northsignalstudio.myram

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.northsignalstudio.myram.ui.screens.PinnedHighlightPalette
import com.northsignalstudio.myram.ui.theme.AppearanceSetting
import com.northsignalstudio.myram.ui.theme.EditorChromeStyle
import com.northsignalstudio.myram.ui.theme.md_theme_dark_editorEntryBackground
import com.northsignalstudio.myram.ui.theme.md_theme_dark_toolbarBackground
import com.northsignalstudio.myram.ui.theme.md_theme_light_editorEntryBackground
import com.northsignalstudio.myram.ui.theme.warm_paper_background
import com.northsignalstudio.myram.ui.theme.warm_paper_dark_background
import com.northsignalstudio.myram.ui.theme.warm_paper_dark_onSurface
import com.northsignalstudio.myram.ui.theme.warm_paper_dark_surface
import com.northsignalstudio.myram.ui.theme.warm_paper_dark_toolbar
import com.northsignalstudio.myram.ui.theme.warm_paper_onSurface
import com.northsignalstudio.myram.ui.theme.warm_paper_surface
import com.northsignalstudio.myram.ui.theme.warm_paper_toolbar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinnedHighlightPaletteTest {

    @Test
    fun pinnedHighlightTextColor_hasReadableContrast() {
        assertEquals(Color(0xFFFAB942), PinnedHighlightPalette.Highlight)
        assertTrue(
            contrastRatio(PinnedHighlightPalette.Text, PinnedHighlightPalette.Highlight) > 4.5f
        )
    }

    @Test
    fun warmPaper_isAChromeStyleWithReadablePalette() {
        assertTrue(AppearanceSetting.entries.none { it.label == "Warm Paper" })
        assertTrue(EditorChromeStyle.entries.none { it.label == "Standard" })
        assertEquals("None", EditorChromeStyle.Standard.label)
        assertEquals("Warm Paper", EditorChromeStyle.WarmPaper.label)
        assertEquals(EditorChromeStyle.WarmPaper, EditorChromeStyle.fromPreferenceValue("warm_paper"))
        assertTrue(EditorChromeStyle.WarmPaper.isWarmPaper)
        assertEquals(Color.White, EditorChromeStyle.Standard.toolbarColor)
        assertEquals(Color(0xFFF5F6F9), md_theme_light_editorEntryBackground)
        assertEquals(Color(0xFF2C2C2E), md_theme_dark_editorEntryBackground)
        assertEquals(Color(0xFF2C2C2E), md_theme_dark_toolbarBackground)
        assertEquals(Color(0xFFFFFAF1), warm_paper_surface)
        assertTrue(contrastRatio(warm_paper_onSurface, warm_paper_surface) > 4.5f)
        assertTrue(contrastRatio(warm_paper_toolbar, warm_paper_background) > 1.1f)
        assertTrue(contrastRatio(warm_paper_dark_onSurface, warm_paper_dark_surface) > 4.5f)
        assertTrue(contrastRatio(warm_paper_dark_toolbar, warm_paper_dark_background) > 1.1f)
    }

    private fun contrastRatio(foreground: Color, background: Color): Float {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
