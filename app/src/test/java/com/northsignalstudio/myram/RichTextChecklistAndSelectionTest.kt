package com.northsignalstudio.myram

import com.northsignalstudio.myram.ui.richtext.CHECKLIST_CHECKED_PREFIX
import com.northsignalstudio.myram.ui.richtext.CHECKLIST_UNCHECKED_PREFIX
import com.northsignalstudio.myram.ui.richtext.checkedChecklistContentRanges
import com.northsignalstudio.myram.ui.richtext.isChecklistIconAtOffset
import com.northsignalstudio.myram.ui.richtext.toggleChecklistInText
import com.northsignalstudio.myram.ui.richtext.toggleSelectAllRange
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
}

