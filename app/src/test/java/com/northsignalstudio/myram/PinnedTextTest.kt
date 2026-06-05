package com.northsignalstudio.myram

import com.northsignalstudio.myram.data.PinnedText
import com.northsignalstudio.myram.ui.PinnedTextExpansionSessionState
import com.northsignalstudio.myram.ui.sortedPinnedText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinnedTextTest {

    @Test
    fun sortedPinnedText_usesOrderThenCreatedAt() {
        val pinnedText = listOf(
            PinnedText(id = 3, noteId = 1, text = "Third", sortOrder = 2, createdAt = 1_000L),
            PinnedText(id = 2, noteId = 1, text = "Second", sortOrder = 1, createdAt = 2_000L),
            PinnedText(id = 1, noteId = 1, text = "First", sortOrder = 1, createdAt = 1_000L)
        )

        val sorted = pinnedText.sortedPinnedText()

        assertEquals(listOf("First", "Second", "Third"), sorted.map { it.text })
    }

    @Test
    fun pinnedTextExpansionSessionState_defaultsCollapsedAndRemembersPerNote() {
        val state = PinnedTextExpansionSessionState()

        assertFalse(state.isExpanded(1))
        assertFalse(state.isExpanded(2))

        state.setExpanded(1, true)

        assertTrue(state.isExpanded(1))
        assertFalse(state.isExpanded(2))

        state.setExpanded(1, false)

        assertFalse(state.isExpanded(1))
    }
}
