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

                var currentScreen by remember { mutableStateOf("folders") }
                var selectedNote by remember { mutableStateOf<Note?>(null) }

                when (currentScreen) {
                    "folders" -> FoldersScreen(vm = vm).apply {
                        vm.selectedFolder.collectAsState().value?.let {
                            currentScreen = "notes"
                        }
                    }
                    "notes" -> NotesListScreen(vm = vm) { note ->
                        selectedNote = note
                        currentScreen = "editor"
                    }
                    "editor" -> NoteEditorScreen(vm = vm, note = selectedNote) {
                        currentScreen = "notes"
                    }
                }
            }
        }
    }
}
