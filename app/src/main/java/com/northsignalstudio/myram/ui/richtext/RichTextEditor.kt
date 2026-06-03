package com.northsignalstudio.myram.ui.richtext

import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.NoCopySpan
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.util.TypedValue
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
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

    fun toggleChecklistItem() {
        editorProvider()?.toggleChecklistItem()
    }

    fun applyColor(color: Color) {
        editorProvider()?.withEditable { editable, start, end ->
            applyTextColor(editable, start, end, color.toArgb())
        }
    }

    fun clearColor() {
        editorProvider()?.withEditable { editable, start, end ->
            clearTextColor(editable, start, end)
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

    fun pinSelection(): PinnedEditorSelection? {
        return editorProvider()?.pinSelection()
    }

    fun insertStoredContent(storedContent: String, preferredOffset: Int) {
        editorProvider()?.insertStoredContent(storedContent, preferredOffset)
    }

    fun appendStoredContentOnNewLine(storedContent: String) {
        editorProvider()?.appendStoredContentOnNewLine(storedContent)
    }
}

internal data class PinnedEditorSelection(
    val text: String,
    val sourceContent: String,
    val sourceStart: Int
)

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
                applySelectionColors(contentTextColor)
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
            editText.setTextColor(contentTextColor.toArgb())
            editText.setHintTextColor(contentTextColor.copy(alpha = 0.45f).toArgb())
            editText.applySelectionColors(contentTextColor)
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
    fun toggleChecklistItem()
    fun toggleSelectAll()
    fun pinSelection(): PinnedEditorSelection?
    fun insertStoredContent(storedContent: String, preferredOffset: Int)
    fun appendStoredContentOnNewLine(storedContent: String)
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            if (tryToggleChecklistFromTouch(event)) {
                return true
            }
        }
        return super.onTouchEvent(event)
    }

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
        val currentStart = selectionStart
        val currentEnd = selectionEnd
        suppressCallbacks = true
        setText(decodeRichTextContent(storedContent), BufferType.EDITABLE)
        val length = text?.length ?: 0
        val safeStart = currentStart.coerceIn(0, length)
        val safeEnd = currentEnd.coerceIn(0, length)
        if (safeStart == safeEnd) {
            setSelection(safeStart)
        } else {
            setSelection(safeStart, safeEnd)
        }
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

    fun applySelectionColors(contentTextColor: Color) {
        val cursorColor = contentTextColor.toArgb()
        highlightColor = contentTextColor.copy(alpha = 0.24f).toArgb()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            textCursorDrawable = GradientDrawable().apply {
                setColor(cursorColor)
                setSize(2, lineHeight.coerceAtLeast(24))
            }
        }
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

    override fun toggleChecklistItem() {
        val editable = text ?: return
        val safeStart = selectionStart.coerceIn(0, editable.length)
        val safeEnd = selectionEnd.coerceIn(0, editable.length)
        suppressCallbacks = true
        val result = toggleChecklistAtSelection(editable, safeStart, safeEnd)
        suppressCallbacks = false
        setSelection(
            result.selectionStart.coerceAtMost(editable.length),
            result.selectionEnd.coerceAtMost(editable.length)
        )
        animateChecklistToggle()
        publishChanges()
    }

    override fun toggleSelectAll() {
        post {
            val editable = text ?: return@post
            if (editable.isEmpty()) return@post
            requestFocus()
            val target = toggleSelectAllRange(
                length = editable.length,
                selectionStart = selectionStart,
                selectionEnd = selectionEnd
            )
            if (target.start == target.end) {
                setSelection(target.end)
            } else {
                setSelection(target.start, target.end)
            }
            emitFormatState()
        }
    }

    override fun pinSelection(): PinnedEditorSelection? {
        val editable = text ?: return null
        val cursor = selectionStart.coerceIn(0, editable.length)
        val candidate = pinCandidateInText(editable.toString(), cursor) ?: return null
        val start = candidate.textStart
        val end = candidate.textEnd

        val selected = SpannableStringBuilder(editable.subSequence(start, end))
        val plainText = selected.toString()
        val sourceContent = encodeRichTextContent(selected)

        suppressCallbacks = true
        editable.delete(candidate.sourceStart, candidate.sourceEnd)
        suppressCallbacks = false
        setSelection(candidate.sourceStart.coerceAtMost(editable.length))
        publishChanges()

        return PinnedEditorSelection(
            text = plainText,
            sourceContent = sourceContent,
            sourceStart = candidate.sourceStart
        )
    }

    override fun insertStoredContent(storedContent: String, preferredOffset: Int) {
        val editable = text ?: return
        val decoded = decodeRichTextContent(storedContent)
        if (decoded.isEmpty()) return

        val start = preferredOffset.coerceIn(0, editable.length)

        suppressCallbacks = true
        insertDecodedContent(editable, start, prefix = "", decoded = decoded)
        suppressCallbacks = false
        setSelection((start + decoded.length).coerceAtMost(editable.length))
        publishChanges()
    }

    override fun appendStoredContentOnNewLine(storedContent: String) {
        val editable = text ?: return
        val decoded = decodeRichTextContent(storedContent)
        if (decoded.isEmpty()) return

        val prefix = if (editable.isNotEmpty()) "\n" else ""
        val start = editable.length
        suppressCallbacks = true
        insertDecodedContent(editable, start, prefix = prefix, decoded = decoded)
        suppressCallbacks = false
        setSelection((start + prefix.length + decoded.length).coerceAtMost(editable.length))
        publishChanges()
    }

    private fun insertDecodedContent(
        editable: Editable,
        start: Int,
        prefix: String,
        decoded: Spanned
    ) {
        val contentStart = start + prefix.length
        val contentEnd = contentStart + decoded.length
        editable.insert(start, prefix + decoded.toString())
        trimInheritedSpans(editable, start, contentEnd)
        copySourceSpans(decoded, editable, contentStart)
    }

    private fun trimInheritedSpans(editable: Editable, start: Int, end: Int) {
        editable.getSpans(start, end, Any::class.java).forEach { span ->
            if (span is NoCopySpan) return@forEach
            val spanStart = editable.getSpanStart(span)
            val spanEnd = editable.getSpanEnd(span)
            val flags = editable.getSpanFlags(span)
            if (spanStart < 0 || spanEnd < 0) return@forEach
            if (spanStart >= start && spanEnd <= end) {
                editable.removeSpan(span)
            } else if (spanStart < start && spanEnd > start) {
                editable.setSpan(span, spanStart, start, flags)
            } else if (spanStart < end && spanEnd > end) {
                editable.setSpan(span, end, spanEnd, flags)
            }
        }
    }

    private fun copySourceSpans(source: Spanned, editable: Editable, destinationStart: Int) {
        source.getSpans(0, source.length, Any::class.java).forEach { span ->
            if (span is NoCopySpan) return@forEach
            val spanStart = source.getSpanStart(span)
            val spanEnd = source.getSpanEnd(span)
            if (spanStart < 0 || spanEnd <= spanStart) return@forEach
            editable.setSpan(
                span,
                destinationStart + spanStart,
                destinationStart + spanEnd,
                source.getSpanFlags(span)
            )
        }
    }

    private fun publishChanges() {
        val editable = text ?: return
        onPlainTextChanged?.invoke(editable.toString())
        onStoredContentChanged?.invoke(encodeRichTextContent(editable))
        emitFormatState()
    }

    override val currentText: CharSequence
        get() = text ?: ""

    private fun tryToggleChecklistFromTouch(event: MotionEvent): Boolean {
        val editable = text ?: return false
        val layout = layout ?: return false
        val contentX = event.x - totalPaddingLeft + scrollX
        val contentY = event.y - totalPaddingTop + scrollY
        val line = layout.getLineForVertical(contentY.toInt())
        if (line < 0 || line >= layout.lineCount) return false
        val offset = layout.getOffsetForHorizontal(line, contentX)
        if (offset < 0 || offset >= editable.length) return false
        if (!isChecklistIconAtOffset(editable.toString(), offset)) return false

        suppressCallbacks = true
        val result = toggleChecklistAtSelection(editable, offset, offset)
        suppressCallbacks = false
        setSelection(result.selectionStart.coerceAtMost(editable.length))
        animateChecklistToggle()
        publishChanges()
        return true
    }

    private fun animateChecklistToggle() {
        animate().cancel()
        animate()
            .alpha(0.85f)
            .setDuration(70L)
            .withEndAction {
                animate().alpha(1f).setDuration(110L).start()
            }
            .start()
    }
}

internal data class SelectionTarget(val start: Int, val end: Int)

internal fun toggleSelectAllRange(length: Int, selectionStart: Int, selectionEnd: Int): SelectionTarget {
    if (length <= 0) return SelectionTarget(0, 0)
    val hasValidSelection = selectionStart >= 0 && selectionEnd >= 0
    if (!hasValidSelection) return SelectionTarget(0, length)

    val safeStart = selectionStart.coerceIn(0, length)
    val safeEnd = selectionEnd.coerceIn(0, length)
    val normalizedStart = minOf(safeStart, safeEnd)
    val normalizedEnd = maxOf(safeStart, safeEnd)
    return if (normalizedStart == 0 && normalizedEnd == length) {
        SelectionTarget(length, length)
    } else {
        SelectionTarget(0, length)
    }
}
