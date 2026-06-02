package com.apexcoretechs.myram

import com.apexcoretechs.myram.data.PinnedText
import com.apexcoretechs.myram.ui.sortedPinnedText
import org.junit.Assert.assertEquals
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
}
