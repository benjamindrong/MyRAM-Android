package com.northsignalstudio.myram.ui.richtext

import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Editable
import android.text.Html
import android.text.InputType
import android.text.NoCopySpan
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.abs

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

    fun pasteClipboardMatchingDestinationFormatting() {
        editorProvider()?.pasteClipboardMatchingDestinationFormatting()
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
        modifier = modifier.clipToBounds(),
        factory = { context ->
            FormattingEditText(context).apply {
                setTextColor(contentTextColor.toArgb())
                setHintTextColor(contentTextColor.copy(alpha = 0.45f).toArgb())
                applySelectionColors(contentTextColor)
                hint = placeholderText
                setText(decodeRichTextContent(storedContent), TextView.BufferType.EDITABLE)
                text?.let { applyRichTextFormatting(it, paragraphSpacingPx) }
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
    fun pasteClipboardMatchingDestinationFormatting()
    fun toggleChecklistItem()
    fun toggleSelectAll()
    fun pinSelection(): PinnedEditorSelection?
    fun insertStoredContent(storedContent: String, preferredOffset: Int)
    fun appendStoredContentOnNewLine(storedContent: String)
}

private class FormattingEditText(context: Context) : AppCompatEditText(context), RichTextEditorBinding {
    private val checklistGutterWidthPx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        32f,
        resources.displayMetrics
    ).toInt()
    private val checklistIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    internal val paragraphSpacingPx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        8f, // Extra 0.5 line spacing for paragraphs (assuming 16sp base)
        resources.displayMetrics
    ).toInt()
    private var baseLeftPaddingPx = 0
    private var baseRightPaddingPx = 0
    private var baseTopPaddingPx = 0
    private var lastBottomPaddingPx = 0
    private var suppressCallbacks = false
    private var onStoredContentChanged: ((String) -> Unit)? = null
    private var onPlainTextChanged: ((String) -> Unit)? = null
    private var onFormatStateChanged: ((RichTextFormatState) -> Unit)? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var touchMoved = false

    init {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        minLines = 10
        isFocusable = true
        isFocusableInTouchMode = true
        isCursorVisible = true
        isVerticalScrollBarEnabled = true
        overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS
        includeFontPadding = true
        baseLeftPaddingPx = paddingLeft
        baseRightPaddingPx = paddingRight
        baseTopPaddingPx = paddingTop
        applyEditorPadding(paddingBottom)
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
                    applyRichTextFormatting(s, paragraphSpacingPx)
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

    override fun onTextContextMenuItem(id: Int): Boolean = false

    override fun isSuggestionsEnabled(): Boolean = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                touchMoved = false
                requestFocus()
                parent?.requestDisallowInterceptTouchEvent(canScrollVertically(-1) || canScrollVertically(1))
            }
            MotionEvent.ACTION_MOVE -> {
                if (abs(event.x - touchDownX) > touchSlop || abs(event.y - touchDownY) > touchSlop) {
                    touchMoved = true
                }
                parent?.requestDisallowInterceptTouchEvent(canScrollVertically(-1) || canScrollVertically(1))
            }
            MotionEvent.ACTION_UP -> {
                if (tryToggleChecklistFromTouch(event)) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }
                parent?.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        val handled = super.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP && hasFocus()) {
            val isShortTap = !touchMoved && event.eventTime - event.downTime < ViewConfiguration.getLongPressTimeout()
            if (isShortTap) {
                placeCursorAtTouch(event)
            }
            post {
                val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                inputMethodManager?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        return handled
    }

    private fun placeCursorAtTouch(event: MotionEvent) {
        val editable = text ?: return
        val editorLayout = layout ?: return
        if (editable.isEmpty()) {
            setSelection(0)
            return
        }
        val contentX = event.x - totalPaddingLeft + scrollX
        val contentY = event.y - totalPaddingTop + scrollY
        val line = editorLayout.getLineForVertical(contentY.toInt()).coerceIn(0, editorLayout.lineCount - 1)
        val offset = editorLayout.getOffsetForHorizontal(line, contentX).coerceIn(0, editable.length)
        setSelection(offset)
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawChecklistGutter(canvas)
    }

    private fun applyEditorPadding(bottomPaddingPx: Int = lastBottomPaddingPx) {
        lastBottomPaddingPx = bottomPaddingPx
        val content = text?.toString().orEmpty()
        val hasChecklist = checklistIconRanges(content).isNotEmpty()
        val gutter = if (hasChecklist) checklistGutterWidthPx else 0
        setPadding(
            baseLeftPaddingPx + gutter,
            baseTopPaddingPx,
            baseRightPaddingPx + gutter,
            bottomPaddingPx
        )
    }

    private fun drawChecklistGutter(canvas: Canvas) {
        val editable = text ?: return
        val editorLayout = layout ?: return
        val content = editable.toString()
        if (content.isEmpty()) return

        checklistIconPaint.color = currentTextColor
        checklistIconPaint.textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            CHECKLIST_ICON_SIZE_SP.toFloat(),
            resources.displayMetrics
        )

        val textMetrics = paint.fontMetrics
        val iconMetrics = checklistIconPaint.fontMetrics
        // Center the icon vertically relative to the text line's center
        val textCenter = (textMetrics.descent + textMetrics.ascent) / 2f
        val iconCenter = (iconMetrics.descent + iconMetrics.ascent) / 2f
        val alignmentOffset = textCenter - iconCenter

        val gutterCenterX = scrollX + baseLeftPaddingPx + checklistGutterWidthPx / 2f
        val saveCount = canvas.save()
        canvas.clipRect(scrollX, 0, scrollX + width, height)
        try {
            checklistIconRanges(content).forEach { range ->
                if (range.start >= editable.length) return@forEach
                val contentOffset = checklistIconContentOffset(range.end, editable.length)
                val line = editorLayout.getLineForOffset(contentOffset)
                val lineBaseline = editorLayout.getLineBaseline(line).toFloat()
                val iconY = checklistIconLayoutY(
                    totalPaddingTop = totalPaddingTop.toFloat(),
                    lineBaseline = lineBaseline,
                    alignmentOffset = alignmentOffset
                )
                if (iconY + iconMetrics.ascent > height || iconY + iconMetrics.descent < 0) {
                    return@forEach
                }
                val icon = when {
                    content.startsWith(CHECKLIST_CHECKED_PREFIX, range.start) -> CHECKLIST_CHECKED_PREFIX.trim()
                    else -> CHECKLIST_UNCHECKED_PREFIX.trim()
                }
                canvas.drawText(
                    icon,
                    gutterCenterX,
                    iconY,
                    checklistIconPaint
                )
            }
        } finally {
            canvas.restoreToCount(saveCount)
        }
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
        text?.let { applyRichTextFormatting(it, paragraphSpacingPx) }
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
        applyEditorPadding(bottomPaddingPx)
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
        applyRichTextFormatting(editable, paragraphSpacingPx)
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
        val pasted = clipboard.primaryClip?.firstStyledText() ?: return
        if (pasted.isEmpty()) return
        val editable = text ?: return
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, editable.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(0, editable.length)
        suppressCallbacks = true
        val cursor = pasteStyledClipboardContent(editable, start, end, pasted)
        suppressCallbacks = false
        applyRichTextFormatting(editable, paragraphSpacingPx)
        setSelection(cursor)
        publishChanges()
    }

    override fun pasteClipboardMatchingDestinationFormatting() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val pasted = clipboard.primaryClip?.firstPlainText().orEmpty()
        if (pasted.isEmpty()) return
        val editable = text ?: return
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, editable.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(0, editable.length)
        suppressCallbacks = true
        val cursor = pastePlainTextMatchingDestinationFormatting(editable, start, end, pasted)
        suppressCallbacks = false
        applyRichTextFormatting(editable, paragraphSpacingPx)
        setSelection(cursor.coerceAtMost(editable.length))
        publishChanges()
    }

    override fun toggleChecklistItem() {
        val editable = text ?: return
        val safeStart = selectionStart.coerceIn(0, editable.length)
        val safeEnd = selectionEnd.coerceIn(0, editable.length)
        suppressCallbacks = true
        val result = toggleChecklistAtSelection(editable, safeStart, safeEnd, paragraphSpacingPx)
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

    private fun pasteStyledClipboardContent(
        editable: Editable,
        start: Int,
        end: Int,
        pasted: CharSequence
    ): Int {
        editable.delete(start, end)
        editable.insert(start, pasted.toString())
        val contentEnd = start + pasted.length
        trimInheritedSpans(editable, start, contentEnd)
        if (pasted is Spanned) {
            copySourceSpans(pasted, editable, start)
        }
        return contentEnd.coerceAtMost(editable.length)
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
        applyEditorPadding()
        emitFormatState()
    }

    override val currentText: CharSequence
        get() = text ?: ""

    private fun tryToggleChecklistFromTouch(event: MotionEvent): Boolean {
        val editable = text ?: return false
        val layout = layout ?: return false
        val rawContentX = event.x - totalPaddingLeft + scrollX
        val contentX = rawContentX.coerceAtLeast(0f)
        val contentY = event.y - totalPaddingTop + scrollY
        val line = layout.getLineForVertical(contentY.toInt())
        if (line < 0 || line >= layout.lineCount) return false
        val offset = layout.getOffsetForHorizontal(line, contentX)
        if (offset < 0 || offset >= editable.length) return false
        val iconRange = checklistIconRangeContainingOffset(editable.toString(), offset) ?: return false
        val iconCenterX = -checklistGutterWidthPx / 2f
        val lineCenterY = (layout.getLineTop(line) + layout.getLineBottom(line)) / 2f
        val minimumTargetSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            48f,
            resources.displayMetrics
        )
        val targetLeft = iconCenterX - minimumTargetSize / 2f
        val targetRight = iconCenterX + minimumTargetSize / 2f
        val targetTop = lineCenterY - minimumTargetSize / 2f
        val targetBottom = lineCenterY + minimumTargetSize / 2f
        if (rawContentX !in targetLeft..targetRight || contentY !in targetTop..targetBottom) return false

        suppressCallbacks = true
        val result = toggleChecklistAtSelection(editable, iconRange.start, iconRange.start, paragraphSpacingPx)
        suppressCallbacks = false
        setSelection(result.selectionStart.coerceAtMost(editable.length))
        animateChecklistToggle()
        publishChanges()
        return true
    }

    private fun animateChecklistToggle() {
        animate().cancel()
        alpha = 1f
        animate()
            .alpha(0.5f)
            .setDuration(100L)
            .withEndAction {
                animate().alpha(1f).setDuration(100L).start()
            }
            .start()
    }
}

internal fun checklistIconLayoutY(
    totalPaddingTop: Float,
    lineBaseline: Float,
    alignmentOffset: Float
): Float = totalPaddingTop + lineBaseline + alignmentOffset

internal fun checklistIconContentOffset(prefixEnd: Int, textLength: Int): Int {
    if (textLength <= 0) return 0
    return prefixEnd.coerceIn(0, textLength - 1)
}

internal data class SelectionTarget(val start: Int, val end: Int)

internal fun ClipData.firstStyledText(): CharSequence? {
    for (index in 0 until itemCount) {
        val item = getItemAt(index) ?: continue
        val text = item.text
        if (text is Spanned && text.isNotEmpty()) return text

        val htmlText = item.htmlText
        if (!htmlText.isNullOrEmpty()) {
            val styledText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(htmlText)
            }
            if (styledText.isNotEmpty()) return styledText
        }

        if (!text.isNullOrEmpty()) return text

        val uriText = item.uri?.toString()
        if (!uriText.isNullOrEmpty()) return uriText

        val intentText = item.intent?.toUri(0)
        if (!intentText.isNullOrEmpty()) return intentText
    }
    return null
}

internal fun ClipData.firstPlainText(): String? {
    return firstStyledText()?.toString()
}

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
