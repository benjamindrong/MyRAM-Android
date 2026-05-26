package com.apexcoretechs.myram

import android.content.Intent
import android.net.Uri
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
        val startupSharedUris = extractSharedImageUris(intent)

        setContent {
            val prefs = remember {
                getSharedPreferences("myram_prefs", MODE_PRIVATE)
            }
            var pendingSharedUris by remember { mutableStateOf(startupSharedUris) }
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

                LaunchedEffect(pendingSharedUris) {
                    if (pendingSharedUris.isEmpty()) return@LaunchedEffect
                    vm.createNote { created ->
                        selectedNote = created
                        currentScreen = "editor"
                        vm.selectNote(created)
                        vm.addPhotoAttachments(created.id, pendingSharedUris)
                        pendingSharedUris = emptyList()
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

    private fun extractSharedImageUris(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        val action = intent.action ?: return emptyList()
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return emptyList()

        @Suppress("DEPRECATION")
        return when (action) {
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                listOfNotNull(uri)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                uris?.filterNotNull().orEmpty()
            }
            else -> emptyList()
        }
    }
}
