package com.apexcoretechs.myram.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apexcoretechs.myram.data.Note
import com.apexcoretechs.myram.data.NotePhotoAttachment
import com.apexcoretechs.myram.data.Repository
import com.apexcoretechs.myram.export.NoteExporter
import java.io.File
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    fun noteAttachments(noteId: Int?): Flow<List<NotePhotoAttachment>> {
        return if (noteId == null || noteId <= 0) {
            flowOf(emptyList())
        } else {
            repo.noteDao.getAttachmentsForNote(noteId)
        }
    }

    fun addPhotoAttachments(noteId: Int, uris: List<Uri>) = viewModelScope.launch {
        if (noteId <= 0 || uris.isEmpty()) return@launch
        uris.forEach { uri ->
            loadNormalizedImageData(uri)?.let { imageData ->
                repo.noteDao.insertAttachment(
                    NotePhotoAttachment(
                        noteId = noteId,
                        imageData = imageData
                    )
                )
            }
        }
    }

    fun removePhotoAttachment(attachment: NotePhotoAttachment) = viewModelScope.launch {
        repo.noteDao.deleteAttachment(attachment)
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

    private fun loadNormalizedImageData(uri: Uri): ByteArray? {
        val resolver = getApplication<Application>().contentResolver
        val sourceBytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val bitmap = BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size) ?: return sourceBytes
        return compressBitmap(bitmap) ?: sourceBytes
    }

    private fun compressBitmap(bitmap: Bitmap): ByteArray? {
        return ByteArrayOutputStream().use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)) return null
            output.toByteArray()
        }
    }

    fun exportNotesForSharing(
        notesToExport: List<Note>,
        onSuccess: (ShareableExport) -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        runCatching {
            createShareableExport(notesToExport)
        }.onSuccess(onSuccess)
            .onFailure { error ->
                onError(error.message ?: "Unable to export notes.")
            }
    }

    private suspend fun createShareableExport(notesToExport: List<Note>): ShareableExport =
        withContext(Dispatchers.IO) {
            val activeNotes = notesToExport.filter { it.deletedAt == null }
            require(activeNotes.isNotEmpty()) { "No notes selected for export." }

            val app = getApplication<Application>()
            val exportsDirectory = File(app.cacheDir, "exports")
            if (!exportsDirectory.exists()) {
                exportsDirectory.mkdirs()
            }

            val artifact = NoteExporter.exportNotes(activeNotes, exportsDirectory)
            val uri = FileProvider.getUriForFile(
                app,
                "${app.packageName}.fileprovider",
                artifact.file
            )

            ShareableExport(
                uri = uri,
                mimeType = artifact.mimeType
            )
        }
}

data class ShareableExport(
    val uri: Uri,
    val mimeType: String
)
