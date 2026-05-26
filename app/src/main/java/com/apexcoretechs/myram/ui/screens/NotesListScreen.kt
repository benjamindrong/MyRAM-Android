package com.apexcoretechs.myram.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apexcoretechs.myram.data.Note
import com.apexcoretechs.myram.ui.NotesViewModel
import com.apexcoretechs.myram.ui.theme.AppearanceSetting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    vm: NotesViewModel,
    appearanceSetting: AppearanceSetting,
    onAppearanceSettingChanged: (AppearanceSetting) -> Unit,
    onNoteSelected: (Note?) -> Unit
) {
    val notes by vm.allNotes.collectAsState()
    val recentlyDeletedNotes by vm.recentlyDeletedNotes.collectAsState()
    var appearanceMenuExpanded by remember { mutableStateOf(false) }
    var showingRecentlyDeleted by remember { mutableStateOf(false) }

    LaunchedEffect(showingRecentlyDeleted) {
        if (showingRecentlyDeleted) {
            vm.refreshRecentlyDeletedNotes()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showingRecentlyDeleted) "Recently Deleted" else "My Notes") },
                navigationIcon = {
                    if (showingRecentlyDeleted) {
                        TextButton(onClick = { showingRecentlyDeleted = false }) {
                            Text("Back")
                        }
                    }
                },
                actions = {
                    if (!showingRecentlyDeleted) {
                        TextButton(onClick = { showingRecentlyDeleted = true }) {
                            Text("Deleted")
                        }

                        Box {
                            TextButton(onClick = { appearanceMenuExpanded = true }) {
                                Text("Appearance")
                            }
                            DropdownMenu(
                                expanded = appearanceMenuExpanded,
                                onDismissRequest = { appearanceMenuExpanded = false }
                            ) {
                                AppearanceSetting.entries.forEach { setting ->
                                    DropdownMenuItem(
                                        text = { Text(setting.label) },
                                        onClick = {
                                            onAppearanceSettingChanged(setting)
                                            appearanceMenuExpanded = false
                                        },
                                        trailingIcon = {
                                            if (setting == appearanceSetting) {
                                                Text("✓")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showingRecentlyDeleted) {
                FloatingActionButton(
                    onClick = {
                        vm.createNote { created ->
                            onNoteSelected(created)
                        }
                    }
                ) {
                    Text("+")
                }
            }
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

            if (visibleNotes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (showingRecentlyDeleted) {
                            "No recently deleted notes."
                        } else {
                            "No notes yet. Tap + to create one."
                        }
                    )
                }
            } else {
                LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                ) {
                    items(visibleNotes, key = { it.id }) { note ->
                        NoteListRow(
                            note = note,
                            showingRecentlyDeleted = showingRecentlyDeleted,
                            onNoteSelected = onNoteSelected,
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
private fun NoteListRow(
    note: Note,
    showingRecentlyDeleted: Boolean,
    onNoteSelected: (Note?) -> Unit,
    onSoftDelete: (Note) -> Unit,
    onRestore: (Note) -> Unit,
    onPermanentDelete: (Note) -> Unit
) {
    val cardShape = MaterialTheme.shapes.medium
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.25f },
        confirmValueChange = { value ->
            when {
                !showingRecentlyDeleted && value != SwipeToDismissBoxValue.Settled -> {
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
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val isRestore = showingRecentlyDeleted && direction == SwipeToDismissBoxValue.StartToEnd
            val label = when {
                isRestore -> "Restore"
                showingRecentlyDeleted -> "Delete"
                else -> "Delete"
            }
            val color = when {
                isRestore -> MaterialTheme.colorScheme.primary
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
                    onNoteSelected(note)
                },
            shape = cardShape
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
