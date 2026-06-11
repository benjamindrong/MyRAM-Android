package com.northsignalstudio.myram

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.northsignalstudio.myram.data.Note
import com.northsignalstudio.myram.ui.NotesViewModel
import com.northsignalstudio.myram.ui.ShareableExport
import com.northsignalstudio.myram.ui.screens.*
import com.northsignalstudio.myram.ui.theme.AppearanceSetting
import com.northsignalstudio.myram.ui.theme.EditorChromeStyle
import com.northsignalstudio.myram.ui.theme.MyRAMTheme
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startupSharedUris = extractSharedImageUris(intent)

        setContent {
            val prefs = remember {
                getSharedPreferences("myram_prefs", MODE_PRIVATE)
            }
            var pendingSharedUris by remember { mutableStateOf(startupSharedUris) }
            var pendingExport by remember { mutableStateOf<ShareableExport?>(null) }
            var pendingSaveFile by remember { mutableStateOf<File?>(null) }
            var appearanceSetting by remember {
                mutableStateOf(
                    AppearanceSetting.fromPreferenceValue(
                        prefs.getString("appearance_setting", AppearanceSetting.System.preferenceValue)
                    )
                )
            }
            var editorChromeStyle by remember {
                mutableStateOf(
                    EditorChromeStyle.fromPreferenceValue(
                        prefs.getString("editor_chrome_style", EditorChromeStyle.Standard.preferenceValue)
                    )
                )
            }
            var pinnedHighlightColor by remember {
                mutableStateOf(
                    PinnedHighlightColor.fromPreferenceValue(
                        prefs.getString("pinned_highlight_color", PinnedHighlightColor.Yellow.preferenceValue)
                    )
                )
            }
            val saveToFilesLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val targetUri = result.data?.data
                val sourceFile = pendingSaveFile
                pendingSaveFile = null
                if (result.resultCode != Activity.RESULT_OK || targetUri == null || sourceFile == null) {
                    return@rememberLauncherForActivityResult
                }

                runCatching {
                    contentResolver.openOutputStream(targetUri)?.use { output ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    } ?: error("Unable to open destination file.")
                }.onSuccess {
                    Toast.makeText(this, "Export saved to Files.", Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    showExportError(error.message ?: "Unable to save export.")
                }
            }

            fun updateAppearanceSetting(setting: AppearanceSetting) {
                appearanceSetting = setting
                prefs.edit().putString("appearance_setting", setting.preferenceValue).apply()
            }

            fun updateEditorChromeStyle(style: EditorChromeStyle) {
                editorChromeStyle = style
                prefs.edit().putString("editor_chrome_style", style.preferenceValue).apply()
            }

            fun updatePinnedHighlightColor(color: PinnedHighlightColor) {
                pinnedHighlightColor = color
                prefs.edit().putString("pinned_highlight_color", color.preferenceValue).apply()
            }

            MyRAMTheme(
                appearanceSetting = appearanceSetting,
                editorChromeStyle = editorChromeStyle
            ) {
                val vm = androidx.lifecycle.viewmodel.compose.viewModel<NotesViewModel>()

                pendingExport?.let { export ->
                    AlertDialog(
                        onDismissRequest = { pendingExport = null },
                        title = { Text("Export Notes") },
                        text = { Text("Choose where to send your export.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    pendingExport = null
                                    shareExportedFile(export)
                                }
                            ) {
                                Text("Share")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    pendingExport = null
                                    saveExportToFiles(export, saveToFilesLauncher) { file ->
                                        pendingSaveFile = file
                                    }
                                }
                            ) {
                                Text("Files")
                            }
                        }
                    )
                }

                var currentScreen by remember { mutableStateOf("list") }
                var selectedNote by remember { mutableStateOf<Note?>(null) }
                val currentNote by vm.currentNote.collectAsState()
                val editorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                    "list", "editor" -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            NotesListScreen(
                                vm = vm,
                                appearanceSetting = appearanceSetting,
                                onAppearanceSettingChanged = ::updateAppearanceSetting,
                                editorChromeStyle = editorChromeStyle,
                                onEditorChromeStyleChanged = ::updateEditorChromeStyle,
                                pinnedHighlightColor = pinnedHighlightColor,
                                onPinnedHighlightColorChanged = ::updatePinnedHighlightColor,
                                onExportSelectedNotes = { selectedNotes ->
                                    vm.exportNotesForSharing(
                                        notesToExport = selectedNotes,
                                        onSuccess = { pendingExport = it },
                                        onError = ::showExportError
                                    )
                                }
                            ) { note ->
                                selectedNote = note
                                currentScreen = "editor"
                                vm.selectNote(note)
                            }

                            if (selectedNote != null) {
                                ModalBottomSheet(
                                    onDismissRequest = {
                                        selectedNote = null
                                        currentScreen = "list"
                                        vm.selectNote(null)
                                    },
                                    sheetState = editorSheetState,
                                    dragHandle = null
                                ) {
                                    NoteEditorScreen(
                                        vm = vm,
                                        note = selectedNote,
                                        editorChromeStyle = editorChromeStyle,
                                        pinnedHighlightColor = pinnedHighlightColor,
                                        onNoteChanged = { selectedNote = it },
                                        onShareNote = { noteToShare ->
                                            vm.exportNotesForSharing(
                                                notesToExport = listOf(noteToShare),
                                                onSuccess = { pendingExport = it },
                                                onError = ::showExportError
                                            )
                                        }
                                    ) {
                                        selectedNote = null
                                        currentScreen = "list"
                                        vm.selectNote(null)
                                    }
                                }
                            }
                        }
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

    private fun shareExportedFile(export: com.northsignalstudio.myram.ui.ShareableExport) {
        if (export.uris.isEmpty()) {
            showExportError("No files were exported.")
            return
        }

        val chooserTitle = if (export.uris.size == 1) "Share note export" else "Share notes export"
        val shareIntent = if (export.uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = export.mimeType
                putExtra(Intent.EXTRA_STREAM, export.uris.first())
                clipData = ClipData.newUri(contentResolver, "note-export", export.uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = export.mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(export.uris))
                clipData = ClipData.newUri(contentResolver, "note-export", export.uris.first()).apply {
                    export.uris.drop(1).forEach { uri -> addItem(ClipData.Item(uri)) }
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }

    private fun saveExportToFiles(
        export: ShareableExport,
        launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
        onSourceReady: (File) -> Unit
    ) {
        val (sourceFile, suggestedName, mimeType) = buildFileForFilesExport(export)
        onSourceReady(sourceFile)
        val saveIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, suggestedName)
        }
        launcher.launch(saveIntent)
    }

    private fun buildFileForFilesExport(export: ShareableExport): Triple<File, String, String> {
        val files = export.files.distinctBy { it.absolutePath }
        require(files.isNotEmpty()) { "No files were exported." }
        if (files.size == 1) {
            val file = files.first()
            val mimeType = when (file.extension.lowercase()) {
                "json" -> "application/json"
                "zip" -> "application/zip"
                else -> export.mimeType
            }
            return Triple(file, file.name, mimeType)
        }

        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val zipFile = File(cacheDir, "MyRAM-Export-$timestamp.zip")
        val root = files.firstOrNull { it.extension.equals("json", ignoreCase = true) }?.parentFile
            ?: files.first().parentFile
            ?: cacheDir

        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            files.sortedBy { it.name }.forEach { file ->
                val entryName = file
                    .relativeToOrSelf(root)
                    .path
                    .replace(File.separatorChar, '/')
                zip.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }

        return Triple(zipFile, zipFile.name, "application/zip")
    }

    private fun showExportError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
