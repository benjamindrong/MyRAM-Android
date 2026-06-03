package com.apexcoretechs.myram.debug

import com.apexcoretechs.myram.data.dao.NoteDao

object DebugDemoDataGenerator {
    const val isAvailable: Boolean = false
    val demoNoteIds: Set<Int> = emptySet()

    suspend fun generateDemoNotes(noteDao: NoteDao) = Unit

    suspend fun clearDemoNotes(noteDao: NoteDao) = Unit
}
