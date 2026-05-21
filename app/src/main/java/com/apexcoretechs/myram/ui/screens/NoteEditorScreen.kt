package com.apexcoretechs.myram.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(TextFieldValue(note?.title ?: "")) }
    var content by remember { mutableStateOf(TextFieldValue(note?.content ?: "")) }
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
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
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
                onValueChange = { content = it },
                placeholder = { Text("Start typing...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                singleLine = false
            )
        }
    }
}