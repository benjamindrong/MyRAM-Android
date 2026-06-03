package com.northsignalstudio.myram.debug

import com.northsignalstudio.myram.data.dao.NoteDao

object DebugDemoDataGenerator {
    const val isAvailable: Boolean = false
    val demoNoteIds: Set<Int> = emptySet()

    suspend fun generateDemoNotes(noteDao: NoteDao) = Unit

    suspend fun clearDemoNotes(noteDao: NoteDao) = Unit
}
