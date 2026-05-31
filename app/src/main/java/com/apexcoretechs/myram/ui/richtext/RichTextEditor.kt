package com.apexcoretechs.myram.ui.richtext

import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

internal class RichTextEditorActions(
    private val editorProvider: () -> RichTextEditorBinding?
) {
    fun toggleBold() {
        editorProvider()?.withEditable { editable, start, end ->
            toggleBold(editable, start, end)
        }
    }

    fun toggleItalic() {
        editorProvider()?.withEditable { editable, start, end ->
            toggleItalic(editable, start, end)
        }
    }

    fun toggleUnderline() {
        editorProvider()?.withEditable { editable, start, end ->
            toggleUnderline(editable, start, end)
        }
    }

    fun toggleStrikethrough() {
        editorProvider()?.withEditable { editable, start, end ->
            toggleStrikethrough(editable, start, end)
        }
    }

    fun applyColor(color: Color) {
        editorProvider()?.withEditable { editable, start, end ->
            applyTextColor(editable, start, end, color.toArgb())
        }
    }

    fun applyFontSize(fontSizeSp: Int) {
        editorProvider()?.withEditable { editable, start, end ->
            applyFontSize(editable, start, end, fontSizeSp)
        }
    }

    fun currentStoredContent(): String? {
        val editor = editorProvider() ?: return null
        return encodeRichTextContent(editor.currentText)
    }

    fun hideKeyboard() {
        editorProvider()?.clearFocus()
    }

    fun copySelection() {
        editorProvider()?.copySelection()
    }

    fun cutSelection() {
        editorProvider()?.cutSelection()
    }

    fun pasteClipboard() {
        editorProvider()?.pasteClipboard()
    }

    fun toggleSelectAll() {
        editorProvider()?.toggleSelectAll()
    }
}

@Composable
internal fun RichTextEditor(
    modifier: Modifier,
    storedContent: String,
    onStoredContentChanged: (String) -> Unit,
    onPlainTextChanged: (String) -> Unit,
    onFormatStateChanged: (RichTextFormatState) -> Unit,
    actionsSink: (RichTextEditorActions) -> Unit,
    contentTextColor: Color,
    placeholderText: String,
    bottomContentInset: Dp = 96.dp
) {
    val actions = remember {
        RichTextEditorActions { currentEditor }
    }
    actionsSink(actions)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            FormattingEditText(context).apply {
                setTextColor(contentTextColor.toArgb())
                setHintTextColor(contentTextColor.copy(alpha = 0.45f).toArgb())
                hint = placeholderText
                setText(decodeRichTextContent(storedContent), TextView.BufferType.EDITABLE)
                moveCursorToEnd()
                installWatchers(
                    onStoredContentChanged = onStoredContentChanged,
                    onPlainTextChanged = onPlainTextChanged,
                    onFormatStateChanged = onFormatStateChanged
                )
                currentEditor = this
                setBottomContentInset(bottomContentInset)
                emitFormatState(onFormatStateChanged)
            }
        },
        update = { editText ->
            if (currentEditor !== editText) {
                currentEditor = editText
            }
            val currentEncoded = encodeRichTextContent(editText.text ?: "")
            if (currentEncoded != storedContent) {
                editText.replaceContent(storedContent)
            }
            editText.setBottomContentInset(bottomContentInset)
            editText.emitFormatState(onFormatStateChanged)
        }
    )
}

private var currentEditor: FormattingEditText? = null

internal interface RichTextEditorBinding {
    val currentText: CharSequence
    fun withEditable(action: (Editable, Int, Int) -> Unit)
    fun clearFocus()
    fun copySelection()
    fun cutSelection()
    fun pasteClipboard()
    fun toggleSelectAll()
}

private class FormattingEditText(context: Context) : AppCompatEditText(context), RichTextEditorBinding {
    private var suppressCallbacks = false
    private var onStoredContentChanged: ((String) -> Unit)? = null
    private var onPlainTextChanged: ((String) -> Unit)? = null
    private var onFormatStateChanged: ((RichTextFormatState) -> Unit)? = null

    init {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        minLines = 10
        isVerticalScrollBarEnabled = true
        overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS

        val disabledActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                menu?.clear()
                mode?.finish()
                return false
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                menu?.clear()
                mode?.finish()
                return false
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean = false
            override fun onDestroyActionMode(mode: ActionMode?) = Unit
        }
        customSelectionActionModeCallback = disabledActionModeCallback
        customInsertionActionModeCallback = disabledActionModeCallback
    }

    fun installWatchers(
        onStoredContentChanged: (String) -> Unit,
        onPlainTextChanged: (String) -> Unit,
        onFormatStateChanged: (RichTextFormatState) -> Unit
    ) {
        this.onStoredContentChanged = onStoredContentChanged
        this.onPlainTextChanged = onPlainTextChanged
        this.onFormatStateChanged = onFormatStateChanged
        addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                override fun afterTextChanged(s: Editable?) {
                    if (s == null || suppressCallbacks) return
                    suppressCallbacks = true
                    applyChecklistStrikeThrough(s)
                    suppressCallbacks = false
                    publishChanges()
                }
            }
        )
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        emitFormatState()
    }

    override fun startActionMode(callback: ActionMode.Callback?): ActionMode? = null

    override fun startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? = null

    override fun onTextContextMenuItem(id: Int): Boolean = false

    override fun isSuggestionsEnabled(): Boolean = false

    fun emitFormatState(onFormatStateChanged: (RichTextFormatState) -> Unit) {
        this.onFormatStateChanged = onFormatStateChanged
        emitFormatState()
    }

    private fun emitFormatState() {
        val editable = text ?: return
        val state = computeRichTextFormatState(
            editable = editable,
            selectionStart = selectionStart,
            selectionEnd = selectionEnd,
            defaultTextColor = currentTextColor,
            defaultFontSizeSp = 16
        )
        onFormatStateChanged?.invoke(state)
    }

    fun replaceContent(storedContent: String) {
        suppressCallbacks = true
        setText(decodeRichTextContent(storedContent), BufferType.EDITABLE)
        moveCursorToEnd()
        suppressCallbacks = false
    }

    fun moveCursorToEnd() {
        text?.let { setSelection(it.length) }
    }

    fun setBottomContentInset(inset: Dp) {
        val bottomPaddingPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            inset.value,
            resources.displayMetrics
        ).toInt()
        setPadding(
            paddingLeft,
            paddingTop,
            paddingRight,
            bottomPaddingPx
        )
    }

    override fun withEditable(action: (Editable, Int, Int) -> Unit) {
        val editable = text ?: return
        val safeStart = selectionStart.coerceIn(0, editable.length)
        val safeEnd = selectionEnd.coerceIn(0, editable.length)
        suppressCallbacks = true
        action(editable, safeStart, safeEnd)
        suppressCallbacks = false
        applyChecklistStrikeThrough(editable)
        setSelection(
            safeStart.coerceAtMost(editable.length),
            safeEnd.coerceAtMost(editable.length)
        )
        publishChanges()
    }

    override fun copySelection() {
        val editable = text ?: return
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, editable.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(0, editable.length)
        if (start == end) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("text", editable.subSequence(start, end)))
    }

    override fun cutSelection() {
        val editable = text ?: return
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, editable.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(0, editable.length)
        if (start == end) return
        copySelection()
        editable.delete(start, end)
        setSelection(start.coerceAtMost(editable.length))
        publishChanges()
    }

    override fun pasteClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = clipboard.primaryClip ?: return
        val pasted = clip.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
        if (pasted.isEmpty()) return
        val editable = text ?: return
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, editable.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(0, editable.length)
        editable.replace(start, end, pasted)
        val cursor = (start + pasted.length).coerceAtMost(editable.length)
        setSelection(cursor)
        publishChanges()
    }

    override fun toggleSelectAll() {
        val editable = text ?: return
        if (editable.isEmpty()) return
        val start = minOf(selectionStart, selectionEnd)
        val end = maxOf(selectionStart, selectionEnd)
        if (start == 0 && end == editable.length) {
            setSelection(editable.length)
        } else {
            setSelection(0, editable.length)
        }
        emitFormatState()
    }

    private fun publishChanges() {
        val editable = text ?: return
        onPlainTextChanged?.invoke(editable.toString())
        onStoredContentChanged?.invoke(encodeRichTextContent(editable))
        emitFormatState()
    }

    override val currentText: CharSequence
        get() = text ?: ""
}
