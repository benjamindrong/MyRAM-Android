package com.apexcoretechs.myram.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.apexcoretechs.myram.data.Folder
import com.apexcoretechs.myram.data.Note
import com.apexcoretechs.myram.ui.NotesViewModel
import com.apexcoretechs.myram.ui.theme.AppearanceSetting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    vm: NotesViewModel,
    appearanceSetting: AppearanceSetting,
    onAppearanceSettingChanged: (AppearanceSetting) -> Unit,
    onExportSelectedNotes: (List<Note>) -> Unit,
    onNoteSelected: (Note?) -> Unit
) {
    val notes by vm.visibleNotes.collectAsState()
    val visibleFolders by vm.visibleFolders.collectAsState()
    val folderActiveNoteCounts by vm.folderActiveNoteCounts.collectAsState()
    val allFolders by vm.allFolders.collectAsState()
    val currentFolder by vm.currentFolder.collectAsState()
    val currentFolderId by vm.currentFolderId.collectAsState()
    val recentlyDeletedNotes by vm.recentlyDeletedNotes.collectAsState()
    val mainListTitle by vm.mainListTitle.collectAsState()
    val canUndoActions by vm.canUndoActions.collectAsState()

    var actionsMenuExpanded by remember { mutableStateOf(false) }
    var appearanceMenuExpanded by remember { mutableStateOf(false) }
    var showingRecentlyDeleted by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedNoteIds by remember { mutableStateOf(setOf<Int>()) }
    var showingCreateFolderDialog by remember { mutableStateOf(false) }
    var createFolderName by remember { mutableStateOf("") }
    var folderToRename by remember { mutableStateOf<Folder?>(null) }
    var renameFolderName by remember { mutableStateOf("") }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }
    var noteToMove by remember { mutableStateOf<Note?>(null) }
    var previewedNote by remember { mutableStateOf<Note?>(null) }
    var showingRenameMainListDialog by remember { mutableStateOf(false) }
    var renameMainListTitle by remember { mutableStateOf(mainListTitle) }
    var activeFolderMenuId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(showingRecentlyDeleted, currentFolderId) {
        selectionMode = false
        selectedNoteIds = emptySet()
        activeFolderMenuId = null
        previewedNote = null
        if (showingRecentlyDeleted) {
            vm.refreshRecentlyDeletedNotes()
        }
    }

    if (showingCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = {
                showingCreateFolderDialog = false
                createFolderName = ""
            },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = createFolderName,
                    onValueChange = { createFolderName = it },
                    label = { Text("Folder name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.createFolder(createFolderName)
                        showingCreateFolderDialog = false
                        createFolderName = ""
                    },
                    enabled = createFolderName.trim().isNotEmpty()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showingCreateFolderDialog = false
                        createFolderName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    folderToRename?.let { folder ->
        AlertDialog(
            onDismissRequest = {
                folderToRename = null
                renameFolderName = ""
            },
            title = { Text("Rename Folder") },
            text = {
                OutlinedTextField(
                    value = renameFolderName,
                    onValueChange = { renameFolderName = it },
                    label = { Text("Folder name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.renameFolder(folder, renameFolderName)
                        folderToRename = null
                        renameFolderName = ""
                    },
                    enabled = renameFolderName.trim().isNotEmpty()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        folderToRename = null
                        renameFolderName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showingRenameMainListDialog) {
        AlertDialog(
            onDismissRequest = {
                showingRenameMainListDialog = false
                renameMainListTitle = mainListTitle
            },
            title = { Text("Rename Main List") },
            text = {
                OutlinedTextField(
                    value = renameMainListTitle,
                    onValueChange = { renameMainListTitle = it },
                    label = { Text("Main list title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.renameMainListTitle(renameMainListTitle)
                        showingRenameMainListDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showingRenameMainListDialog = false
                        renameMainListTitle = mainListTitle
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete Folder") },
            text = {
                Text("Delete this folder only and keep its notes at top level, or delete this folder and all notes inside it?")
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { folderToDelete = null }) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            vm.deleteFolder(folder, preserveNotes = true)
                            folderToDelete = null
                        }
                    ) {
                        Text("Keep Notes")
                    }
                    TextButton(
                        onClick = {
                            vm.deleteFolder(folder, preserveNotes = false)
                            folderToDelete = null
                        }
                    ) {
                        Text("Delete All", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }

    noteToMove?.let { note ->
        var destinationFolderId by remember(note.id) { mutableStateOf(note.folderId) }
        AlertDialog(
            onDismissRequest = { noteToMove = null },
            title = { Text("Move Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Move \"${note.title.ifBlank { "Untitled" }}\" to:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(
                        onClick = { destinationFolderId = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (destinationFolderId == null) "✓ Top level" else "Top level",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    allFolders
                        .sortedWith(compareBy<Folder> { folderDepth(it, allFolders) }.thenBy { it.name.lowercase() })
                        .forEach { folder ->
                            val indent = "  ".repeat(folderDepth(folder, allFolders).coerceAtMost(6))
                            val selectedPrefix = if (destinationFolderId == folder.id) "✓ " else ""
                            TextButton(
                                onClick = { destinationFolderId = folder.id },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "$selectedPrefix$indent${folder.name}",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.moveNote(note, destinationFolderId)
                        noteToMove = null
                    }
                ) {
                    Text("Move")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToMove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    val screenTitle = when {
        showingRecentlyDeleted -> "Recently Deleted"
        currentFolder != null -> currentFolder!!.name
        else -> mainListTitle
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    when {
                        showingRecentlyDeleted -> {
                            IconButton(onClick = { showingRecentlyDeleted = false }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                        currentFolder != null -> {
                            IconButton(onClick = { vm.navigateToParentFolder() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    }
                },
                actions = {
                    if (!showingRecentlyDeleted) {
                        if (selectionMode) {
                            TextButton(
                                onClick = {
                                    selectionMode = false
                                    selectedNoteIds = emptySet()
                                }
                            ) {
                                Text("Cancel")
                            }
                            TextButton(
                                onClick = {
                                    onExportSelectedNotes(notes.filter { selectedNoteIds.contains(it.id) })
                                    selectionMode = false
                                    selectedNoteIds = emptySet()
                                },
                                enabled = selectedNoteIds.isNotEmpty()
                            ) {
                                Text("Export")
                            }
                        } else {
                            TextButton(onClick = { vm.undoLastAction() }, enabled = canUndoActions) {
                                Text("Undo")
                            }
                            TextButton(onClick = { selectionMode = true }) {
                                Text("Select")
                            }
                            TextButton(onClick = { showingRecentlyDeleted = true }) {
                                Text("Deleted")
                            }
                            IconButton(onClick = { actionsMenuExpanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More actions")
                            }
                            DropdownMenu(
                                expanded = actionsMenuExpanded,
                                onDismissRequest = { actionsMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("New note") },
                                    onClick = {
                                        actionsMenuExpanded = false
                                        vm.createNote { created ->
                                            onNoteSelected(created)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("New folder") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.CreateNewFolder, contentDescription = null)
                                    },
                                    onClick = {
                                        actionsMenuExpanded = false
                                        showingCreateFolderDialog = true
                                    }
                                )
                                if (currentFolder == null) {
                                    DropdownMenuItem(
                                        text = { Text("Rename Main List") },
                                        onClick = {
                                            actionsMenuExpanded = false
                                            renameMainListTitle = mainListTitle
                                            showingRenameMainListDialog = true
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Appearance") },
                                    onClick = {
                                        actionsMenuExpanded = false
                                        appearanceMenuExpanded = true
                                    }
                                )
                            }
                            DropdownMenu(
                                expanded = appearanceMenuExpanded,
                                onDismissRequest = { appearanceMenuExpanded = false }
                            ) {
                                AppearanceSetting.entries.forEach { setting ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (setting == appearanceSetting) {
                                                    "✓ ${setting.label}"
                                                } else {
                                                    setting.label
                                                }
                                            )
                                        },
                                        onClick = {
                                            onAppearanceSettingChanged(setting)
                                            appearanceMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        val visibleNotes = if (showingRecentlyDeleted) recentlyDeletedNotes else notes

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (showingRecentlyDeleted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Deleted notes are kept here for 7 days.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val hasItems = if (showingRecentlyDeleted) {
                visibleNotes.isNotEmpty()
            } else {
                visibleFolders.isNotEmpty() || visibleNotes.isNotEmpty()
            }

            if (!hasItems) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (showingRecentlyDeleted) {
                            "No recently deleted notes."
                        } else {
                            "No notes yet. Use the menu to add a note or folder."
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (!showingRecentlyDeleted) {
                        items(visibleFolders, key = { "folder-${it.id}" }) { folder ->
                            FolderListRow(
                                folder = folder,
                                noteCount = folderActiveNoteCounts[folder.id] ?: 0,
                                selectionMode = selectionMode,
                                menuExpanded = activeFolderMenuId == folder.id,
                                onOpen = { vm.openFolder(folder) },
                                onOpenMenu = { activeFolderMenuId = folder.id },
                                onDismissMenu = {
                                    if (activeFolderMenuId == folder.id) activeFolderMenuId = null
                                },
                                onRename = {
                                    activeFolderMenuId = null
                                    renameFolderName = folder.name
                                    folderToRename = folder
                                },
                                onDelete = {
                                    activeFolderMenuId = null
                                    folderToDelete = folder
                                }
                            )
                        }
                    }

                    items(visibleNotes, key = { "note-${it.id}" }) { note ->
                        NoteListRow(
                            note = note,
                            showingRecentlyDeleted = showingRecentlyDeleted,
                            selectionMode = selectionMode,
                            selected = selectedNoteIds.contains(note.id),
                            onNoteSelected = onNoteSelected,
                            onSelectionToggled = { toggled ->
                                selectedNoteIds = if (selectedNoteIds.contains(toggled.id)) {
                                    selectedNoteIds - toggled.id
                                } else {
                                    selectedNoteIds + toggled.id
                                }
                            },
                            onPreviewRequested = { previewedNote = it },
                            onRestore = vm::restoreNote,
                            onPermanentDelete = vm::permanentlyDeleteNote
                        )
                    }
                }
            }
        }
    }

    previewedNote?.let { note ->
        NotePreviewDialog(
            note = note,
            onDismiss = { previewedNote = null },
            onTogglePinned = {
                vm.setNotePinned(note, !note.isPinned)
                previewedNote = null
            },
            onMove = {
                noteToMove = note
                previewedNote = null
            },
            onExport = {
                onExportSelectedNotes(listOf(note))
                previewedNote = null
            },
            onDelete = {
                vm.deleteNote(note)
                previewedNote = null
            }
        )
    }
}

@Composable
private fun FolderListRow(
    folder: Folder,
    noteCount: Int,
    selectionMode: Boolean,
    menuExpanded: Boolean,
    onOpen: () -> Unit,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Box(modifier = Modifier.padding(vertical = 4.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (!selectionMode) {
                            onOpen()
                        }
                    },
                    onLongClick = {
                        if (!selectionMode) {
                            onOpenMenu()
                        }
                    }
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(folder.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(
                    text = noteCount.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = onDismissMenu
        ) {
            DropdownMenuItem(text = { Text("Rename") }, onClick = onRename)
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun NoteListRow(
    note: Note,
    showingRecentlyDeleted: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    onNoteSelected: (Note?) -> Unit,
    onSelectionToggled: (Note) -> Unit,
    onPreviewRequested: (Note) -> Unit,
    onRestore: (Note) -> Unit,
    onPermanentDelete: (Note) -> Unit
) {
    val containerColor = if (note.isPinned) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    }

    Box(modifier = Modifier.padding(vertical = 4.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (!showingRecentlyDeleted) {
                            if (selectionMode) {
                                onSelectionToggled(note)
                            } else {
                                onNoteSelected(note)
                            }
                        }
                    },
                    onLongClick = {
                        if (!selectionMode && !showingRecentlyDeleted) {
                            onPreviewRequested(note)
                        }
                    }
                ),
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectionMode && !showingRecentlyDeleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { onSelectionToggled(note) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Selected", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        note.title.ifBlank { "Untitled" },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    note.content.take(120),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )

                if (showingRecentlyDeleted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { onRestore(note) }) {
                            Text("Restore")
                        }
                        TextButton(onClick = { onPermanentDelete(note) }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotePreviewDialog(
    note: Note,
    onDismiss: () -> Unit,
    onTogglePinned: () -> Unit,
    onMove: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(1f)
                .testTag("note-preview-dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = note.content.ifBlank { "No content yet" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onTogglePinned) {
                        Text(if (note.isPinned) "Unpin" else "Pin")
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = onMove) {
                        Text("Move to folder")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onExport) {
                        Text("Export")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

private fun folderDepth(folder: Folder, allFolders: List<Folder>): Int {
    val foldersById = allFolders.associateBy { it.id }
    var depth = 0
    var cursor = folder.parentFolderId
    while (cursor != null) {
        depth += 1
        cursor = foldersById[cursor]?.parentFolderId
    }
    return depth
}
