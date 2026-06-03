package com.northsignalstudio.myram

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.northsignalstudio.myram.ui.screens.copySelectedText
import com.northsignalstudio.myram.ui.screens.cutSelectedText
import com.northsignalstudio.myram.ui.screens.pasteIntoSelection
import com.northsignalstudio.myram.ui.screens.selectAllText
import com.northsignalstudio.myram.ui.screens.toggleSelectAllText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditorTextActionsTest {

    @Test
    fun copySelectedText_returnsSelectedPortion() {
        val value = TextFieldValue("Hello World", selection = TextRange(0, 5))

        val copied = copySelectedText(value)

        assertEquals("Hello", copied)
    }

    @Test
    fun copySelectedText_returnsNullWhenNoSelection() {
        val value = TextFieldValue("Hello", selection = TextRange(2, 2))

        val copied = copySelectedText(value)

        assertNull(copied)
    }

    @Test
    fun cutSelectedText_removesSelectionAndReturnsCutText() {
        val value = TextFieldValue("Hello World", selection = TextRange(6, 11))

        val (updated, cutText) = cutSelectedText(value)

        assertEquals("World", cutText)
        assertEquals("Hello ", updated.text)
        assertEquals(TextRange(6), updated.selection)
    }

    @Test
    fun pasteIntoSelection_replacesSelectionAndMovesCursorToEnd() {
        val value = TextFieldValue("Hello World", selection = TextRange(6, 11))

        val updated = pasteIntoSelection(value, "Android")

        assertEquals("Hello Android", updated.text)
        assertEquals(TextRange(13), updated.selection)
    }

    @Test
    fun selectAllText_selectsFullTextRange() {
        val value = TextFieldValue("Compose")

        val updated = selectAllText(value)

        assertEquals(TextRange(0, 7), updated.selection)
    }

    @Test
    fun toggleSelectAllText_selectsAllWhenNotFullySelected() {
        val value = TextFieldValue("Compose", selection = TextRange(2))

        val updated = toggleSelectAllText(value)

        assertEquals(TextRange(0, 7), updated.selection)
    }

    @Test
    fun toggleSelectAllText_collapsesSelectionWhenFullySelected() {
        val value = TextFieldValue("Compose", selection = TextRange(0, 7))

        val updated = toggleSelectAllText(value)

        assertEquals(TextRange(7), updated.selection)
    }
}
