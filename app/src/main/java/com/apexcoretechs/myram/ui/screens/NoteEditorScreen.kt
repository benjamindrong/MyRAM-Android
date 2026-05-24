package com.apexcoretechs.myram.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
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
    var undoContent by remember(note?.id) { mutableStateOf<TextFieldValue?>(null) }
    var saveJob by remember { mutableStateOf<Job?>(null) }

    // Auto-save with debounce
    LaunchedEffect(title, content) {
        saveJob?.cancel()
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
                        onClick = {
                            undoContent?.let {
                                content = it
                                undoContent = null
                            }
                        },
                        enabled = undoContent != null
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
                onValueChange = { title = it },
                placeholder = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = content,
                onValueChange = {
                    if (it.text != content.text) {
                        undoContent = content.copy(selection = TextRange(content.text.length))
                    }
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
