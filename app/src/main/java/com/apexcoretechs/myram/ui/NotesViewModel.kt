package com.apexcoretechs.myram.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apexcoretechs.myram.data.Folder
import com.apexcoretechs.myram.data.Note
import com.apexcoretechs.myram.data.NotePhotoAttachment
import com.apexcoretechs.myram.data.Repository
import com.apexcoretechs.myram.export.NoteExporter
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val recentlyDeletedRetentionMillis = 7L * 24 * 60 * 60 * 1000
    val repo = Repository.get(getApplication())

    private val _currentFolderId = MutableStateFlow<Int?>(null)
    val currentFolderId: StateFlow<Int?> = _currentFolderId.asStateFlow()

    val allFolders = repo.folderDao.getAll().stateIn(
        viewModelScope, SharingStarted.Lazily, emptyList()
    )

    private val allActiveNotes = repo.noteDao.getAll().stateIn(
        viewModelScope, SharingStarted.Lazily, emptyList()
    )

    val visibleFolders = combine(allFolders, _currentFolderId) { folders, parentId ->
        folders.filter { it.parentFolderId == parentId }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val visibleNotes = combine(allActiveNotes, _currentFolderId) { notes, folderId ->
        notes.filter { it.folderId == folderId }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val folderActiveNoteCounts = combine(allFolders, allActiveNotes) { folders, notes ->
        computeFolderActiveNoteCounts(folders = folders, notes = notes)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // Backward-compatible alias used by older screens.
    val allNotes = visibleNotes

    val currentFolder = combine(allFolders, _currentFolderId) { folders, selectedId ->
        folders.firstOrNull { it.id == selectedId }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote = _currentNote.asStateFlow()

    private val _recentlyDeletedNotes = MutableStateFlow<List<Note>>(emptyList())
    val recentlyDeletedNotes = _recentlyDeletedNotes.asStateFlow()

    private val prefs = app.getSharedPreferences("myram_prefs", Application.MODE_PRIVATE)

    init {
        viewModelScope.launch {
            purgeExpiredDeletedNotes()
            loadLastNote()
        }
    }

    private suspend fun loadLastNote() {
        val lastNoteId = prefs.getInt("last_note_id", -1)
        if (lastNoteId == -1) return

        repo.noteDao.getById(lastNoteId).first()?.let { note ->
            _currentNote.value = note
            _currentFolderId.value = note.folderId
        }
    }

    fun selectNote(note: Note?) {
        _currentNote.value = note
        if (note == null) {
            prefs.edit().remove("last_note_id").apply()
            return
        }
        prefs.edit().putInt("last_note_id", note.id).apply()
    }

    fun openFolder(folder: Folder) {
        _currentFolderId.value = folder.id
    }

    fun navigateToParentFolder() {
        val activeFolder = currentFolder.value ?: return
        _currentFolderId.value = activeFolder.parentFolderId
    }

    fun createNote(
        title: String? = null,
        content: String? = null,
        onCreated: (Note) -> Unit = {}
    ) = viewModelScope.launch {
        val newNote = Note(
            title = title?.takeIf { it.isNotBlank() } ?: "",
            content = content?.takeIf { it.isNotBlank() } ?: "",
            folderId = _currentFolderId.value
        )
        val id = repo.noteDao.insert(newNote)
        val created = newNote.copy(id = id.toInt())
        selectNote(created)
        onCreated(created)
    }

    fun updateNote(note: Note) = viewModelScope.launch {
        if (note.deletedAt != null) return@launch
        val updated = note.copy(lastModified = System.currentTimeMillis())
        repo.noteDao.update(updated)
        if (_currentNote.value?.id == updated.id) {
            _currentNote.value = updated
        }
    }

    fun createFolder(name: String = "New Folder") = viewModelScope.launch {
        val trimmed = name.trim()
        val now = System.currentTimeMillis()
        val folder = Folder(
            name = if (trimmed.isBlank()) "New Folder" else trimmed,
            parentFolderId = _currentFolderId.value,
            createdAt = now,
            modifiedAt = now
        )
        repo.folderDao.insert(folder)
    }

    fun renameFolder(folder: Folder, newName: String) = viewModelScope.launch {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return@launch
        repo.folderDao.update(
            folder.copy(
                name = trimmed,
                modifiedAt = System.currentTimeMillis()
            )
        )
    }

    fun deleteFolder(folder: Folder, preserveNotes: Boolean) = viewModelScope.launch {
        val subtreeIds = folderSubtreeIds(folder.id, allFolders.value)
        if (subtreeIds.isEmpty()) return@launch
        val now = System.currentTimeMillis()

        if (preserveNotes) {
            repo.noteDao.moveNotesInFoldersToTopLevel(subtreeIds.toList(), now)
            _currentNote.value = _currentNote.value?.let { note ->
                if (note.folderId != null && subtreeIds.contains(note.folderId)) {
                    note.copy(folderId = null, lastModified = now)
                } else {
                    note
                }
            }
        } else {
            repo.noteDao.deleteNotesInFolders(subtreeIds.toList())
            if (_currentNote.value?.folderId != null && subtreeIds.contains(_currentNote.value?.folderId)) {
                _currentNote.value = null
                prefs.edit().remove("last_note_id").apply()
            }
        }

        val foldersById = allFolders.value.associateBy { it.id }
        val orderedForDelete = subtreeIds
            .mapNotNull { foldersById[it] }
            .sortedByDescending { depth(it, foldersById) }
        orderedForDelete.forEach { repo.folderDao.delete(it) }

        if (_currentFolderId.value != null && subtreeIds.contains(_currentFolderId.value)) {
            _currentFolderId.value = folder.parentFolderId
        }
    }

    private fun depth(folder: Folder, byId: Map<Int, Folder>): Int {
        var depth = 0
        var cursor = folder.parentFolderId
        while (cursor != null) {
            depth += 1
            cursor = byId[cursor]?.parentFolderId
        }
        return depth
    }

    private fun folderSubtreeIds(rootId: Int, folders: List<Folder>): Set<Int> {
        val childrenByParent = folders.groupBy { it.parentFolderId }
        val result = mutableSetOf<Int>()
        val queue = ArrayDeque<Int>()
        queue.add(rootId)
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            if (!result.add(next)) continue
            childrenByParent[next].orEmpty().forEach { child ->
                queue.add(child.id)
            }
        }
        return result
    }

    fun moveNote(note: Note, destinationFolderId: Int?) = viewModelScope.launch {
        if (note.deletedAt != null || note.folderId == destinationFolderId) return@launch
        val updated = note.copy(
            folderId = destinationFolderId,
            lastModified = System.currentTimeMillis()
        )
        repo.noteDao.update(updated)
        if (_currentNote.value?.id == updated.id) {
            _currentNote.value = updated
        }
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
        val updated = note.copy(
            lastModified = System.currentTimeMillis(),
            deletedAt = System.currentTimeMillis()
        )
        repo.noteDao.update(updated)
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
        val restored = note.copy(
            lastModified = System.currentTimeMillis(),
            deletedAt = null
        )
        repo.noteDao.update(restored)
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

            val noteIds = activeNotes.map { it.id }
            val attachmentsByNoteId = repo.noteDao.getAttachmentsForNotes(noteIds)
                .groupBy { it.noteId }

            val foldersById = repo.folderDao.getAll().first().associateBy { it.id }
            val folderPathProvider: (Note) -> List<String> = { note ->
                buildFolderPath(note.folderId, foldersById)
            }

            val artifact = NoteExporter.exportNotes(
                notes = activeNotes,
                attachmentsByNoteId = attachmentsByNoteId,
                folderPathProvider = folderPathProvider,
                exportDirectory = exportsDirectory
            )

            val uris = artifact.files.map { file ->
                FileProvider.getUriForFile(
                    app,
                    "${app.packageName}.fileprovider",
                    file
                )
            }

            ShareableExport(
                uris = uris,
                mimeType = artifact.mimeType,
                files = artifact.files
            )
        }

    private fun buildFolderPath(folderId: Int?, foldersById: Map<Int, Folder>): List<String> {
        if (folderId == null) return emptyList()
        val segments = mutableListOf<String>()
        var cursor: Int? = folderId
        while (cursor != null) {
            val folder = foldersById[cursor] ?: break
            segments.add(folder.name)
            cursor = folder.parentFolderId
        }
        return segments.reversed()
    }
}

internal fun computeFolderActiveNoteCounts(
    folders: List<Folder>,
    notes: List<Note>
): Map<Int, Int> {
    if (folders.isEmpty()) return emptyMap()

    val childFolderIdsByParent = folders
        .groupBy(keySelector = { it.parentFolderId }, valueTransform = { it.id })
    val directCountsByFolderId = notes
        .asSequence()
        .filter { it.deletedAt == null }
        .mapNotNull { it.folderId }
        .groupingBy { it }
        .eachCount()

    return buildMap(folders.size) {
        folders.forEach { folder ->
            var subtreeCount = 0
            val queue = ArrayDeque<Int>()
            val visited = mutableSetOf<Int>()
            queue.addLast(folder.id)

            while (queue.isNotEmpty()) {
                val folderId = queue.removeFirst()
                if (!visited.add(folderId)) continue

                subtreeCount += directCountsByFolderId[folderId] ?: 0
                childFolderIdsByParent[folderId].orEmpty().forEach(queue::addLast)
            }

            put(folder.id, subtreeCount)
        }
    }
}

data class ShareableExport(
    val uris: List<Uri>,
    val mimeType: String,
    val files: List<File>
)
