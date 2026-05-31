package com.apexcoretechs.myram.ui.components

import androidx.compose.foundation.background
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
import com.apexcoretechs.myram.ui.theme.EditorChromeStyle

@Composable
fun ChromeActionBar(
    style: EditorChromeStyle,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = if (style == EditorChromeStyle.ChromeAccent) {
                    chromeAccentBrush(MaterialTheme.colorScheme.background)
                } else {
                    SolidColor(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                }
            )
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
