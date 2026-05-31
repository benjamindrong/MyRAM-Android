package com.apexcoretechs.myram

import com.apexcoretechs.myram.ui.richtext.isRichTextContent
import com.apexcoretechs.myram.ui.richtext.plainTextFromStoredContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RichTextContentTest {
    @Test
    fun isRichTextContent_returnsFalse_forPlainText() {
        assertFalse(isRichTextContent("Just text"))
    }

    @Test
    fun plainTextFromStoredContent_returnsInput_forPlainText() {
        val text = "Line one\nLine two"
        assertEquals(text, plainTextFromStoredContent(text))
    }
}
