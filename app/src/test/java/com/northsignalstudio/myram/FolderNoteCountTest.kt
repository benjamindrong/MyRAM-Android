package com.northsignalstudio.myram

import com.northsignalstudio.myram.data.Folder
import com.northsignalstudio.myram.data.Note
import com.northsignalstudio.myram.ui.computeFolderActiveNoteCounts
import org.junit.Assert.assertEquals
import org.junit.Test

class FolderNoteCountTest {

    @Test
    fun computeFolderActiveNoteCounts_excludesDeletedAndOtherFolders() {
        val folderA = Folder(id = 1, name = "A")
        val folderB = Folder(id = 2, name = "B")
        val folders = listOf(folderA, folderB)
        val notes = listOf(
            Note(id = 1, folderId = 1, deletedAt = null),
            Note(id = 2, folderId = 1, deletedAt = 1000L),
            Note(id = 3, folderId = 2, deletedAt = null),
            Note(id = 4, folderId = null, deletedAt = null)
        )

        val counts = computeFolderActiveNoteCounts(folders = folders, notes = notes)

        assertEquals(1, counts[1])
        assertEquals(1, counts[2])
    }

    @Test
    fun computeFolderActiveNoteCounts_includesNestedFolderNotes() {
        val parent = Folder(id = 1, name = "Parent")
        val child = Folder(id = 2, name = "Child", parentFolderId = 1)
        val grandchild = Folder(id = 3, name = "Grandchild", parentFolderId = 2)
        val folders = listOf(parent, child, grandchild)
        val notes = listOf(
            Note(id = 1, folderId = 2, deletedAt = null),
            Note(id = 2, folderId = 3, deletedAt = null),
            Note(id = 3, folderId = 3, deletedAt = 2000L)
        )

        val counts = computeFolderActiveNoteCounts(folders = folders, notes = notes)

        assertEquals(2, counts[1])
        assertEquals(2, counts[2])
        assertEquals(1, counts[3])
    }
}
