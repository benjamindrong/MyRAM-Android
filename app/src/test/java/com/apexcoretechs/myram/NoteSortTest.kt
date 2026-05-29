package com.apexcoretechs.myram

import com.apexcoretechs.myram.data.Note
import com.apexcoretechs.myram.ui.sortNotesForDisplay
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteSortTest {

    @Test
    fun sortNotesForDisplay_placesPinnedNotesFirst() {
        val notes = listOf(
            Note(id = 1, title = "A", isPinned = false, lastModified = 200L, createdAt = 100L),
            Note(id = 2, title = "B", isPinned = true, lastModified = 150L, createdAt = 100L),
            Note(id = 3, title = "C", isPinned = false, lastModified = 300L, createdAt = 100L)
        )

        val sorted = sortNotesForDisplay(notes)

        assertEquals(listOf(2, 3, 1), sorted.map { it.id })
    }

    @Test
    fun sortNotesForDisplay_sortsByLastModifiedWithinSamePinState() {
        val notes = listOf(
            Note(id = 1, title = "A", isPinned = true, lastModified = 200L, createdAt = 100L),
            Note(id = 2, title = "B", isPinned = true, lastModified = 450L, createdAt = 100L),
            Note(id = 3, title = "C", isPinned = true, lastModified = 300L, createdAt = 100L)
        )

        val sorted = sortNotesForDisplay(notes)

        assertEquals(listOf(2, 3, 1), sorted.map { it.id })
    }
}
