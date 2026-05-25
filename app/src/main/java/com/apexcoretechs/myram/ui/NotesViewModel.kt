package com.apexcoretechs.myram.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apexcoretechs.myram.data.Note
import com.apexcoretechs.myram.data.Repository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val recentlyDeletedRetentionMillis = 7L * 24 * 60 * 60 * 1000
    val repo = Repository.get(getApplication())

    val allNotes = repo.noteDao.getAll().stateIn(
        viewModelScope, SharingStarted.Lazily, emptyList()
    )

    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote = _currentNote.asStateFlow()

    private val _recentlyDeletedNotes = MutableStateFlow<List<Note>>(emptyList())
    val recentlyDeletedNotes = _recentlyDeletedNotes.asStateFlow()

    // Save last opened note
    private val prefs = app.getSharedPreferences("myram_prefs", Application.MODE_PRIVATE)

    init {
        viewModelScope.launch {
            purgeExpiredDeletedNotes()
        }

        // Load last note on startup
        val lastNoteId = prefs.getInt("last_note_id", -1)
        if (lastNoteId != -1) {
            viewModelScope.launch {
                _currentNote.value = repo.noteDao.getById(lastNoteId).first()
            }
        }
    }

    fun selectNote(note: Note?) {
        _currentNote.value = note
        note?.let {
            prefs.edit().putInt("last_note_id", it.id).apply()
        }
    }

    fun createNote(
        title: String? = null,
        content: String? = null,
        onCreated: (Note) -> Unit = {}
    ) = viewModelScope.launch {
        val newNote = Note(
            title = title?.takeIf { it.isNotBlank() } ?: "",
            content = content?.takeIf { it.isNotBlank() } ?: ""
        )
        val id = repo.noteDao.insert(newNote)
        val created = newNote.copy(id = id.toInt())
        selectNote(created)
        onCreated(created)
    }

    fun updateNote(note: Note) = viewModelScope.launch {
        if (note.deletedAt != null) return@launch
        repo.noteDao.update(note.copy(lastModified = System.currentTimeMillis()))
    }

    fun deleteNote(note: Note) = viewModelScope.launch {
        repo.noteDao.update(
            note.copy(
                lastModified = System.currentTimeMillis(),
                deletedAt = System.currentTimeMillis()
            )
        )
        if (_currentNote.value?.id == note.id) {
            _currentNote.value = null
            prefs.edit().remove("last_note_id").apply()
        }
        refreshRecentlyDeletedNotes()
    }

    fun refreshRecentlyDeletedNotes() = viewModelScope.launch {
        purgeExpiredDeletedNotes()
        _recentlyDeletedNotes.value = repo.noteDao.getRecentlyDeleted()
    }

    fun restoreNote(note: Note) = viewModelScope.launch {
        repo.noteDao.update(
            note.copy(
                lastModified = System.currentTimeMillis(),
                deletedAt = null
            )
        )
        refreshRecentlyDeletedNotes()
    }

    fun permanentlyDeleteNote(note: Note) = viewModelScope.launch {
        repo.noteDao.delete(note)
        if (_currentNote.value?.id == note.id) {
            _currentNote.value = null
            prefs.edit().remove("last_note_id").apply()
        }
        refreshRecentlyDeletedNotes()
    }

    private suspend fun purgeExpiredDeletedNotes() {
        val cutoff = System.currentTimeMillis() - recentlyDeletedRetentionMillis
        repo.noteDao.purgeDeletedBefore(cutoff)
    }
}
