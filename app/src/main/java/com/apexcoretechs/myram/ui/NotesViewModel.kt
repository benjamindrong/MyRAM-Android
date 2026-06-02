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
import com.apexcoretechs.myram.data.PinnedText
import com.apexcoretechs.myram.data.Repository
import com.apexcoretechs.myram.export.NoteExporter
import com.apexcoretechs.myram.intelligence.NoteIntelligenceService
import com.apexcoretechs.myram.ui.richtext.plainTextFromStoredContent
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val recentlyDeletedRetentionMillis = 7L * 24 * 60 * 60 * 1000
    val repo = Repository.get(getApplication())
    private val noteIntelligenceService = NoteIntelligenceService(app)

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
        sortNotesForDisplay(notes.filter { it.folderId == folderId })
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val pinnedTextByNoteId = combine(allActiveNotes, _currentFolderId) { notes, folderId ->
        notes.filter { it.folderId == folderId }.map { it.id }
    }.flatMapLatest { noteIds ->
        if (noteIds.isEmpty()) {
            flowOf(emptyMap())
        } else {
            repo.noteDao.getPinnedTextForNotes(noteIds).map { pinnedText ->
                pinnedText.sortedPinnedText().groupBy { it.noteId }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

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
    private val _noteSuggestionLabels = MutableStateFlow<List<String>>(emptyList())
    val noteSuggestionLabels = _noteSuggestionLabels.asStateFlow()

    private val _recentlyDeletedNotes = MutableStateFlow<List<Note>>(emptyList())
    val recentlyDeletedNotes = _recentlyDeletedNotes.asStateFlow()

    private val prefs = app.getSharedPreferences("myram_prefs", Application.MODE_PRIVATE)
    private var applyingHistoryAction = false
    private val _mainListTitle = MutableStateFlow(
        prefs.getString("main_list_title", "My Notes")?.takeIf { it.isNotBlank() } ?: "My Notes"
    )
    val mainListTitle: StateFlow<String> = _mainListTitle.asStateFlow()

    private val _canUndoActions = MutableStateFlow(false)
    val canUndoActions: StateFlow<Boolean> = _canUndoActions.asStateFlow()
    private val _canRedoActions = MutableStateFlow(false)
    val canRedoActions: StateFlow<Boolean> = _canRedoActions.asStateFlow()
    private val undoStack = ArrayDeque<HistoryAction>()
    private val redoStack = ArrayDeque<HistoryAction>()

    init {
        viewModelScope.launch {
            purgeExpiredDeletedNotes()
            loadLastNote()
        }
    }

    fun renameMainListTitle(newTitle: String) {
        val previous = _mainListTitle.value
        val trimmed = newTitle.trim()
        val resolved = if (trimmed.isBlank()) "My Notes" else trimmed
        if (previous == resolved) return
        _mainListTitle.value = resolved
        prefs.edit().putString("main_list_title", resolved).apply()
        if (!applyingHistoryAction) {
            pushUndoAction(
                HistoryAction(
                    undo = {
                        _mainListTitle.value = previous
                        prefs.edit().putString("main_list_title", previous).apply()
                    },
                    redo = {
                        _mainListTitle.value = resolved
                        prefs.edit().putString("main_list_title", resolved).apply()
                    }
                )
            )
        }
    }

    private suspend fun loadLastNote() {
        val lastNoteId = prefs.getInt("last_note_id", -1)
        if (lastNoteId == -1) return

        repo.noteDao.getById(lastNoteId).first()?.let { note ->
            _currentNote.value = note
            _currentFolderId.value = note.folderId
            noteIntelligenceService.recordOpen(note.id)
            refreshNoteSuggestions(note, note.title, plainTextFromStoredContent(note.content))
        }
    }

    fun selectNote(note: Note?) {
        _currentNote.value = note
        if (note == null) {
            _noteSuggestionLabels.value = emptyList()
            prefs.edit().remove("last_note_id").apply()
            return
        }
        prefs.edit().putInt("last_note_id", note.id).apply()
        noteIntelligenceService.recordOpen(note.id)
        refreshNoteSuggestions(note, note.title, plainTextFromStoredContent(note.content))
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
        if (!applyingHistoryAction) {
            pushUndoAction(
                HistoryAction(
                    undo = {
                        repo.noteDao.delete(created)
                        if (_currentNote.value?.id == created.id) {
                            _currentNote.value = null
                            _noteSuggestionLabels.value = emptyList()
                        }
                    },
                    redo = {
                        repo.noteDao.upsert(created)
                    },
                    referencedNoteIds = setOf(created.id)
                )
            )
        }
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
        noteIntelligenceService.recordEdit(updated.id)
        refreshNoteSuggestions(
            updated,
            updated.title,
            plainTextFromStoredContent(updated.content)
        )
    }

    fun refreshNoteSuggestions(note: Note, draftTitle: String, draftContent: String) = viewModelScope.launch {
        val workingNote = note.copy(title = draftTitle, content = draftContent)
        val attachments = repo.noteDao.getAttachmentsForNote(note.id).first()
        val activeNotes = allActiveNotes.value
            .map { existing ->
                val plainExisting = existing.copy(content = plainTextFromStoredContent(existing.content))
                if (existing.id == note.id) workingNote else plainExisting
            }

        val labels = noteIntelligenceService.suggestionLabelsFor(
            note = workingNote,
            allActiveNotes = activeNotes,
            attachments = attachments
        )
        _noteSuggestionLabels.value = labels
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
        val id = repo.folderDao.insert(folder).toInt()
        val created = folder.copy(id = id)
        if (!applyingHistoryAction) {
            pushUndoAction(
                HistoryAction(
                    undo = {
                        repo.folderDao.delete(created)
                    },
                    redo = {
                        repo.folderDao.upsert(created)
                    }
                )
            )
        }
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

    fun renameNote(note: Note, newTitle: String) = viewModelScope.launch {
        if (note.deletedAt != null) return@launch
        val updated = note.copy(
            title = newTitle.trim(),
            lastModified = System.currentTimeMillis()
        )
        repo.noteDao.update(updated)
        if (_currentNote.value?.id == updated.id) {
            _currentNote.value = updated
        }
    }

    fun setNotePinned(note: Note, isPinned: Boolean) = viewModelScope.launch {
        if (note.deletedAt != null || note.isPinned == isPinned) return@launch
        val updated = note.copy(
            isPinned = isPinned,
            lastModified = System.currentTimeMillis()
        )
        repo.noteDao.update(updated)
        if (_currentNote.value?.id == updated.id) {
            _currentNote.value = updated
        }
    }

    fun recordTextUndoSnapshot(note: Note, previousTitle: String, previousContent: String) {
        if (applyingHistoryAction) return
        var redoTitle: String? = null
        var redoContent: String? = null
        pushUndoAction(
            HistoryAction(
                undo = undo@{
                    val latest = repo.noteDao.getByIdIncludingDeleted(note.id) ?: return@undo
                    if (latest.deletedAt != null) return@undo
                    redoTitle = latest.title
                    redoContent = latest.content
                    repo.noteDao.update(
                        latest.copy(
                            title = previousTitle,
                            content = previousContent,
                            lastModified = System.currentTimeMillis()
                        )
                    )
                },
                redo = redo@{
                    val restoredTitle = redoTitle ?: return@redo
                    val restoredContent = redoContent ?: return@redo
                    val latest = repo.noteDao.getByIdIncludingDeleted(note.id) ?: return@redo
                    if (latest.deletedAt != null) return@redo
                    repo.noteDao.update(
                        latest.copy(
                            title = restoredTitle,
                            content = restoredContent,
                            lastModified = System.currentTimeMillis()
                        )
                    )
                },
                referencedNoteIds = setOf(note.id)
            )
        )
    }

    fun deleteFolder(folder: Folder, preserveNotes: Boolean) = viewModelScope.launch {
        val subtreeIds = folderSubtreeIds(folder.id, allFolders.value)
        if (subtreeIds.isEmpty()) return@launch
        val now = System.currentTimeMillis()
        val snapshotFolders = allFolders.value.filter { subtreeIds.contains(it.id) }
        val noteSnapshots = repo.noteDao.getAllIncludingDeleted()
            .filter { note -> note.folderId != null && subtreeIds.contains(note.folderId) }
            .map { note ->
                NoteFolderSnapshot(
                    noteId = note.id,
                    previousFolderId = note.folderId,
                    previousDeletedAt = note.deletedAt
                )
            }

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
            repo.noteDao.softDeleteNotesInFolders(subtreeIds.toList(), now)
            if (_currentNote.value?.folderId != null && subtreeIds.contains(_currentNote.value?.folderId)) {
                _currentNote.value = null
                _noteSuggestionLabels.value = emptyList()
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

        if (!applyingHistoryAction) {
            pushUndoAction(
                HistoryAction(
                    undo = {
                        val foldersById = snapshotFolders.associateBy { it.id }
                        snapshotFolders
                            .sortedBy { depth(it, foldersById) }
                            .forEach { folderSnapshot ->
                                repo.folderDao.upsert(folderSnapshot)
                            }

                        noteSnapshots.forEach { noteSnapshot ->
                            val restored = repo.noteDao.getByIdIncludingDeleted(noteSnapshot.noteId) ?: return@forEach
                            repo.noteDao.update(
                                restored.copy(
                                    folderId = noteSnapshot.previousFolderId,
                                    deletedAt = noteSnapshot.previousDeletedAt,
                                    lastModified = System.currentTimeMillis()
                                )
                            )
                        }
                    },
                    redo = {
                        val deletedFoldersById = snapshotFolders.associateBy { it.id }
                        val ordered = snapshotFolders.sortedByDescending { depth(it, deletedFoldersById) }
                        ordered.forEach { folderSnapshot -> repo.folderDao.delete(folderSnapshot) }

                        val nowRedo = System.currentTimeMillis()
                        if (preserveNotes) {
                            repo.noteDao.moveNotesInFoldersToTopLevel(subtreeIds.toList(), nowRedo)
                        } else {
                            repo.noteDao.softDeleteNotesInFolders(subtreeIds.toList(), nowRedo)
                        }
                    },
                    referencedNoteIds = noteSnapshots.mapTo(mutableSetOf()) { it.noteId }
                )
            )
        }
        refreshRecentlyDeletedNotes()
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
        val previousFolderId = note.folderId
        val updated = note.copy(
            folderId = destinationFolderId,
            lastModified = System.currentTimeMillis()
        )
        repo.noteDao.update(updated)
        if (!applyingHistoryAction) {
            pushUndoAction(
                HistoryAction(
                    undo = undo@{
                        val target = repo.noteDao.getByIdIncludingDeleted(note.id) ?: return@undo
                        repo.noteDao.update(
                            target.copy(
                                folderId = previousFolderId,
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    },
                    redo = redo@{
                        val target = repo.noteDao.getByIdIncludingDeleted(note.id) ?: return@redo
                        repo.noteDao.update(
                            target.copy(
                                folderId = destinationFolderId,
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    },
                    referencedNoteIds = setOf(note.id)
                )
            )
        }
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

    fun pinnedText(noteId: Int?): Flow<List<PinnedText>> {
        return if (noteId == null || noteId <= 0) {
            flowOf(emptyList())
        } else {
            repo.noteDao.getPinnedTextForNote(noteId)
        }
    }

    fun addPinnedText(
        note: Note,
        text: String = "",
        sourceContent: String = "",
        sourceStart: Int = 0,
        onCreated: (PinnedText) -> Unit = {}
    ) = viewModelScope.launch {
        if (note.deletedAt != null) return@launch
        val trimmed = text.trim()
        val existing = repo.noteDao.getPinnedTextForNotesOnce(listOf(note.id))
        val now = System.currentTimeMillis()
        val pinnedText = PinnedText(
            noteId = note.id,
            text = trimmed,
            sourceContent = sourceContent.ifBlank { trimmed },
            sourceStart = sourceStart.coerceAtLeast(0),
            sortOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1,
            createdAt = now,
            lastModified = now
        )
        val id = repo.noteDao.insertPinnedText(pinnedText)
        touchNote(note.id, now)
        onCreated(pinnedText.copy(id = id))
    }

    fun updatePinnedText(pinnedText: PinnedText, text: String) = viewModelScope.launch {
        val trimmed = text.trim()
        if (pinnedText.text == trimmed) return@launch
        val now = System.currentTimeMillis()
        repo.noteDao.updatePinnedText(
            pinnedText.copy(
                text = trimmed,
                lastModified = now
            )
        )
        touchNote(pinnedText.noteId, now)
    }

    fun setPinnedTextCollapsed(pinnedText: PinnedText, isCollapsed: Boolean) = viewModelScope.launch {
        if (pinnedText.isCollapsed == isCollapsed) return@launch
        val now = System.currentTimeMillis()
        repo.noteDao.updatePinnedText(
            pinnedText.copy(
                isCollapsed = isCollapsed,
                lastModified = now
            )
        )
        touchNote(pinnedText.noteId, now)
    }

    fun movePinnedText(pinnedText: PinnedText, toIndex: Int) = viewModelScope.launch {
        val ordered = repo.noteDao.getPinnedTextForNotesOnce(listOf(pinnedText.noteId))
            .sortedPinnedText()
            .toMutableList()
        val currentIndex = ordered.indexOfFirst { it.id == pinnedText.id }
        if (currentIndex < 0) return@launch
        val clampedTarget = toIndex.coerceIn(0, ordered.size)
        if (clampedTarget == currentIndex || clampedTarget == currentIndex + 1) return@launch

        val moved = ordered.removeAt(currentIndex)
        val adjustedTarget = if (clampedTarget > currentIndex) clampedTarget - 1 else clampedTarget
        ordered.add(adjustedTarget, moved)

        val now = System.currentTimeMillis()
        ordered.forEachIndexed { index, item ->
            repo.noteDao.updatePinnedText(item.copy(sortOrder = index, lastModified = now))
        }
        touchNote(pinnedText.noteId, now)
    }

    fun unpinText(pinnedText: PinnedText) = viewModelScope.launch {
        repo.noteDao.deletePinnedText(pinnedText)
        val now = System.currentTimeMillis()
        repo.noteDao.getPinnedTextForNotesOnce(listOf(pinnedText.noteId))
            .sortedPinnedText()
            .forEachIndexed { index, item ->
                if (item.sortOrder != index) {
                    repo.noteDao.updatePinnedText(item.copy(sortOrder = index, lastModified = now))
                }
            }
        touchNote(pinnedText.noteId, now)
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
        val previousDeletedAt = note.deletedAt
        val previousFolderId = note.folderId
        val updated = note.copy(
            lastModified = System.currentTimeMillis(),
            deletedAt = System.currentTimeMillis()
        )
        repo.noteDao.update(updated)
        if (!applyingHistoryAction) {
            pushUndoAction(
                HistoryAction(
                    undo = undo@{
                        val target = repo.noteDao.getByIdIncludingDeleted(note.id) ?: return@undo
                        repo.noteDao.update(
                            target.copy(
                                deletedAt = previousDeletedAt,
                                folderId = previousFolderId,
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    },
                    redo = redo@{
                        val target = repo.noteDao.getByIdIncludingDeleted(note.id) ?: return@redo
                        repo.noteDao.update(
                            target.copy(
                                deletedAt = System.currentTimeMillis(),
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    },
                    referencedNoteIds = setOf(note.id)
                )
            )
        }
        if (_currentNote.value?.id == note.id) {
            _currentNote.value = null
            _noteSuggestionLabels.value = emptyList()
            prefs.edit().remove("last_note_id").apply()
        }
        refreshRecentlyDeletedNotes()
    }

    fun undoLastAction() = viewModelScope.launch {
        val action = undoStack.removeLastOrNull() ?: return@launch
        applyingHistoryAction = true
        runCatching { action.undo() }
        applyingHistoryAction = false
        redoStack.addLast(action)
        if (redoStack.size > 200) {
            redoStack.removeFirst()
        }
        updateHistoryAvailability()
        _currentNote.value?.let { active ->
            repo.noteDao.getByIdIncludingDeleted(active.id)?.let { refreshed ->
                if (refreshed.deletedAt == null) {
                    _currentNote.value = refreshed
                } else {
                    _currentNote.value = null
                }
            }
        }
        if (_currentNote.value == null) {
            _noteSuggestionLabels.value = emptyList()
            prefs.edit().remove("last_note_id").apply()
        }
        refreshRecentlyDeletedNotes()
    }

    fun redoLastAction() = viewModelScope.launch {
        val action = redoStack.removeLastOrNull() ?: return@launch
        applyingHistoryAction = true
        runCatching { action.redo() }
        applyingHistoryAction = false
        undoStack.addLast(action)
        if (undoStack.size > 200) {
            undoStack.removeFirst()
        }
        updateHistoryAvailability()
        _currentNote.value?.let { active ->
            repo.noteDao.getByIdIncludingDeleted(active.id)?.let { refreshed ->
                if (refreshed.deletedAt == null) {
                    _currentNote.value = refreshed
                } else {
                    _currentNote.value = null
                }
            }
        }
        if (_currentNote.value == null) {
            _noteSuggestionLabels.value = emptyList()
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
        removeUndoReferencesForNote(note.id)
        if (_currentNote.value?.id == note.id) {
            _currentNote.value = null
            _noteSuggestionLabels.value = emptyList()
            prefs.edit().remove("last_note_id").apply()
        }
        refreshRecentlyDeletedNotes()
    }

    private fun pushUndoAction(action: HistoryAction) {
        undoStack.addLast(action)
        if (undoStack.size > 200) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        updateHistoryAvailability()
    }

    private fun removeUndoReferencesForNote(noteId: Int) {
        val filtered = undoStack.filterNot { action -> action.referencedNoteIds.contains(noteId) }
        val filteredRedo = redoStack.filterNot { action -> action.referencedNoteIds.contains(noteId) }
        undoStack.clear()
        undoStack.addAll(filtered)
        redoStack.clear()
        redoStack.addAll(filteredRedo)
        updateHistoryAvailability()
    }

    private fun updateHistoryAvailability() {
        _canUndoActions.value = undoStack.isNotEmpty()
        _canRedoActions.value = redoStack.isNotEmpty()
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
            val pinnedTextByNoteId = repo.noteDao.getPinnedTextForNotesOnce(noteIds)
                .groupBy { it.noteId }

            val foldersById = repo.folderDao.getAll().first().associateBy { it.id }
            val folderPathProvider: (Note) -> List<String> = { note ->
                buildFolderPath(note.folderId, foldersById)
            }

            val artifact = NoteExporter.exportNotes(
                notes = activeNotes,
                attachmentsByNoteId = attachmentsByNoteId,
                pinnedTextByNoteId = pinnedTextByNoteId,
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

    private suspend fun touchNote(noteId: Int, timestamp: Long = System.currentTimeMillis()) {
        val latest = repo.noteDao.getByIdIncludingDeleted(noteId) ?: return
        if (latest.deletedAt != null) return
        val updated = latest.copy(lastModified = timestamp)
        repo.noteDao.update(updated)
        if (_currentNote.value?.id == updated.id) {
            _currentNote.value = updated
        }
    }
}

private data class HistoryAction(
    val undo: suspend () -> Unit,
    val redo: suspend () -> Unit,
    val referencedNoteIds: Set<Int> = emptySet()
)

private data class NoteFolderSnapshot(
    val noteId: Int,
    val previousFolderId: Int?,
    val previousDeletedAt: Long?
)

internal fun sortNotesForDisplay(notes: List<Note>): List<Note> {
    return notes.sortedWith(
        compareByDescending<Note> { it.isPinned }
            .thenByDescending { it.lastModified }
            .thenByDescending { it.createdAt }
    )
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

internal fun List<PinnedText>.sortedPinnedText(): List<PinnedText> {
    return sortedWith(compareBy<PinnedText> { it.sortOrder }.thenBy { it.createdAt })
}
