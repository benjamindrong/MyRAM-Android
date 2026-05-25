package com.apexcoretechs.myram.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.apexcoretechs.myram.data.Note
import com.apexcoretechs.myram.ui.NotesViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    vm: NotesViewModel,
    note: Note?,
    onNoteChanged: (Note) -> Unit,
    onBack: () -> Unit
) {
    var title by remember(note?.id) { mutableStateOf(TextFieldValue(note?.title ?: "")) }
    var content by remember(note?.id) { mutableStateOf(TextFieldValue(note?.content ?: "")) }
    var undoHistory by remember(note?.id) { mutableStateOf<List<EditorSnapshot>>(emptyList()) }
    var pendingUndoSnapshot by remember(note?.id) { mutableStateOf<EditorSnapshot?>(null) }
    var saveJob by remember { mutableStateOf<Job?>(null) }
    var undoJob by remember { mutableStateOf<Job?>(null) }
    var hasEditedCurrentNote by remember(note?.id) { mutableStateOf(false) }
    var isRestoringUndo by remember(note?.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun currentSnapshot() = EditorSnapshot(title = title, content = content)

    fun pushUndoSnapshot(snapshot: EditorSnapshot) {
        if (snapshot == currentSnapshot()) return
        undoHistory = (undoHistory + snapshot).takeLast(200)
    }

    fun flushPendingUndoSnapshot() {
        undoJob?.cancel()
        pendingUndoSnapshot?.let(::pushUndoSnapshot)
        pendingUndoSnapshot = null
    }

    fun scheduleUndoSnapshot(snapshot: EditorSnapshot) {
        if (isRestoringUndo) return
        if (pendingUndoSnapshot == null) {
            pendingUndoSnapshot = snapshot
        }
        undoJob?.cancel()
        undoJob = scope.launch {
            delay(800)
            pendingUndoSnapshot?.let(::pushUndoSnapshot)
            pendingUndoSnapshot = null
        }
    }

    fun undoLastEdit() {
        flushPendingUndoSnapshot()
        val snapshot = undoHistory.lastOrNull() ?: return
        undoHistory = undoHistory.dropLast(1)
        isRestoringUndo = true
        hasEditedCurrentNote = true
        title = snapshot.title
        content = snapshot.content
        isRestoringUndo = false
    }

    // Auto-save with debounce
    LaunchedEffect(note?.id, title, content) {
        saveJob?.cancel()
        if (!hasEditedCurrentNote) return@LaunchedEffect
        saveJob = vm.viewModelScope.launch {
            delay(800) // 800ms debounce
            note?.let { current ->
                val updated = current.copy(
                    title = title.text,
                    content = content.text
                )
                vm.updateNote(updated)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Note") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = ::undoLastEdit,
                        enabled = undoHistory.isNotEmpty() || pendingUndoSnapshot != null
                    ) {
                        Text("Undo")
                    }
                    IconButton(
                        onClick = {
                            note?.let { current ->
                                vm.deleteNote(current)
                                onBack()
                            }
                        },
                        enabled = note != null
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete note")
                    }
                    IconButton(
                        onClick = {
                            saveJob?.cancel()
                            vm.createNote { created ->
                                onNoteChanged(created)
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "New note")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    if (it != title) {
                        scheduleUndoSnapshot(currentSnapshot())
                    }
                    hasEditedCurrentNote = true
                    title = it
                },
                placeholder = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = content,
                onValueChange = {
                    if (it != content) {
                        scheduleUndoSnapshot(currentSnapshot())
                    }
                    hasEditedCurrentNote = true
                    content = it
                },
                placeholder = { Text("Start typing...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                singleLine = false
            )
        }
    }
}

private data class EditorSnapshot(
    val title: TextFieldValue,
    val content: TextFieldValue
)
