package com.apexcoretechs.myram.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apexcoretechs.myram.data.Note
import com.apexcoretechs.myram.data.Repository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {
    val repo = Repository.get(getApplication())

    val allNotes = repo.noteDao.getAll().stateIn(
        viewModelScope, SharingStarted.Lazily, emptyList()
    )

    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote = _currentNote.asStateFlow()

    // Save last opened note
    private val prefs = app.getSharedPreferences("myram_prefs", Application.MODE_PRIVATE)

    init {
        // Load last note on startup
        val lastNoteId = prefs.getInt("last_note_id", -1)
        if (lastNoteId != -1) {
            viewModelScope.launch {
                repo.noteDao.getById(lastNoteId).collect { note ->
                    _currentNote.value = note
                }
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
        repo.noteDao.update(note.copy(lastModified = System.currentTimeMillis()))
    }

    fun deleteNote(note: Note) = viewModelScope.launch {
        repo.noteDao.delete(note)
        if (_currentNote.value?.id == note.id) {
            _currentNote.value = null
        }
    }
}
