package com.northsignalstudio.myram

import com.northsignalstudio.myram.ui.richtext.CHECKLIST_CHECKED_PREFIX
import com.northsignalstudio.myram.ui.richtext.CHECKLIST_UNCHECKED_PREFIX
import com.northsignalstudio.myram.ui.richtext.checkedChecklistContentRanges
import com.northsignalstudio.myram.ui.richtext.checklistIconContentOffset
import com.northsignalstudio.myram.ui.richtext.checklistIconRangeContainingOffset
import com.northsignalstudio.myram.ui.richtext.checklistIconLayoutY
import com.northsignalstudio.myram.ui.richtext.isChecklistIconAtOffset
import com.northsignalstudio.myram.ui.richtext.pinCandidateInText
import com.northsignalstudio.myram.ui.richtext.toggleChecklistInText
import com.northsignalstudio.myram.ui.richtext.toggleSelectAllRange
import android.graphics.Paint
import com.northsignalstudio.myram.ui.richtext.ParagraphSpacingSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextChecklistAndSelectionTest {

    @Test
    fun toggleChecklistInText_createsUncheckedItemAtCurrentLine() {
        val updated = toggleChecklistInText(
            text = "Buy milk",
            selectionStart = 0,
            selectionEnd = 0
        )

        assertEquals("$CHECKLIST_UNCHECKED_PREFIX" + "Buy milk", updated.text)
        assertEquals(CHECKLIST_UNCHECKED_PREFIX.length, updated.selectionStart)
        assertEquals(updated.selectionStart, updated.selectionEnd)
    }

    @Test
    fun toggleChecklistInText_togglesUncheckedAndCheckedMarkers() {
        val first = toggleChecklistInText(
            text = "${CHECKLIST_UNCHECKED_PREFIX}Buy milk",
            selectionStart = 0,
            selectionEnd = 0
        )
        assertEquals("${CHECKLIST_CHECKED_PREFIX}Buy milk", first.text)

        val second = toggleChecklistInText(
            text = first.text,
            selectionStart = 0,
            selectionEnd = 0
        )
        assertEquals("${CHECKLIST_UNCHECKED_PREFIX}Buy milk", second.text)
    }

    @Test
    fun toggleChecklistInText_convertsLegacyMarkersToIconMarkers() {
        val updated = toggleChecklistInText(
            text = "- [x] Done",
            selectionStart = 0,
            selectionEnd = 0
        )

        assertEquals("${CHECKLIST_UNCHECKED_PREFIX}Done", updated.text)
    }

    @Test
    fun checkedChecklistContentRanges_returnsCheckedContentOnly() {
        val ranges = checkedChecklistContentRanges("${CHECKLIST_CHECKED_PREFIX}Done\n${CHECKLIST_UNCHECKED_PREFIX}Pending")

        assertEquals(1, ranges.size)
        assertEquals(CHECKLIST_CHECKED_PREFIX.length, ranges.first().start)
        assertEquals("${CHECKLIST_CHECKED_PREFIX}Done".length, ranges.first().end)
    }

    @Test
    fun isChecklistIconAtOffset_detectsOnlyPrefixOffsets() {
        val text = "${CHECKLIST_UNCHECKED_PREFIX}Task"

        assertTrue(isChecklistIconAtOffset(text, 0))
        assertTrue(isChecklistIconAtOffset(text, 1))
        assertFalse(isChecklistIconAtOffset(text, CHECKLIST_UNCHECKED_PREFIX.length))
    }

    @Test
    fun checklistIconRangeContainingOffset_returnsPrefixForContainingLine() {
        val text = "Before\n${CHECKLIST_UNCHECKED_PREFIX}Task\nAfter"
        val offset = text.indexOf("Task")

        val range = checklistIconRangeContainingOffset(text, offset)

        requireNotNull(range)
        assertEquals(CHECKLIST_UNCHECKED_PREFIX, text.substring(range.start, range.end))
    }

    @Test
    fun toggleSelectAllRange_selectsAllWhenSelectionIsInvalid() {
        val selection = toggleSelectAllRange(length = 8, selectionStart = -1, selectionEnd = -1)

        assertEquals(0, selection.start)
        assertEquals(8, selection.end)
    }

    @Test
    fun toggleSelectAllRange_collapsesSelectionWhenAlreadyFullySelected() {
        val selection = toggleSelectAllRange(length = 8, selectionStart = 0, selectionEnd = 8)

        assertEquals(8, selection.start)
        assertEquals(8, selection.end)
    }

    @Test
    fun pinCandidateInText_ignoresSelectionAndUsesEntireCursorLine() {
        val text = "Before\nPin this entire line please\nAfter"
        val cursor = text.indexOf("entire")

        val candidate = pinCandidateInText(text, cursor)

        requireNotNull(candidate)
        assertEquals("Pin this entire line please", text.substring(candidate.textStart, candidate.textEnd))
        assertEquals("Pin this entire line please\n", text.substring(candidate.sourceStart, candidate.sourceEnd))
    }

    @Test
    fun pinCandidateInText_usesCursorLineWhenSelectionIsCollapsed() {
        val text = "Before\nPin this line from cursor\nAfter"
        val cursor = text.indexOf("from")

        val candidate = pinCandidateInText(text, cursor)

        requireNotNull(candidate)
        assertEquals("Pin this line from cursor", text.substring(candidate.textStart, candidate.textEnd))
        assertEquals("Pin this line from cursor\n", text.substring(candidate.sourceStart, candidate.sourceEnd))
    }

    @Test
    fun pinCandidateInText_stripsChecklistPrefixFromPinnedText() {
        val text = "Before\n${CHECKLIST_UNCHECKED_PREFIX}Follow up on pinned thought\nAfter"
        val cursor = text.indexOf("Follow")

        val candidate = pinCandidateInText(text, cursor)

        requireNotNull(candidate)
        assertEquals("Follow up on pinned thought", text.substring(candidate.textStart, candidate.textEnd))
        assertEquals(
            "${CHECKLIST_UNCHECKED_PREFIX}Follow up on pinned thought\n",
            text.substring(candidate.sourceStart, candidate.sourceEnd)
        )
    }

    @Test
    fun checklistIconLayoutY_matchesTextLayoutCoordinates() {
        val iconY = checklistIconLayoutY(
            totalPaddingTop = 12f,
            lineBaseline = 120f,
            alignmentOffset = -2f
        )

        assertEquals(130f, iconY, 0.001f)
    }

    @Test
    fun checklistIconContentOffset_usesFirstVisibleCharacterAfterPrefix() {
        val text = "${CHECKLIST_UNCHECKED_PREFIX}LongUnbrokenChecklistText"

        assertEquals(
            CHECKLIST_UNCHECKED_PREFIX.length,
            checklistIconContentOffset(CHECKLIST_UNCHECKED_PREFIX.length, text.length)
        )
    }

    @Test
    fun paragraphSpacingSpan_appliesSpacingAtParagraphEnd() {
        val spacingPx = 20
        val span = ParagraphSpacingSpan(spacingPx)
        val text = "Line 1\nLine 2"
        val fm = Paint.FontMetricsInt()
        fm.descent = 10
        fm.bottom = 15

        // Line 1 ends with \n at index 6
        span.chooseHeight(text, 0, 7, 0, 0, fm)

        assertEquals(30, fm.descent) // 10 + 20
        assertEquals(35, fm.bottom)  // 15 + 20
    }

    @Test
    fun paragraphSpacingSpan_appliesSpacingWhenFollowingCharIsNewline() {
        val spacingPx = 15
        val span = ParagraphSpacingSpan(spacingPx)
        val text = "Line 1\nLine 2"
        val fm = Paint.FontMetricsInt()
        fm.descent = 5

        // Layout for "Line 1" without the \n. If 'end' is 6 (just before \n), 
        // the new logic should still apply spacing because text[6] is \n.
        span.chooseHeight(text, 0, 6, 0, 0, fm)

        assertEquals(5 + 15, fm.descent)
    }

}
