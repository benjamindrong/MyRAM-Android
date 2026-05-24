package com.apexcoretechs.myram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.apexcoretechs.myram.data.Note
import com.apexcoretechs.myram.ui.NotesViewModel
import com.apexcoretechs.myram.ui.screens.*
import com.apexcoretechs.myram.ui.theme.MyRAMTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyRAMTheme {
                val vm = androidx.lifecycle.viewmodel.compose.viewModel<NotesViewModel>()

                var currentScreen by remember { mutableStateOf("list") }
                var selectedNote by remember { mutableStateOf<Note?>(null) }
                val currentNote by vm.currentNote.collectAsState()

                // Auto open last note if available
                LaunchedEffect(currentNote) {
                    currentNote?.let {
                        selectedNote = it
                        currentScreen = "editor"
                    }
                }

                when (currentScreen) {
                    "list" -> NotesListScreen(vm = vm) { note ->
                        selectedNote = note
                        currentScreen = "editor"
                        vm.selectNote(note)
                    }
                    "editor" -> NoteEditorScreen(
                        vm = vm,
                        note = selectedNote,
                        onNoteChanged = { selectedNote = it }
                    ) {
                        currentScreen = "list"
                        vm.selectNote(null) // optional: clear current on back
                    }
                }
            }
        }
    }
}
