package com.northsignalstudio.myram

import android.content.ClipData
import android.graphics.Typeface
import android.text.Spanned
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import com.northsignalstudio.myram.ui.richtext.firstPlainText
import com.northsignalstudio.myram.ui.richtext.firstStyledText
import com.northsignalstudio.myram.ui.richtext.pastePlainTextMatchingDestinationFormatting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RichTextPasteFormattingTest {
    @Test
    fun firstPlainText_readsClipboardTextInsteadOfClipItemDebugString() {
        val clip = ClipData.newPlainText("text", "Copied note text")

        val pastedText = clip.firstPlainText()

        assertEquals("Copied note text", pastedText)
    }

    @Test
    fun firstStyledText_preservesSpannedClipboardTextForNormalPaste() {
        val source = SpannableStringBuilder("Styled")
        source.setSpan(StyleSpan(Typeface.BOLD), 0, source.length, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        val clip = ClipData.newPlainText("text", source)

        val pastedText = clip.firstStyledText()

        assertTrue(pastedText is Spanned)
        val pastedSpans = (pastedText as Spanned).getSpans(0, pastedText.length, StyleSpan::class.java)
        assertTrue(pastedSpans.any { it.style == Typeface.BOLD })
    }

    @Test
    fun pastePlainTextMatchingDestinationFormatting_appliesDestinationSpansToPastedText() {
        val editable = SpannableStringBuilder("Hello World")
        editable.setSpan(StyleSpan(Typeface.BOLD), 6, 11, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        editable.setSpan(UnderlineSpan(), 6, 11, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        editable.setSpan(ForegroundColorSpan(0xff3366cc.toInt()), 6, 11, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        editable.setSpan(AbsoluteSizeSpan(22, true), 6, 11, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)

        val cursor = pastePlainTextMatchingDestinationFormatting(
            editable = editable,
            selectionStart = 6,
            selectionEnd = 11,
            pastedText = "Android"
        )

        assertEquals("Hello Android", editable.toString())
        assertEquals(13, cursor)
        assertTrue(editable.getSpans(6, 13, StyleSpan::class.java).any { it.style == Typeface.BOLD })
        assertTrue(editable.getSpans(6, 13, UnderlineSpan::class.java).isNotEmpty())
        assertEquals(
            0xff3366cc.toInt(),
            editable.getSpans(6, 13, ForegroundColorSpan::class.java).last().foregroundColor
        )
        assertEquals(22, editable.getSpans(6, 13, AbsoluteSizeSpan::class.java).last().size)
    }
}
