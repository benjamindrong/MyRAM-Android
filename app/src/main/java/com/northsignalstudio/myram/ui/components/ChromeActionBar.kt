package com.northsignalstudio.myram.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.northsignalstudio.myram.ui.theme.EditorChromeStyle
import com.northsignalstudio.myram.ui.theme.md_theme_dark_surface
import com.northsignalstudio.myram.ui.theme.md_theme_dark_toolbarBackground

@Composable
fun ChromeActionBar(
    style: EditorChromeStyle,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val isDarkAppearance = MaterialTheme.colorScheme.background == md_theme_dark_surface
    val fillBrush = when (style) {
        EditorChromeStyle.ChromeAccent -> chromeAccentBrush(MaterialTheme.colorScheme.background)
        EditorChromeStyle.WarmPaper -> SolidColor(MaterialTheme.colorScheme.surfaceVariant)
        EditorChromeStyle.Standard -> SolidColor(if (isDarkAppearance) md_theme_dark_toolbarBackground else style.toolbarColor)
    }
    val strokeColor = when (style) {
        EditorChromeStyle.ChromeAccent -> chromeAccentStroke(MaterialTheme.colorScheme.background)
        EditorChromeStyle.WarmPaper -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        EditorChromeStyle.Standard -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    }

    Row(
        modifier = modifier
            .clip(shape)
            .background(brush = fillBrush)
            .border(width = 1.dp, color = strokeColor, shape = shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        content = content
    )
}

private fun chromeAccentBrush(backgroundColor: Color): Brush {
    val isDark = backgroundColor.red + backgroundColor.green + backgroundColor.blue < 1.5f
    return if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF575B62),
                Color(0xFF33353B),
                Color(0xFF4E5158)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFF2F2F4),
                Color(0xFFD3D5DA),
                Color(0xFFE9EAEE)
            )
        )
    }
}

private fun chromeAccentStroke(backgroundColor: Color): Color {
    val isDark = backgroundColor.red + backgroundColor.green + backgroundColor.blue < 1.5f
    return Color.White.copy(alpha = if (isDark) 0.22f else 0.44f)
}
