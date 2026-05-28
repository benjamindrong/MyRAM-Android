package com.apexcoretechs.myram.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val allFolders by vm.allFolders.collectAsState()
    val currentFolder by vm.currentFolder.collectAsState()
    val currentFolderId by vm.currentFolderId.collectAsState()
    val recentlyDeletedNotes by vm.recentlyDeletedNotes.collectAsState()

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

    LaunchedEffect(showingRecentlyDeleted, currentFolderId) {
        selectionMode = false
        selectedNoteIds = emptySet()
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
        else -> "My Notes"
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
                            TextButton(onClick = { selectionMode = true }) {
                                Text("Select")
                            }
                            TextButton(onClick = { showingRecentlyDeleted = true }) {
                                Text("Deleted")
                            }
                            IconButton(
                                onClick = { actionsMenuExpanded = true }
                            ) {
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
                                        Icon(
                                            Icons.Filled.CreateNewFolder,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        actionsMenuExpanded = false
                                        showingCreateFolderDialog = true
                                    }
                                )
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
                                selectionMode = selectionMode,
                                onOpen = { vm.openFolder(folder) },
                                onRename = {
                                    renameFolderName = folder.name
                                    folderToRename = folder
                                },
                                onDelete = { folderToDelete = folder }
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
                            onMove = { noteToMove = note },
                            onSoftDelete = vm::deleteNote,
                            onRestore = vm::restoreNote,
                            onPermanentDelete = vm::permanentlyDeleteNote
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderListRow(
    folder: Folder,
    selectionMode: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val cardShape = MaterialTheme.shapes.medium
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.25f },
        confirmValueChange = { value ->
            if (selectionMode) return@rememberSwipeToDismissBoxState false
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onRename()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        modifier = Modifier.padding(vertical = 4.dp),
        state = dismissState,
        enableDismissFromStartToEnd = !selectionMode,
        enableDismissFromEndToStart = !selectionMode,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val label = if (direction == SwipeToDismissBoxValue.StartToEnd) "Rename" else "Delete"
            val color = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
            val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                Alignment.CenterStart
            } else {
                Alignment.CenterEnd
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(cardShape)
                    .background(color),
                contentAlignment = alignment
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !selectionMode, onClick = onOpen),
            shape = cardShape
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Folder, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(folder.name, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteListRow(
    note: Note,
    showingRecentlyDeleted: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    onNoteSelected: (Note?) -> Unit,
    onSelectionToggled: (Note) -> Unit,
    onMove: (Note) -> Unit,
    onSoftDelete: (Note) -> Unit,
    onRestore: (Note) -> Unit,
    onPermanentDelete: (Note) -> Unit
) {
    val cardShape = MaterialTheme.shapes.medium
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.25f },
        confirmValueChange = { value ->
            if (selectionMode) return@rememberSwipeToDismissBoxState false
            when {
                !showingRecentlyDeleted && value == SwipeToDismissBoxValue.StartToEnd -> {
                    onMove(note)
                    false
                }
                !showingRecentlyDeleted && value == SwipeToDismissBoxValue.EndToStart -> {
                    onSoftDelete(note)
                    true
                }
                showingRecentlyDeleted && value == SwipeToDismissBoxValue.StartToEnd -> {
                    onRestore(note)
                    true
                }
                showingRecentlyDeleted && value == SwipeToDismissBoxValue.EndToStart -> {
                    onPermanentDelete(note)
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        modifier = Modifier.padding(vertical = 4.dp),
        state = dismissState,
        enableDismissFromStartToEnd = !selectionMode,
        enableDismissFromEndToStart = !selectionMode,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val isRestore = showingRecentlyDeleted && direction == SwipeToDismissBoxValue.StartToEnd
            val isMove = !showingRecentlyDeleted && direction == SwipeToDismissBoxValue.StartToEnd
            val label = when {
                isRestore -> "Restore"
                isMove -> "Move"
                else -> "Delete"
            }
            val color = when {
                isRestore || isMove -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.error
            }
            val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                Alignment.CenterStart
            } else {
                Alignment.CenterEnd
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(cardShape)
                    .background(color),
                contentAlignment = alignment
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !showingRecentlyDeleted) {
                    if (selectionMode) {
                        onSelectionToggled(note)
                    } else {
                        onNoteSelected(note)
                    }
                },
            shape = cardShape
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
                Text(note.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium)
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
