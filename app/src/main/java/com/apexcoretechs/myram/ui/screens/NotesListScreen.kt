package com.apexcoretechs.myram.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.apexcoretechs.myram.data.Folder
import com.apexcoretechs.myram.data.Note
import com.apexcoretechs.myram.data.PinnedText
import com.apexcoretechs.myram.ui.components.ChromeActionBar
import com.apexcoretechs.myram.ui.components.computeTopBarLayout
import com.apexcoretechs.myram.ui.NotesViewModel
import com.apexcoretechs.myram.ui.richtext.plainTextFromStoredContent
import com.apexcoretechs.myram.ui.theme.AppearanceSetting
import com.apexcoretechs.myram.ui.theme.EditorChromeStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    vm: NotesViewModel,
    appearanceSetting: AppearanceSetting,
    onAppearanceSettingChanged: (AppearanceSetting) -> Unit,
    editorChromeStyle: EditorChromeStyle,
    onEditorChromeStyleChanged: (EditorChromeStyle) -> Unit,
    onExportSelectedNotes: (List<Note>) -> Unit,
    onNoteSelected: (Note?) -> Unit
) {
    val topBarControlSize = 44.dp
    val topBarIconSize = 24.dp
    val hintIconSize = 16.dp
    val notes by vm.visibleNotes.collectAsState()
    val pinnedTextByNoteId by vm.pinnedTextByNoteId.collectAsState()
    val visibleFolders by vm.visibleFolders.collectAsState()
    val folderActiveNoteCounts by vm.folderActiveNoteCounts.collectAsState()
    val allFolders by vm.allFolders.collectAsState()
    val currentFolder by vm.currentFolder.collectAsState()
    val currentFolderId by vm.currentFolderId.collectAsState()
    val recentlyDeletedNotes by vm.recentlyDeletedNotes.collectAsState()
    val mainListTitle by vm.mainListTitle.collectAsState()
    val canUndoActions by vm.canUndoActions.collectAsState()
    val canRedoActions by vm.canRedoActions.collectAsState()
    val haptic = LocalHapticFeedback.current

    var actionsMenuExpanded by remember { mutableStateOf(false) }
    var historyMenuExpanded by remember { mutableStateOf(false) }
    var showingRecentlyDeleted by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedNoteIds by remember { mutableStateOf(setOf<Int>()) }
    var showingCreateFolderDialog by remember { mutableStateOf(false) }
    var createFolderName by remember { mutableStateOf("") }
    var folderToRename by remember { mutableStateOf<Folder?>(null) }
    var renameFolderName by remember { mutableStateOf("") }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }
    var noteToMove by remember { mutableStateOf<Note?>(null) }
    var notesToMoveBatch by remember { mutableStateOf<List<Note>>(emptyList()) }
    var previewedNote by remember { mutableStateOf<Note?>(null) }
    var showingBulkActions by remember { mutableStateOf(false) }
    var showingRenameMainListDialog by remember { mutableStateOf(false) }
    var showingAppearanceDialog by remember { mutableStateOf(false) }
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

    if (showingAppearanceDialog) {
        AlertDialog(
            onDismissRequest = { showingAppearanceDialog = false },
            title = { Text("Appearance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Mode", style = MaterialTheme.typography.labelLarge)
                    AppearanceSetting.entries.forEach { setting ->
                        TextButton(
                            onClick = {
                                onAppearanceSettingChanged(setting)
                            },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                        ) {
                            Text(if (setting == appearanceSetting) "✓ ${setting.label}" else setting.label)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Style", style = MaterialTheme.typography.labelLarge)
                    EditorChromeStyle.entries.forEach { style ->
                        TextButton(
                            onClick = {
                                onEditorChromeStyleChanged(style)
                            },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                        ) {
                            Text(if (style == editorChromeStyle) "✓ ${style.label}" else style.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showingAppearanceDialog = false }) { Text("Done") }
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

    if (notesToMoveBatch.isNotEmpty()) {
        var destinationFolderId by remember(notesToMoveBatch.map { it.id }) { mutableStateOf<Int?>(null) }
        AlertDialog(
            onDismissRequest = { notesToMoveBatch = emptyList() },
            title = { Text("Move Selected Notes") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Move ${notesToMoveBatch.size} notes to:",
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
                        notesToMoveBatch.forEach { note -> vm.moveNote(note, destinationFolderId) }
                        notesToMoveBatch = emptyList()
                        showingBulkActions = false
                    }
                ) {
                    Text("Move")
                }
            },
            dismissButton = {
                TextButton(onClick = { notesToMoveBatch = emptyList() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showingBulkActions) {
        AlertDialog(
            onDismissRequest = { showingBulkActions = false },
            title = { Text("${selectedNoteIds.size} selected") },
            text = { Text("Choose an action for selected notes.") },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            notes.filter { selectedNoteIds.contains(it.id) }
                                .forEach { note -> vm.setNotePinned(note, true) }
                            showingBulkActions = false
                        }
                    ) {
                        Text("Pin")
                    }
                    TextButton(
                        onClick = {
                            notes.filter { selectedNoteIds.contains(it.id) }
                                .forEach { note -> vm.setNotePinned(note, false) }
                            showingBulkActions = false
                        }
                    ) {
                        Text("Unpin")
                    }
                    TextButton(
                        onClick = {
                            onExportSelectedNotes(notes.filter { selectedNoteIds.contains(it.id) })
                            showingBulkActions = false
                        }
                    ) {
                        Text("Export")
                    }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            notesToMoveBatch = notes.filter { selectedNoteIds.contains(it.id) }
                            showingBulkActions = false
                        },
                        enabled = selectedNoteIds.isNotEmpty()
                    ) {
                        Text("Move")
                    }
                    TextButton(
                        onClick = {
                            notes.filter { selectedNoteIds.contains(it.id) }
                                .forEach(vm::deleteNote)
                            showingBulkActions = false
                            selectionMode = false
                            selectedNoteIds = emptySet()
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showingBulkActions = false }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    val screenTitle = when {
        showingRecentlyDeleted -> "Recently Deleted"
        currentFolder != null -> currentFolder!!.name
        else -> mainListTitle
    }

    val topBarActionSpecs = remember(canUndoActions, canRedoActions) {
        listOf(
            ListTopBarActionSpec("history", Icons.AutoMirrored.Filled.Undo, enabled = canUndoActions || canRedoActions) {},
            ListTopBarActionSpec("new-note", Icons.Filled.Edit, enabled = true) {
                vm.createNote { created -> onNoteSelected(created) }
            },
            ListTopBarActionSpec("new-folder", Icons.Filled.CreateNewFolder, enabled = true) {
                showingCreateFolderDialog = true
            },
            ListTopBarActionSpec("recently-deleted", Icons.Filled.Delete, enabled = true) {
                showingRecentlyDeleted = true
            }
        )
    }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    Scaffold(topBar = {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val totalWidthPx = with(density) { maxWidth.toPx() } - with(density) { 20.dp.toPx() }
            val iconUnitPx = with(density) { topBarControlSize.toPx() }
            val spacingPx = with(density) { 8.dp.toPx() }
            val backVisible = showingRecentlyDeleted || currentFolder != null
            val selectVisible = !showingRecentlyDeleted
            val leadingUnits = (if (backVisible) 1 else 0) + (if (selectVisible) 1 else 0)
            val leadingWidthPx = if (leadingUnits == 0) 0f else {
                (leadingUnits * iconUnitPx) + ((leadingUnits - 1) * spacingPx)
            }

            val titleTextLayout = textMeasurer.measure(
                text = screenTitle,
                style = MaterialTheme.typography.titleMedium
            )
            val editableTitle = !showingRecentlyDeleted
            val titleWidthPx =
                titleTextLayout.size.width.toFloat() + if (editableTitle) with(density) { 24.dp.toPx() } else 0f

            val layout = computeTopBarLayout(
                totalWidthPx = totalWidthPx,
                leadingWidthPx = leadingWidthPx,
                titleWidthPx = titleWidthPx,
                actionCount = if (showingRecentlyDeleted || selectionMode) 0 else topBarActionSpecs.size + 1,
                actionWidthPx = iconUnitPx,
                spacingPx = spacingPx,
                overflowButtonWidthPx = iconUnitPx
            )

            ChromeActionBar(style = editorChromeStyle, modifier = Modifier.fillMaxWidth()) {
                if (backVisible) {
                    IconButton(
                        onClick = {
                            if (showingRecentlyDeleted) showingRecentlyDeleted = false else vm.navigateToParentFolder()
                        },
                        modifier = Modifier.size(topBarControlSize)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(topBarIconSize)
                        )
                    }
                }
                if (selectVisible) {
                    IconButton(
                        onClick = {
                            selectionMode = !selectionMode
                            if (!selectionMode) selectedNoteIds = emptySet()
                        },
                        modifier = Modifier.size(topBarControlSize)
                    ) {
                        Icon(
                            imageVector = if (selectionMode) {
                                Icons.Filled.CheckBox
                            } else {
                                Icons.Outlined.CheckBoxOutlineBlank
                            },
                            contentDescription = if (selectionMode) "Finish selecting notes" else "Select notes",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(topBarIconSize)
                        )
                    }
                }

                if (editableTitle) {
                    TextButton(onClick = {
                        if (currentFolder == null) {
                            renameMainListTitle = mainListTitle
                            showingRenameMainListDialog = true
                        } else {
                            currentFolder?.let { folder ->
                                renameFolderName = folder.name
                                folderToRename = folder
                            }
                        }
                    },
                        modifier = Modifier
                            .height(topBarControlSize)
                            .testTag(if (currentFolder == null) "edit-main-list-title" else "edit-folder-title"),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                screenTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = if (layout.ellipsizeTitle) TextOverflow.Ellipsis else TextOverflow.Clip
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = if (currentFolder == null) "Edit main list title" else "Edit folder title",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(hintIconSize)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .weight(1f, fill = false),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = screenTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = if (layout.ellipsizeTitle) TextOverflow.Ellipsis else TextOverflow.Clip
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                if (selectionMode && !showingRecentlyDeleted) {
                    IconButton(
                        onClick = { showingBulkActions = true },
                        enabled = selectedNoteIds.isNotEmpty(),
                        modifier = Modifier.size(topBarControlSize)
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Selection actions",
                            modifier = Modifier.size(topBarIconSize),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (!showingRecentlyDeleted) {
                    val visiblePromotable = (layout.visibleActionCount - 1).coerceAtLeast(0)
                    topBarActionSpecs.take(visiblePromotable).forEach { spec ->
                        if (spec.id == "history") {
                            Box {
                                IconButton(
                                    onClick = { historyMenuExpanded = true },
                                    enabled = spec.enabled,
                                    modifier = Modifier.size(topBarControlSize)
                                ) {
                                    Icon(
                                        spec.icon,
                                        contentDescription = spec.id,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(topBarIconSize)
                                    )
                                }
                                DropdownMenu(
                                    expanded = historyMenuExpanded,
                                    onDismissRequest = { historyMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Undo") },
                                        enabled = canUndoActions,
                                        onClick = {
                                            historyMenuExpanded = false
                                            vm.undoLastAction()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Redo") },
                                        enabled = canRedoActions,
                                        onClick = {
                                            historyMenuExpanded = false
                                            vm.redoLastAction()
                                        }
                                    )
                                }
                            }
                        } else {
                            IconButton(
                                onClick = spec.onClick,
                                enabled = spec.enabled,
                                modifier = Modifier.size(topBarControlSize)
                            ) {
                                Icon(
                                    spec.icon,
                                    contentDescription = spec.id,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(topBarIconSize)
                                )
                            }
                        }
                    }

                    Box {
                        DropdownMenu(
                            expanded = historyMenuExpanded && topBarActionSpecs.take(visiblePromotable).none { it.id == "history" },
                            onDismissRequest = { historyMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Undo") },
                                enabled = canUndoActions,
                                onClick = {
                                    historyMenuExpanded = false
                                    vm.undoLastAction()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Redo") },
                                enabled = canRedoActions,
                                onClick = {
                                    historyMenuExpanded = false
                                    vm.redoLastAction()
                                }
                            )
                        }
                        IconButton(
                            onClick = { actionsMenuExpanded = true },
                            modifier = Modifier.size(topBarControlSize)
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "More actions",
                                modifier = Modifier.size(topBarIconSize),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = actionsMenuExpanded,
                            onDismissRequest = { actionsMenuExpanded = false }
                        ) {
                            topBarActionSpecs.drop(visiblePromotable).forEach { spec ->
                                DropdownMenuItem(
                                    text = { Text(spec.menuLabel) },
                                    onClick = {
                                        actionsMenuExpanded = false
                                        if (spec.id == "history") {
                                            historyMenuExpanded = true
                                        } else if (spec.enabled) {
                                            spec.onClick()
                                        }
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Appearance") },
                                onClick = {
                                    actionsMenuExpanded = false
                                    showingAppearanceDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }) { padding ->
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
                            pinnedTextItems = pinnedTextByNoteId[note.id].orEmpty(),
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
                            onLongPress = { pressed ->
                                if (showingRecentlyDeleted) return@NoteListRow
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val selectedCount = selectedNoteIds.size
                                val isSelected = selectedNoteIds.contains(pressed.id)
                                when {
                                    selectedCount == 0 -> {
                                        previewedNote = pressed
                                    }
                                    selectedCount == 1 && isSelected -> {
                                        previewedNote = pressed
                                    }
                                    selectedCount > 1 && isSelected -> {
                                        showingBulkActions = true
                                    }
                                    else -> {
                                        previewedNote = pressed
                                    }
                                }
                            },
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
            pinnedTextItems = pinnedTextByNoteId[note.id].orEmpty(),
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
    pinnedTextItems: List<PinnedText>,
    showingRecentlyDeleted: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    onNoteSelected: (Note?) -> Unit,
    onSelectionToggled: (Note) -> Unit,
    onLongPress: (Note) -> Unit,
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
                        if (!showingRecentlyDeleted) onLongPress(note)
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

                pinnedTextPreviewLine(pinnedTextItems)?.let { preview ->
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("note-row-pinned-text")
                    )
                }

                Text(
                    plainTextFromStoredContent(note.content).take(120),
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
    pinnedTextItems: List<PinnedText>,
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
                pinnedTextPreviewLine(pinnedTextItems)?.let { preview ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = preview,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = plainTextFromStoredContent(note.content).ifBlank { "No content yet" },
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

private fun pinnedTextPreviewLine(pinnedTextItems: List<PinnedText>): String? {
    return pinnedTextItems
        .sortedWith(compareBy<PinnedText> { it.sortOrder }.thenBy { it.createdAt })
        .firstOrNull()
        ?.text
        ?.lineSequence()
        ?.firstOrNull()
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}

private data class ListTopBarActionSpec(
    val id: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val enabled: Boolean,
    val onClick: () -> Unit
) {
    val menuLabel: String
        get() = when (id) {
            "history" -> "History"
            "new-note" -> "New note"
            "new-folder" -> "New folder"
            "recently-deleted" -> "Recently Deleted"
            else -> id
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
