package com.apexcoretechs.myram.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.SelectAll
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewModelScope
import com.apexcoretechs.myram.data.Note
import com.apexcoretechs.myram.data.NotePhotoAttachment
import com.apexcoretechs.myram.ui.NotesViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class EditorField {
    TITLE,
    CONTENT
}

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
private fun AttachmentInlineActions(
    onDismissKeyboard: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onSelectAll: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        InlineIconAction(Icons.Filled.KeyboardHide, "Hide keyboard", onDismissKeyboard)
        InlineIconAction(Icons.Filled.ContentCut, "Cut", onCut)
        InlineIconAction(Icons.Filled.ContentCopy, "Copy", onCopy)
        InlineIconAction(Icons.Filled.ContentPaste, "Paste", onPaste)
        InlineIconAction(Icons.Filled.SelectAll, "Select all", onSelectAll)
    }
}

@Composable
private fun InlineIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(24.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    vm: NotesViewModel,
    note: Note?,
    onNoteChanged: (Note) -> Unit,
    onBack: () -> Unit
) {
    var title by remember(note?.id) { mutableStateOf(TextFieldValue(note?.title ?: "")) }
    var content by remember(note?.id) { mutableStateOf(TextFieldValue(note?.content ?: "")) }
    var undoHistory by remember(note?.id) { mutableStateOf<List<EditorSnapshot>>(emptyList()) }
    var pendingUndoSnapshot by remember(note?.id) { mutableStateOf<EditorSnapshot?>(null) }
    var saveJob by remember { mutableStateOf<Job?>(null) }
    var undoJob by remember { mutableStateOf<Job?>(null) }
    var hasEditedCurrentNote by remember(note?.id) { mutableStateOf(false) }
    var isRestoringUndo by remember(note?.id) { mutableStateOf(false) }
    var isAttachmentMenuExpanded by remember { mutableStateOf(false) }
    var areAttachmentsExpanded by remember(note?.id) { mutableStateOf(false) }
    var expandedAttachmentId by remember(note?.id) { mutableStateOf<Long?>(null) }
    var focusedField by remember(note?.id) { mutableStateOf<EditorField?>(null) }
    var previousAttachmentCount by remember(note?.id) { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val attachments by vm.noteAttachments(note?.id).collectAsState(initial = emptyList())
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

    fun currentSnapshot() = EditorSnapshot(title = title, content = content)

    fun pushUndoSnapshot(snapshot: EditorSnapshot) {
        if (snapshot == currentSnapshot()) return
        undoHistory = (undoHistory + snapshot).takeLast(200)
    }

    fun flushPendingUndoSnapshot() {
        undoJob?.cancel()
        pendingUndoSnapshot?.let(::pushUndoSnapshot)
        pendingUndoSnapshot = null
    }

    fun scheduleUndoSnapshot(snapshot: EditorSnapshot) {
        if (isRestoringUndo) return
        if (pendingUndoSnapshot == null) {
            pendingUndoSnapshot = snapshot
        }
        undoJob?.cancel()
        undoJob = scope.launch {
            delay(800)
            pendingUndoSnapshot?.let(::pushUndoSnapshot)
            pendingUndoSnapshot = null
        }
    }

    fun undoLastEdit() {
        flushPendingUndoSnapshot()
        val snapshot = undoHistory.lastOrNull() ?: return
        undoHistory = undoHistory.dropLast(1)
        isRestoringUndo = true
        hasEditedCurrentNote = true
        title = snapshot.title
        content = snapshot.content
        isRestoringUndo = false
    }

    fun applyToFocusedField(transform: (TextFieldValue) -> TextFieldValue) {
        when (focusedField) {
            EditorField.TITLE -> title = transform(title)
            EditorField.CONTENT, null -> content = transform(content)
        }
        hasEditedCurrentNote = true
    }

    fun copyFromFocused() {
        val source = when (focusedField) {
            EditorField.TITLE -> title
            EditorField.CONTENT, null -> content
        }
        val copied = copySelectedText(source) ?: return
        clipboardManager.setText(AnnotatedString(copied))
    }

    fun cutFromFocused() {
        val source = when (focusedField) {
            EditorField.TITLE -> title
            EditorField.CONTENT, null -> content
        }
        val (updated, cutText) = cutSelectedText(source)
        if (cutText.isNullOrEmpty()) return
        applyToFocusedField { updated }
        clipboardManager.setText(AnnotatedString(cutText))
    }

    fun pasteIntoFocused() {
        val pastedText = clipboardManager.getText()?.text?.toString().orEmpty()
        if (pastedText.isEmpty()) return
        applyToFocusedField { current -> pasteIntoSelection(current, pastedText) }
    }

    fun selectAllFocused() {
        applyToFocusedField(::selectAllText)
    }

    // Auto-save with debounce
    LaunchedEffect(note?.id, title, content) {
        saveJob?.cancel()
        if (!hasEditedCurrentNote) return@LaunchedEffect
        saveJob = vm.viewModelScope.launch {
            delay(800)
            note?.let { current ->
                val updated = current.copy(
                    title = title.text,
                    content = content.text
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
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Note") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = ::undoLastEdit,
                        enabled = undoHistory.isNotEmpty() || pendingUndoSnapshot != null
                    ) {
                        Text("Undo")
                    }

                    Box {
                        TextButton(
                            onClick = { isAttachmentMenuExpanded = true },
                            enabled = note != null
                        ) {
                            Text("Attach")
                        }

                        DropdownMenu(
                            expanded = isAttachmentMenuExpanded,
                            onDismissRequest = { isAttachmentMenuExpanded = false }
                        ) {
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
                        }
                    }

                    IconButton(
                        onClick = {
                            note?.let { current ->
                                vm.deleteNote(current)
                                onBack()
                            }
                        },
                        enabled = note != null
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete note")
                    }

                    IconButton(
                        onClick = {
                            saveJob?.cancel()
                            vm.createNote { created ->
                                onNoteChanged(created)
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "New note")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    if (it != title) {
                        scheduleUndoSnapshot(currentSnapshot())
                    }
                    hasEditedCurrentNote = true
                    title = it
                },
                placeholder = { Text("Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) focusedField = EditorField.TITLE
                    }
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = content,
                onValueChange = {
                    if (it != content) {
                        scheduleUndoSnapshot(currentSnapshot())
                    }
                    hasEditedCurrentNote = true
                    content = it
                },
                placeholder = { Text("Start typing...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) focusedField = EditorField.CONTENT
                    },
                singleLine = false
            )

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

                            AttachmentInlineActions(
                                onDismissKeyboard = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                },
                                onCut = ::cutFromFocused,
                                onCopy = ::copyFromFocused,
                                onPaste = ::pasteIntoFocused,
                                onSelectAll = ::selectAllFocused
                            )

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
                                        onOpen = {
                                            expandedAttachmentId = attachment.id
                                        },
                                        onRemove = {
                                            vm.removePhotoAttachment(attachment)
                                        }
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

private data class EditorSnapshot(
    val title: TextFieldValue,
    val content: TextFieldValue
)
