package com.apexcoretechs.myram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.apexcoretechs.myram.data.Note
import com.apexcoretechs.myram.ui.NotesViewModel
import com.apexcoretechs.myram.ui.screens.*
import com.apexcoretechs.myram.ui.theme.AppearanceSetting
import com.apexcoretechs.myram.ui.theme.MyRAMTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs = remember {
                getSharedPreferences("myram_prefs", MODE_PRIVATE)
            }
            var appearanceSetting by remember {
                mutableStateOf(
                    AppearanceSetting.fromPreferenceValue(
                        prefs.getString("appearance_setting", AppearanceSetting.System.preferenceValue)
                    )
                )
            }

            fun updateAppearanceSetting(setting: AppearanceSetting) {
                appearanceSetting = setting
                prefs.edit().putString("appearance_setting", setting.preferenceValue).apply()
            }

            MyRAMTheme(appearanceSetting = appearanceSetting) {
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
                    "list" -> NotesListScreen(
                        vm = vm,
                        appearanceSetting = appearanceSetting,
                        onAppearanceSettingChanged = ::updateAppearanceSetting
                    ) { note ->
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
