package com.apexcoretechs.myram.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apexcoretechs.myram.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {
//    private val repo = Repository.get(app)

    // make repo public if needed
    val repo = Repository.get(getApplication())

    val folders = repo.folderDao.getAll().stateIn(
        viewModelScope, SharingStarted.Lazily, emptyList()
    )

    private val _selectedFolder = MutableStateFlow<Folder?>(null)
    val selectedFolder = _selectedFolder.asStateFlow()

    fun selectFolder(folder: Folder?) { _selectedFolder.value = folder }

    fun createFolder(name: String) = viewModelScope.launch {
        repo.folderDao.insert(Folder(name = name))
    }

    fun deleteFolder(folder: Folder) = viewModelScope.launch {
        repo.folderDao.delete(folder)
    }

    fun createNote(title: String, content: String) = viewModelScope.launch {
        selectedFolder.value?.let {
            repo.noteDao.insert(Note(folderId = it.id, title = title, content = content))
        }
    }

    fun updateNote(note: Note) = viewModelScope.launch {
        repo.noteDao.update(note)
    }


}
