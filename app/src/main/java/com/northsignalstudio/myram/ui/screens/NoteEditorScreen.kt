package com.northsignalstudio.myram.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StrikethroughS
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewModelScope
import com.northsignalstudio.myram.data.Note
import com.northsignalstudio.myram.data.NotePhotoAttachment
import com.northsignalstudio.myram.data.PinnedText
import com.northsignalstudio.myram.ui.components.ChromeActionBar
import com.northsignalstudio.myram.ui.components.computeTopBarLayout
import com.northsignalstudio.myram.ui.NotesViewModel
import com.northsignalstudio.myram.ui.richtext.RichTextEditor
import com.northsignalstudio.myram.ui.richtext.RichTextEditorActions
import com.northsignalstudio.myram.ui.richtext.RichTextFormatState
import com.northsignalstudio.myram.ui.richtext.plainTextFromStoredContent
import com.northsignalstudio.myram.ui.theme.EditorChromeStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val editorColorSwatches = listOf(
    Color(0xFF1F2933),
    Color(0xFFB91C1C),
    Color(0xFFB45309),
    Color(0xFF047857),
    Color(0xFF1D4ED8),
    Color(0xFF6D28D9)
)

@Composable
private fun AttachmentThumbnail(
    attachment: NotePhotoAttachment,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val imageBitmap = remember(attachment.id) {
        BitmapFactory.decodeByteArray(
            attachment.imageData,
            0,
            attachment.imageData.size
        )?.asImageBitmap()
    }

    Box {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Attached image",
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpen)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "IMG",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(18.dp)
                .clickable(onClick = onRemove),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.65f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove attachment",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun AttachmentViewerDialog(
    attachment: NotePhotoAttachment,
    onDismiss: () -> Unit
) {
    val imageBitmap = remember(attachment.id) {
        BitmapFactory.decodeByteArray(
            attachment.imageData,
            0,
            attachment.imageData.size
        )?.asImageBitmap()
    }

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (imageBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(attachment.id) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                if (newScale == 1f) {
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                }
                                scale = newScale
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Full size attachment",
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun RichTextActionBars(
    actions: RichTextEditorActions?,
    formatState: RichTextFormatState,
    showingFormattingControls: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleFormattingControls: () -> Unit,
    keyboardVisible: Boolean,
    onToggleKeyboard: () -> Unit,
    onSetFontSize: (Int) -> Unit,
    onPinSelection: () -> Unit,
    chromeStyle: EditorChromeStyle,
    modifier: Modifier = Modifier
) {
    var sizeMenuExpanded by remember { mutableStateOf(false) }
    var historyMenuExpanded by remember { mutableStateOf(false) }
    var pasteMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.End
    ) {
        if (showingFormattingControls) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.testTag("keyboard-control-overflow-panel"))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ToggleFormatIcon(
                        icon = Icons.Filled.FormatBold,
                        label = "Bold",
                        selected = formatState.bold,
                        onClick = { actions?.toggleBold() }
                    )
                    ToggleFormatIcon(
                        icon = Icons.Filled.FormatItalic,
                        label = "Italic",
                        selected = formatState.italic,
                        onClick = { actions?.toggleItalic() }
                    )
                    ToggleFormatIcon(
                        icon = Icons.Filled.FormatUnderlined,
                        label = "Underline",
                        selected = formatState.underline,
                        onClick = { actions?.toggleUnderline() }
                    )
                    ToggleFormatIcon(
                        icon = Icons.Filled.StrikethroughS,
                        label = "Strikethrough",
                        selected = formatState.strikethrough,
                        onClick = { actions?.toggleStrikethrough() }
                    )
                    Box {
                        ToggleFormatIcon(
                            icon = Icons.Filled.TextFields,
                            label = "Font size",
                            selected = false,
                            onClick = { sizeMenuExpanded = true }
                        )
                        DropdownMenu(
                            expanded = sizeMenuExpanded,
                            onDismissRequest = { sizeMenuExpanded = false }
                        ) {
                            listOf(14, 16, 18, 20, 24).forEach { size ->
                                DropdownMenuItem(
                                    text = { Text(if (formatState.fontSizeSp == size) "✓ ${size}sp" else "${size}sp") },
                                    onClick = {
                                        sizeMenuExpanded = false
                                        onSetFontSize(size)
                                    }
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { actions?.clearColor() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = if (formatState.textColor == MaterialTheme.colorScheme.onSurface.toArgb()) 4.dp else 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "A",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                    editorColorSwatches.forEach { swatch ->
                        val selected = formatState.textColor == swatch.toArgb()
                        Surface(
                            modifier = Modifier
                                .size(if (selected) 20.dp else 18.dp)
                                .clickable { actions?.applyColor(swatch) },
                            shape = CircleShape,
                            color = swatch,
                            tonalElevation = if (selected) 4.dp else 0.dp
                        ) {}
                    }
                }
            }
        }

        ChromeActionBar(
            style = chromeStyle,
            modifier = Modifier
                .testTag("keyboard-control-bar")
        ) {
            ToggleFormatIcon(
                icon = if (keyboardVisible) Icons.Filled.KeyboardHide else Icons.Filled.Keyboard,
                label = "Keyboard toggle",
                selected = false,
                onClick = onToggleKeyboard
            )
            Box {
                ToggleFormatIcon(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    label = "History",
                    selected = false,
                    enabled = canUndo || canRedo,
                    onClick = { historyMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = historyMenuExpanded,
                    onDismissRequest = { historyMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Undo") },
                        enabled = canUndo,
                        onClick = {
                            historyMenuExpanded = false
                            onUndo()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Redo") },
                        enabled = canRedo,
                        onClick = {
                            historyMenuExpanded = false
                            onRedo()
                        }
                    )
                }
            }
            ToggleFormatIcon(icon = Icons.Filled.ContentCut, label = "Cut", selected = false, onClick = { actions?.cutSelection() })
            ToggleFormatIcon(icon = Icons.Filled.ContentCopy, label = "Copy", selected = false, onClick = { actions?.copySelection() })
            Box {
                ToggleFormatIcon(
                    icon = Icons.Filled.ContentPaste,
                    label = "Paste",
                    selected = false,
                    onClick = { pasteMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = pasteMenuExpanded,
                    onDismissRequest = { pasteMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Paste") },
                        onClick = {
                            pasteMenuExpanded = false
                            actions?.pasteClipboard()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Paste and Match Destination Formatting") },
                        onClick = {
                            pasteMenuExpanded = false
                            actions?.pasteClipboardMatchingDestinationFormatting()
                        }
                    )
                }
            }
            ToggleFormatIcon(icon = Icons.Filled.SelectAll, label = "Select all", selected = false, onClick = { actions?.toggleSelectAll() })
            ToggleFormatIcon(icon = Icons.Filled.CheckBox, label = "Checklist", selected = false, onClick = { actions?.toggleChecklistItem() })
            ToggleFormatIcon(
                icon = Icons.Filled.PushPin,
                label = "Pin",
                selected = false,
                onClick = onPinSelection,
                modifier = Modifier.testTag("keyboard-control-pin")
            )
            ToggleFormatIcon(
                icon = if (showingFormattingControls) Icons.Filled.Close else Icons.Filled.MoreVert,
                label = "Formatting menu",
                selected = showingFormattingControls,
                onClick = onToggleFormattingControls,
                modifier = Modifier.testTag("keyboard-control-overflow-toggle")
            )
        }
    }
}

@Composable
private fun ToggleFormatIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(onClick = onClick, modifier = modifier.size(30.dp), enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = if (enabled) tint else tint.copy(alpha = 0.45f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    vm: NotesViewModel,
    note: Note?,
    editorChromeStyle: EditorChromeStyle,
    onNoteChanged: (Note) -> Unit,
    onShareNote: (Note) -> Unit,
    onBack: () -> Unit
) {
    val topBarControlSize = 44.dp
    val topBarIconSize = 24.dp
    val hintIconSize = 16.dp
    var title by remember(note?.id) { mutableStateOf(TextFieldValue(note?.title ?: "")) }
    var storedContent by remember(note?.id) { mutableStateOf(note?.content ?: "") }
    var plainContent by remember(note?.id) { mutableStateOf(plainTextFromStoredContent(note?.content ?: "")) }
    var undoHistory by remember(note?.id) { mutableStateOf<List<EditorSnapshot>>(emptyList()) }
    var redoHistory by remember(note?.id) { mutableStateOf<List<EditorSnapshot>>(emptyList()) }
    var pendingUndoSnapshot by remember(note?.id) { mutableStateOf<EditorSnapshot?>(null) }
    var saveJob by remember { mutableStateOf<Job?>(null) }
    var undoJob by remember { mutableStateOf<Job?>(null) }
    var hasEditedCurrentNote by remember(note?.id) { mutableStateOf(false) }
    var isRestoringUndo by remember(note?.id) { mutableStateOf(false) }
    var isAttachmentMenuExpanded by remember { mutableStateOf(false) }
    var showingCreateFolderPrompt by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var areAttachmentsExpanded by remember(note?.id) { mutableStateOf(false) }
    var arePinnedExpanded by remember(note?.id) { mutableStateOf(vm.isPinnedTextSectionExpanded(note?.id)) }
    var expandedAttachmentId by remember(note?.id) { mutableStateOf<Long?>(null) }
    var previousAttachmentCount by remember(note?.id) { mutableIntStateOf(0) }
    var showingTitleEditor by remember(note?.id) { mutableStateOf(false) }
    var titleDraft by remember(note?.id) { mutableStateOf(title.text) }
    var showingFormattingControls by remember(note?.id) { mutableStateOf(false) }
    var editorActions by remember(note?.id) { mutableStateOf<RichTextEditorActions?>(null) }
    var keyboardVisible by remember(note?.id) { mutableStateOf(false) }
    var formatState by remember(note?.id) {
                mutableStateOf(
                    RichTextFormatState(
                        bold = false,
                        italic = false,
                        underline = false,
                        strikethrough = false,
                        textColor = Color.Black.toArgb(),
                        fontSizeSp = 16
                    )
                )
    }

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val editorFocusRequester = remember { FocusRequester() }

    val attachments by vm.noteAttachments(note?.id).collectAsState(initial = emptyList())
    val pinnedTextItems by vm.pinnedText(note?.id).collectAsState(initial = emptyList())
    val canUndoActions by vm.canUndoActions.collectAsState()
    val expandedAttachment = remember(attachments, expandedAttachmentId) {
        attachments.firstOrNull { it.id == expandedAttachmentId }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        val noteId = note?.id ?: return@rememberLauncherForActivityResult
        if (uris.isNotEmpty()) {
            vm.addPhotoAttachments(noteId, uris)
        }
    }

    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val noteId = note?.id ?: return@rememberLauncherForActivityResult
        if (uris.isNotEmpty()) {
            vm.addPhotoAttachments(noteId, uris)
        }
    }

    fun currentSnapshot() = EditorSnapshot(
        title = title,
        storedContent = storedContent,
        plainContent = plainContent,
        pinnedTextItems = pinnedTextItems
    )

    fun pushUndoSnapshot(snapshot: EditorSnapshot) {
        if (snapshot == currentSnapshot()) return
        undoHistory = (undoHistory + snapshot).takeLast(200)
    }

    fun flushPendingUndoSnapshot() {
        undoJob?.cancel()
        pendingUndoSnapshot?.let { snapshot ->
            pushUndoSnapshot(snapshot)
            note?.let { current ->
                vm.recordTextUndoSnapshot(
                    note = current,
                    previousTitle = snapshot.title.text,
                    previousContent = snapshot.storedContent
                )
            }
        }
        pendingUndoSnapshot = null
    }

    fun scheduleUndoSnapshot(snapshot: EditorSnapshot) {
        if (isRestoringUndo) return
        if (pendingUndoSnapshot == null) {
            pendingUndoSnapshot = snapshot
        }
        redoHistory = emptyList()
        undoJob?.cancel()
        undoJob = scope.launch {
            delay(800)
            pendingUndoSnapshot?.let(::pushUndoSnapshot)
            pendingUndoSnapshot = null
        }
    }

    fun undoLastEdit() {
        flushPendingUndoSnapshot()
        val snapshot = undoHistory.lastOrNull()
        if (snapshot != null) {
            val current = currentSnapshot()
            undoHistory = undoHistory.dropLast(1)
            redoHistory = redoHistory + current
            isRestoringUndo = true
            hasEditedCurrentNote = true
            title = snapshot.title
            storedContent = snapshot.storedContent
            plainContent = snapshot.plainContent
            note?.let { vm.replacePinnedText(it, snapshot.pinnedTextItems) }
            isRestoringUndo = false
        } else {
            vm.undoLastAction()
        }
    }

    fun redoLastEdit() {
        flushPendingUndoSnapshot()
        val snapshot = redoHistory.lastOrNull() ?: return
        val current = currentSnapshot()
        redoHistory = redoHistory.dropLast(1)
        undoHistory = (undoHistory + current).takeLast(200)
        isRestoringUndo = true
        hasEditedCurrentNote = true
        title = snapshot.title
        storedContent = snapshot.storedContent
        plainContent = snapshot.plainContent
        note?.let { vm.replacePinnedText(it, snapshot.pinnedTextItems) }
        isRestoringUndo = false
    }

    fun markContentEdited(newStored: String) {
        if (newStored == storedContent) return
        scheduleUndoSnapshot(currentSnapshot())
        hasEditedCurrentNote = true
        storedContent = newStored
    }

    fun saveTitleEdit() {
        val trimmed = titleDraft.trim()
        if (trimmed == title.text) return
        scheduleUndoSnapshot(currentSnapshot())
        hasEditedCurrentNote = true
        title = title.copy(text = trimmed, selection = TextRange(trimmed.length))
    }

    LaunchedEffect(note?.id, title.text, storedContent) {
        saveJob?.cancel()
        if (!hasEditedCurrentNote) return@LaunchedEffect
        saveJob = vm.viewModelScope.launch {
            delay(800)
            note?.let { current ->
                val updated = current.copy(
                    title = title.text,
                    content = storedContent
                )
                vm.updateNote(updated)
            }
        }
    }

    LaunchedEffect(note?.id, attachments.size) {
        if (attachments.size > previousAttachmentCount) {
            areAttachmentsExpanded = true
        }
        previousAttachmentCount = attachments.size
        val current = note
        if (current != null) {
            vm.refreshNoteSuggestions(
                note = current,
                draftTitle = title.text,
                draftContent = plainContent
            )
        }
    }

    LaunchedEffect(note?.id) {
        arePinnedExpanded = vm.isPinnedTextSectionExpanded(note?.id)
    }

    LaunchedEffect(note?.id, title.text, plainContent) {
        val current = note ?: return@LaunchedEffect
        delay(400)
        vm.refreshNoteSuggestions(
            note = current,
            draftTitle = title.text,
            draftContent = plainContent
        )
    }

    if (showingTitleEditor) {
        AlertDialog(
            onDismissRequest = { showingTitleEditor = false },
            title = { Text("Edit Title") },
            text = {
                OutlinedTextField(
                    value = titleDraft,
                    onValueChange = { titleDraft = it },
                    label = { Text("Title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        saveTitleEdit()
                        showingTitleEditor = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showingTitleEditor = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showingCreateFolderPrompt) {
        AlertDialog(
            onDismissRequest = {
                showingCreateFolderPrompt = false
                newFolderName = ""
            },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.createFolder(newFolderName)
                        newFolderName = ""
                        showingCreateFolderPrompt = false
                    },
                    enabled = newFolderName.trim().isNotEmpty()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showingCreateFolderPrompt = false
                        newFolderName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    val canPerformUndo = undoHistory.isNotEmpty() || pendingUndoSnapshot != null || canUndoActions
    val canPerformRedo = redoHistory.isNotEmpty()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val editorTopActions = remember(note?.id) {
        listOf(
            EditorTopBarAction("new-note", Icons.Filled.Add, "New note") {
                saveJob?.cancel()
                vm.createNote { created -> onNoteChanged(created) }
            },
            EditorTopBarAction("new-folder", Icons.Filled.Folder, "New folder") {
                newFolderName = ""
                showingCreateFolderPrompt = true
            },
            EditorTopBarAction("export-note", Icons.Filled.Share, "Export note") {
                note?.let(onShareNote)
            }
        )
    }

    Scaffold(topBar = {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val totalWidthPx = with(density) { maxWidth.toPx() } - with(density) { 20.dp.toPx() }
            val iconWidthPx = with(density) { topBarControlSize.toPx() }
            val spacingPx = with(density) { 8.dp.toPx() }
            val titleDisplay = title.text.ifBlank { "Untitled" }
            val titleWidthPx = textMeasurer.measure(
                text = titleDisplay,
                style = MaterialTheme.typography.titleMedium
            ).size.width.toFloat() + with(density) { 24.dp.toPx() }
            val promotableActionCount = editorTopActions.size
            val layout = computeTopBarLayout(
                totalWidthPx = totalWidthPx,
                leadingWidthPx = iconWidthPx,
                titleWidthPx = titleWidthPx,
                actionCount = promotableActionCount + 1,
                actionWidthPx = iconWidthPx,
                spacingPx = spacingPx,
                overflowButtonWidthPx = iconWidthPx
            )

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .width(44.dp)
                        .height(4.dp),
                    shape = RoundedCornerShape(100),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                ) {}

                ChromeActionBar(style = editorChromeStyle, modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(topBarControlSize)
                            .testTag("close-note-editor")
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close note editor",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(topBarIconSize)
                        )
                    }

                    TextButton(
                        onClick = {
                            titleDraft = title.text
                            showingTitleEditor = true
                        },
                        modifier = Modifier
                            .height(topBarControlSize)
                            .testTag("edit-note-title"),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                titleDisplay,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = if (layout.ellipsizeTitle) TextOverflow.Ellipsis else TextOverflow.Clip
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Edit title",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(hintIconSize)
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))

                    editorTopActions.take(layout.visibleActionCount.coerceAtLeast(0)).forEach { action ->
                        IconButton(onClick = action.onClick, modifier = Modifier.size(topBarControlSize)) {
                            Icon(
                                action.icon,
                                contentDescription = action.label,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(topBarIconSize)
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { isAttachmentMenuExpanded = true },
                            modifier = Modifier.size(topBarControlSize)
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "More note actions",
                                modifier = Modifier.size(topBarIconSize),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = isAttachmentMenuExpanded,
                            onDismissRequest = { isAttachmentMenuExpanded = false }
                        ) {
                            editorTopActions.drop(layout.visibleActionCount.coerceAtLeast(0)).forEach { action ->
                                DropdownMenuItem(
                                    text = { Text(action.label) },
                                    onClick = {
                                        isAttachmentMenuExpanded = false
                                        action.onClick()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Attachments") },
                                onClick = {
                                    isAttachmentMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Photo Library") },
                                onClick = {
                                    isAttachmentMenuExpanded = false
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import Image") },
                                onClick = {
                                    isAttachmentMenuExpanded = false
                                    fileImportLauncher.launch(arrayOf("image/*"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Note") },
                                onClick = {
                                    isAttachmentMenuExpanded = false
                                    note?.let { current ->
                                        vm.deleteNote(current)
                                        onBack()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                PinnedTextSection(
                    pinnedTextItems = pinnedTextItems,
                    expanded = arePinnedExpanded,
                    onExpandedChanged = {
                        arePinnedExpanded = it
                        vm.setPinnedTextSectionExpanded(note?.id, it)
                    },
                    onAdd = {
                        note?.let { current ->
                            arePinnedExpanded = true
                            vm.setPinnedTextSectionExpanded(current.id, true)
                            vm.addPinnedText(current)
                        }
                    },
                    onUpdate = vm::updatePinnedText,
                    onMove = vm::movePinnedText,
                    onUnpin = { pinnedText ->
                        flushPendingUndoSnapshot()
                        val before = currentSnapshot()
                        editorActions?.appendStoredContentOnNewLine(pinnedText.sourceContent)
                        redoHistory = emptyList()
                        undoHistory = (undoHistory + before).takeLast(200)
                        vm.unpinText(pinnedText)
                    },
                    onDelete = vm::deletePinnedParagraph
                )

                RichTextEditor(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .focusRequester(editorFocusRequester),
                    storedContent = storedContent,
                    onStoredContentChanged = { newStored ->
                        markContentEdited(newStored)
                    },
                    onPlainTextChanged = { plain ->
                        plainContent = plain
                    },
                    onFormatStateChanged = { state ->
                        formatState = state
                    },
                    actionsSink = { editorActions = it },
                    contentTextColor = MaterialTheme.colorScheme.onSurface,
                    placeholderText = "Start typing...",
                    bottomContentInset = 0.dp
                )

                RichTextActionBars(
                    actions = editorActions,
                    formatState = formatState,
                    showingFormattingControls = showingFormattingControls,
                    canUndo = canPerformUndo,
                    canRedo = canPerformRedo,
                    onUndo = { if (canPerformUndo) undoLastEdit() },
                    onRedo = { if (canPerformRedo) redoLastEdit() },
                    onToggleFormattingControls = {
                        showingFormattingControls = !showingFormattingControls
                    },
                    keyboardVisible = keyboardVisible,
                    onToggleKeyboard = {
                        if (keyboardVisible) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            keyboardVisible = false
                        } else {
                            editorFocusRequester.requestFocus()
                            keyboardController?.show()
                            keyboardVisible = true
                        }
                    },
                    onSetFontSize = { size ->
                        editorActions?.applyFontSize(size)
                    },
                    onPinSelection = {
                        flushPendingUndoSnapshot()
                        val before = currentSnapshot()
                        val selection = editorActions?.pinSelection()
                        if (selection != null) {
                            note?.let { current ->
                                arePinnedExpanded = true
                                vm.setPinnedTextSectionExpanded(current.id, true)
                                redoHistory = emptyList()
                                undoHistory = (undoHistory + before).takeLast(200)
                                vm.addPinnedText(
                                    note = current,
                                    text = selection.text,
                                    sourceContent = selection.sourceContent,
                                    sourceStart = selection.sourceStart
                                )
                            }
                        }
                    },
                    chromeStyle = editorChromeStyle,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                )
            }

            if (attachments.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Attachments", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(100)
                                ) {
                                    Text(
                                        text = attachments.size.toString(),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            IconButton(onClick = { areAttachmentsExpanded = !areAttachmentsExpanded }) {
                                Text(
                                    text = if (areAttachmentsExpanded) "▴" else "▾",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        if (areAttachmentsExpanded) {
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(attachments, key = { it.id }) { attachment ->
                                    AttachmentThumbnail(
                                        attachment = attachment,
                                        onOpen = { expandedAttachmentId = attachment.id },
                                        onRemove = { vm.removePhotoAttachment(attachment) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    if (expandedAttachment != null) {
        AttachmentViewerDialog(
            attachment = expandedAttachment,
            onDismiss = { expandedAttachmentId = null }
        )
    }
}

@Composable
private fun PinnedTextSection(
    pinnedTextItems: List<PinnedText>,
    expanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    onAdd: () -> Unit,
    onUpdate: (PinnedText, String) -> Unit,
    onMove: (PinnedText, Int) -> Unit,
    onUnpin: (PinnedText) -> Unit,
    onDelete: (PinnedText) -> Unit
) {
    var activeDragId by remember { mutableStateOf<Long?>(null) }
    var activeDragOffsetY by remember { mutableFloatStateOf(0f) }
    var pendingInsertionIndex by remember { mutableStateOf<Int?>(null) }

    if (pinnedTextItems.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onAdd,
                modifier = Modifier.testTag("pinned-text-add")
            ) {
                Text("Pinned (0)")
            }
        }
        return
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .testTag("pinned-text-section"),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onExpandedChanged(!expanded) },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("pinned-text-toggle")
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Pinned (${pinnedTextItems.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onAdd) {
                    Text("Add")
                }
            }

            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pendingInsertionIndex == 0) {
                        ReorderInsertionIndicator()
                    }
                    pinnedTextItems.forEachIndexed { index, pinnedText ->
                        PinnedTextRow(
                            pinnedText = pinnedText,
                            index = index,
                            count = pinnedTextItems.size,
                            activeDragId = activeDragId,
                            activeDragOffsetY = activeDragOffsetY,
                            onUpdate = onUpdate,
                            onMove = { item, targetIndex ->
                                if (targetIndex != index && targetIndex != index + 1) {
                                    onMove(item, targetIndex)
                                }
                                activeDragId = null
                                activeDragOffsetY = 0f
                                pendingInsertionIndex = null
                            },
                            onDragChanged = { item, offsetY ->
                                activeDragId = item.id
                                activeDragOffsetY = offsetY
                                pendingInsertionIndex = reorderInsertionIndex(
                                    currentIndex = index,
                                    count = pinnedTextItems.size,
                                    offsetY = offsetY
                                )
                            },
                            onDragCancelled = {
                                activeDragId = null
                                activeDragOffsetY = 0f
                                pendingInsertionIndex = null
                            },
                            onUnpin = onUnpin,
                            onDelete = onDelete
                        )
                        if (pendingInsertionIndex == index + 1) {
                            ReorderInsertionIndicator()
                        }
                    }
                }
            } else {
                val preview = pinnedTextItems.firstOrNull()?.text
                    ?.ifBlank { "Pinned" }
                    ?: "Pinned"
                Surface(
                    modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp, bottom = 6.dp)
                        .testTag("pinned-text-collapsed-preview"),
                    shape = RoundedCornerShape(8.dp),
                    color = PinnedHighlightPalette.Highlight
                ) {
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = if (preview == "Pinned") {
                            PinnedHighlightPalette.PlaceholderText
                        } else {
                            PinnedHighlightPalette.Text
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PinnedTextRow(
    pinnedText: PinnedText,
    index: Int,
    count: Int,
    activeDragId: Long?,
    activeDragOffsetY: Float,
    onUpdate: (PinnedText, String) -> Unit,
    onMove: (PinnedText, Int) -> Unit,
    onDragChanged: (PinnedText, Float) -> Unit,
    onDragCancelled: () -> Unit,
    onUnpin: (PinnedText) -> Unit,
    onDelete: (PinnedText) -> Unit
) {
    var draft by remember(pinnedText.id, pinnedText.text) { mutableStateOf(pinnedText.text) }
    var editing by remember(pinnedText.id) { mutableStateOf(false) }
    var menuExpanded by remember(pinnedText.id) { mutableStateOf(false) }
    var dragOffsetY by remember(pinnedText.id) { mutableFloatStateOf(0f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = if (activeDragId == pinnedText.id) activeDragOffsetY else 0f
            }
            .combinedClickable(
                onClick = { editing = true },
                onLongClick = { menuExpanded = true }
            )
            .testTag("pinned-text-row")
            .background(PinnedHighlightPalette.Highlight, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ReorderGripDots(
            modifier = Modifier
                .size(width = 24.dp, height = 30.dp)
                .pointerInput(pinnedText.id, index, count) {
                    var totalDragY = 0f
                    detectDragGestures(
                        onDragEnd = {
                            val targetIndex = reorderInsertionIndex(
                                currentIndex = index,
                                count = count,
                                offsetY = totalDragY
                            ) ?: index
                            onMove(pinnedText, targetIndex)
                            dragOffsetY = 0f
                            totalDragY = 0f
                        },
                        onDragCancel = {
                            dragOffsetY = 0f
                            totalDragY = 0f
                            onDragCancelled()
                        },
                        onDrag = { _, dragAmount ->
                            dragOffsetY += dragAmount.y
                            totalDragY += dragAmount.y
                            onDragChanged(pinnedText, totalDragY)
                        }
                    )
                }
                .testTag("pinned-text-drag-handle")
        )

        if (editing) {
            OutlinedTextField(
                value = draft,
                onValueChange = {
                    draft = it
                    onUpdate(pinnedText, it)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("pinned-text-field"),
                maxLines = 4,
                placeholder = { Text("Pinned") }
            )
        } else {
            Text(
                text = pinnedText.text.ifBlank { "Pinned" },
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (pinnedText.text.isBlank()) {
                    PinnedHighlightPalette.PlaceholderText
                } else {
                    PinnedHighlightPalette.Text
                },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                Icons.Filled.Edit,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = PinnedHighlightPalette.PlaceholderText
            )
        }

        Box {
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Unpin") },
                    onClick = {
                        menuExpanded = false
                        onUnpin(pinnedText)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        menuExpanded = false
                        onDelete(pinnedText)
                    }
                )
            }
        }
    }
}

@Composable
private fun ReorderGripDots(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(2) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) {
                    Surface(
                        modifier = Modifier.size(4.dp),
                        shape = CircleShape,
                        color = PinnedHighlightPalette.PlaceholderText
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun ReorderInsertionIndicator() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp),
        shape = RoundedCornerShape(100),
        color = MaterialTheme.colorScheme.primary
    ) {}
}

private fun reorderInsertionIndex(currentIndex: Int, count: Int, offsetY: Float): Int? {
    val rowStepPx = 56f
    val steps = (offsetY / rowStepPx).toInt()
    val targetIndex = when {
        steps > 0 -> currentIndex + steps + 1
        steps < 0 -> currentIndex + steps
        else -> return null
    }.coerceIn(0, count)
    return targetIndex
}

private data class EditorSnapshot(
    val title: TextFieldValue,
    val storedContent: String,
    val plainContent: String,
    val pinnedTextItems: List<PinnedText> = emptyList()
)

private data class EditorTopBarAction(
    val id: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val onClick: () -> Unit
)
