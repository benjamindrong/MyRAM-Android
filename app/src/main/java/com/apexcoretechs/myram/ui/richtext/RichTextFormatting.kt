package com.apexcoretechs.myram.ui.richtext

import android.graphics.Typeface
import android.text.Editable
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import kotlin.math.max
import kotlin.math.min

data class RichTextFormatState(
    val bold: Boolean,
    val italic: Boolean,
    val underline: Boolean,
    val strikethrough: Boolean,
    val textColor: Int,
    val fontSizeSp: Int
)

internal const val CHECKLIST_UNCHECKED_PREFIX = "☐ "
internal const val CHECKLIST_CHECKED_PREFIX = "☑ "
private const val CHECKLIST_CHECKED_PREFIX_VARIANT = "☑︎ "
private const val LEGACY_CHECKLIST_UNCHECKED_PREFIX = "- [ ] "
private const val LEGACY_CHECKLIST_CHECKED_PREFIX = "- [x] "
private const val LEGACY_SHORT_UNCHECKED_PREFIX = "[ ] "
private const val LEGACY_SHORT_CHECKED_PREFIX = "[x] "

internal data class ChecklistSelectionResult(
    val selectionStart: Int,
    val selectionEnd: Int
)

internal data class ChecklistTextUpdate(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int
)

internal data class ChecklistContentRange(
    val start: Int,
    val end: Int
)

fun computeRichTextFormatState(
    editable: Editable,
    selectionStart: Int,
    selectionEnd: Int,
    defaultTextColor: Int,
    defaultFontSizeSp: Int
): RichTextFormatState {
    val length = editable.length
    val safeStart = selectionStart.coerceIn(0, length)
    val safeEnd = selectionEnd.coerceIn(0, length)
    val probeIndex = when {
        safeStart < length -> safeStart
        length > 0 -> length - 1
        else -> 0
    }

    val bold = if (safeStart == safeEnd) {
        hasStyleAt(editable, probeIndex, Typeface.BOLD)
    } else {
        isStyleAppliedAcrossRange(editable, safeStart, safeEnd, Typeface.BOLD)
    }
    val italic = if (safeStart == safeEnd) {
        hasStyleAt(editable, probeIndex, Typeface.ITALIC)
    } else {
        isStyleAppliedAcrossRange(editable, safeStart, safeEnd, Typeface.ITALIC)
    }
    val underline = if (safeStart == safeEnd) {
        hasUnderlineAt(editable, probeIndex)
    } else {
        isUnderlineAcrossRange(editable, safeStart, safeEnd)
    }
    val strikethrough = if (safeStart == safeEnd) {
        hasStrikethroughAt(editable, probeIndex)
    } else {
        isStrikethroughAcrossRange(editable, safeStart, safeEnd)
    }

    val textColor = colorAt(editable, probeIndex, defaultTextColor)
    val fontSize = fontSizeAt(editable, probeIndex, defaultFontSizeSp)

    return RichTextFormatState(
        bold = bold,
        italic = italic,
        underline = underline,
        strikethrough = strikethrough,
        textColor = textColor,
        fontSizeSp = fontSize
    )
}

fun toggleBold(editable: Editable, selectionStart: Int, selectionEnd: Int) {
    toggleStyleSpan(editable, selectionStart, selectionEnd, Typeface.BOLD)
}

fun toggleItalic(editable: Editable, selectionStart: Int, selectionEnd: Int) {
    toggleStyleSpan(editable, selectionStart, selectionEnd, Typeface.ITALIC)
}

fun toggleUnderline(editable: Editable, selectionStart: Int, selectionEnd: Int) {
    toggleDecorationSpan(
        editable = editable,
        selectionStart = selectionStart,
        selectionEnd = selectionEnd,
        isAppliedAcrossRange = ::isUnderlineAcrossRange,
        hasAt = ::hasUnderlineAt,
        removeRange = { text, start, end ->
            removeSpanFromRange(text, start, end, UnderlineSpan::class.java) { UnderlineSpan() }
        },
        applyRange = { text, start, end ->
            text.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        },
        removeCursorSpans = { text, index ->
            removeCursorSpan(text, index, UnderlineSpan::class.java)
        }
    )
}

fun toggleStrikethrough(editable: Editable, selectionStart: Int, selectionEnd: Int) {
    toggleDecorationSpan(
        editable = editable,
        selectionStart = selectionStart,
        selectionEnd = selectionEnd,
        isAppliedAcrossRange = ::isStrikethroughAcrossRange,
        hasAt = ::hasStrikethroughAt,
        removeRange = { text, start, end ->
            removeSpanFromRange(text, start, end, StrikethroughSpan::class.java) { StrikethroughSpan() }
        },
        applyRange = { text, start, end ->
            text.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        },
        removeCursorSpans = { text, index ->
            removeCursorSpan(text, index, StrikethroughSpan::class.java)
        }
    )
}

fun applyTextColor(
    editable: Editable,
    selectionStart: Int,
    selectionEnd: Int,
    color: Int
) {
    val (start, end) = normalizeRange(selectionStart, selectionEnd, editable.length)
    if (start == end) {
        removeCursorSpan(editable, start, ForegroundColorSpan::class.java)
        editable.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
    } else {
        removeSpanFromRange(editable, start, end, ForegroundColorSpan::class.java) {
            ForegroundColorSpan(it.foregroundColor)
        }
        editable.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
    }
}

fun clearTextColor(
    editable: Editable,
    selectionStart: Int,
    selectionEnd: Int
) {
    val (start, end) = normalizeRange(selectionStart, selectionEnd, editable.length)
    if (start == end) {
        removeCursorSpan(editable, start, ForegroundColorSpan::class.java)
    } else {
        removeSpanFromRange(editable, start, end, ForegroundColorSpan::class.java) {
            ForegroundColorSpan(it.foregroundColor)
        }
    }
}

fun applyFontSize(
    editable: Editable,
    selectionStart: Int,
    selectionEnd: Int,
    fontSizeSp: Int
) {
    val (start, end) = normalizeRange(selectionStart, selectionEnd, editable.length)
    if (start == end) {
        removeCursorSpan(editable, start, AbsoluteSizeSpan::class.java)
        editable.setSpan(AbsoluteSizeSpan(fontSizeSp, true), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
    } else {
        removeSpanFromRange(editable, start, end, AbsoluteSizeSpan::class.java) {
            AbsoluteSizeSpan(it.size, it.dip)
        }
        editable.setSpan(
            AbsoluteSizeSpan(fontSizeSp, true),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_INCLUSIVE
        )
    }
}

fun applyChecklistStrikeThrough(editable: Editable) {
    normalizeLegacyChecklistPrefixes(editable)
    val existing = editable.getSpans(0, editable.length, ChecklistStrikeThroughSpan::class.java)
    existing.forEach(editable::removeSpan)

    checkedChecklistContentRanges(editable.toString()).forEach { range ->
        if (range.start < range.end) {
            editable.setSpan(
                ChecklistStrikeThroughSpan(),
                range.start,
                range.end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
}

internal fun toggleChecklistAtSelection(
    editable: Editable,
    selectionStart: Int,
    selectionEnd: Int
): ChecklistSelectionResult {
    val text = editable.toString()
    val length = text.length
    val safeCursor = min(selectionStart, selectionEnd).coerceIn(0, length)
    val lineStart = text.lastIndexOf('\n', startIndex = (safeCursor - 1).coerceAtLeast(0))
        .let { if (it == -1) 0 else it + 1 }
    val lineEnd = text.indexOf('\n', startIndex = safeCursor).let { if (it == -1) length else it }
    val line = text.substring(lineStart, lineEnd)

    val replacement = when {
        line.startsWith(CHECKLIST_CHECKED_PREFIX) -> ChecklistPrefixReplacement(
            start = lineStart,
            end = lineStart + CHECKLIST_CHECKED_PREFIX.length,
            replacement = CHECKLIST_UNCHECKED_PREFIX
        )
        line.startsWith(CHECKLIST_CHECKED_PREFIX_VARIANT) -> ChecklistPrefixReplacement(
            start = lineStart,
            end = lineStart + CHECKLIST_CHECKED_PREFIX_VARIANT.length,
            replacement = CHECKLIST_UNCHECKED_PREFIX
        )
        line.startsWith(CHECKLIST_UNCHECKED_PREFIX) -> ChecklistPrefixReplacement(
            start = lineStart,
            end = lineStart + CHECKLIST_UNCHECKED_PREFIX.length,
            replacement = CHECKLIST_CHECKED_PREFIX
        )
        line.startsWith(LEGACY_CHECKLIST_CHECKED_PREFIX, ignoreCase = true) -> ChecklistPrefixReplacement(
            start = lineStart,
            end = lineStart + LEGACY_CHECKLIST_CHECKED_PREFIX.length,
            replacement = CHECKLIST_UNCHECKED_PREFIX
        )
        line.startsWith(LEGACY_SHORT_CHECKED_PREFIX, ignoreCase = true) -> ChecklistPrefixReplacement(
            start = lineStart,
            end = lineStart + LEGACY_SHORT_CHECKED_PREFIX.length,
            replacement = CHECKLIST_UNCHECKED_PREFIX
        )
        line.startsWith(LEGACY_CHECKLIST_UNCHECKED_PREFIX) -> ChecklistPrefixReplacement(
            start = lineStart,
            end = lineStart + LEGACY_CHECKLIST_UNCHECKED_PREFIX.length,
            replacement = CHECKLIST_CHECKED_PREFIX
        )
        line.startsWith(LEGACY_SHORT_UNCHECKED_PREFIX) -> ChecklistPrefixReplacement(
            start = lineStart,
            end = lineStart + LEGACY_SHORT_UNCHECKED_PREFIX.length,
            replacement = CHECKLIST_CHECKED_PREFIX
        )
        else -> ChecklistPrefixReplacement(
            start = lineStart,
            end = lineStart,
            replacement = CHECKLIST_UNCHECKED_PREFIX
        )
    }

    editable.replace(replacement.start, replacement.end, replacement.replacement)
    applyChecklistStrikeThrough(editable)

    val oldEnd = replacement.end
    val newEnd = replacement.start + replacement.replacement.length
    val delta = replacement.replacement.length - (replacement.end - replacement.start)
    val newCursor = when {
        safeCursor < replacement.start -> safeCursor
        safeCursor <= oldEnd -> newEnd
        else -> safeCursor + delta
    }.coerceIn(0, editable.length)

    return ChecklistSelectionResult(newCursor, newCursor)
}

internal fun toggleChecklistInText(
    text: String,
    selectionStart: Int,
    selectionEnd: Int
): ChecklistTextUpdate {
    val length = text.length
    val safeCursor = min(selectionStart, selectionEnd).coerceIn(0, length)
    val lineStart = text.lastIndexOf('\n', startIndex = (safeCursor - 1).coerceAtLeast(0))
        .let { if (it == -1) 0 else it + 1 }
    val lineEnd = text.indexOf('\n', startIndex = safeCursor).let { if (it == -1) length else it }
    val line = text.substring(lineStart, lineEnd)

    val replacement = when {
        line.startsWith(CHECKLIST_CHECKED_PREFIX) -> ChecklistPrefixReplacement(
            start = lineStart,
            end = lineStart + CHECKLIST_CHECKED_PREFIX.length,
            replacement = CHECKLIST_UNCHECKED_PREFIX
        )
        line.startsWith(CHECKLIST_CHECKED_PREFIX_VARIANT) -> ChecklistPrefixReplacement(
            start = lineStart,
            end = lineStart + CHECKLIST_CHECKED_PREFIX_VARIANT.length,
            replacement = CHECKLIST_UNCHECKED_PREFIX
        )
        line.startsWith(CHECKLIST_UNCHECKED_PREFIX) -> ChecklistPrefixReplacement(
            start = lineStart,
            end = lineStart + CHECKLIST_UNCHECKED_PREFIX.length,
            replacement = CHECKLIST_CHECKED_PREFIX
        )
        line.startsWith(LEGACY_CHECKLIST_CHECKED_PREFIX, ignoreCase = true) -> ChecklistPrefixReplacement(
            start = lineStart,
            end = lineStart + LEGACY_CHECKLIST_CHECKED_PREFIX.length,
            replacement = CHECKLIST_UNCHECKED_PREFIX
        )
        line.startsWith(LEGACY_SHORT_CHECKED_PREFIX, ignoreCase = true) -> ChecklistPrefixReplacement(
            start = lineStart,
            end = lineStart + LEGACY_SHORT_CHECKED_PREFIX.length,
            replacement = CHECKLIST_UNCHECKED_PREFIX
        )
        line.startsWith(LEGACY_CHECKLIST_UNCHECKED_PREFIX) -> ChecklistPrefixReplacement(
            start = lineStart,
            end = lineStart + LEGACY_CHECKLIST_UNCHECKED_PREFIX.length,
            replacement = CHECKLIST_CHECKED_PREFIX
        )
        line.startsWith(LEGACY_SHORT_UNCHECKED_PREFIX) -> ChecklistPrefixReplacement(
            start = lineStart,
            end = lineStart + LEGACY_SHORT_UNCHECKED_PREFIX.length,
            replacement = CHECKLIST_CHECKED_PREFIX
        )
        else -> ChecklistPrefixReplacement(
            start = lineStart,
            end = lineStart,
            replacement = CHECKLIST_UNCHECKED_PREFIX
        )
    }

    val updatedText = buildString {
        append(text, 0, replacement.start)
        append(replacement.replacement)
        append(text, replacement.end, text.length)
    }
    val oldEnd = replacement.end
    val newEnd = replacement.start + replacement.replacement.length
    val delta = replacement.replacement.length - (replacement.end - replacement.start)
    val newCursor = when {
        safeCursor < replacement.start -> safeCursor
        safeCursor <= oldEnd -> newEnd
        else -> safeCursor + delta
    }.coerceIn(0, updatedText.length)

    return ChecklistTextUpdate(
        text = updatedText,
        selectionStart = newCursor,
        selectionEnd = newCursor
    )
}

internal fun isChecklistIconAtOffset(text: String, offset: Int): Boolean {
    if (text.isEmpty()) return false
    val safeOffset = offset.coerceIn(0, text.length - 1)
    val lineStart = text.lastIndexOf('\n', startIndex = (safeOffset - 1).coerceAtLeast(0))
        .let { if (it == -1) 0 else it + 1 }
    val lineEnd = text.indexOf('\n', startIndex = safeOffset).let { if (it == -1) text.length else it }
    val line = text.substring(lineStart, lineEnd)
    val prefixLength = when {
        line.startsWith(CHECKLIST_UNCHECKED_PREFIX) -> CHECKLIST_UNCHECKED_PREFIX.length
        line.startsWith(CHECKLIST_CHECKED_PREFIX) -> CHECKLIST_CHECKED_PREFIX.length
        line.startsWith(CHECKLIST_CHECKED_PREFIX_VARIANT) -> CHECKLIST_CHECKED_PREFIX_VARIANT.length
        else -> return false
    }
    return safeOffset < lineStart + prefixLength
}

internal fun checkedChecklistContentRanges(text: String): List<ChecklistContentRange> {
    if (text.isEmpty()) return emptyList()
    val ranges = mutableListOf<ChecklistContentRange>()
    var lineStart = 0
    while (lineStart <= text.length) {
        val lineEnd = text.indexOf('\n', startIndex = lineStart).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)
        val contentStart = when {
            line.startsWith(CHECKLIST_CHECKED_PREFIX) -> lineStart + CHECKLIST_CHECKED_PREFIX.length
            line.startsWith(CHECKLIST_CHECKED_PREFIX_VARIANT) -> lineStart + CHECKLIST_CHECKED_PREFIX_VARIANT.length
            else -> -1
        }
        if (contentStart in lineStart..lineEnd) {
            ranges.add(ChecklistContentRange(start = contentStart, end = lineEnd))
        }
        if (lineEnd >= text.length) break
        lineStart = lineEnd + 1
    }
    return ranges
}

private fun normalizeLegacyChecklistPrefixes(editable: Editable) {
    val text = editable.toString()
    if (text.isEmpty()) return

    var lineStart = 0
    val replacements = mutableListOf<ChecklistPrefixReplacement>()
    while (lineStart <= text.length) {
        val lineEnd = text.indexOf('\n', startIndex = lineStart).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)
        val replacement = when {
            line.startsWith(LEGACY_CHECKLIST_CHECKED_PREFIX, ignoreCase = true) -> ChecklistPrefixReplacement(
                start = lineStart,
                end = lineStart + LEGACY_CHECKLIST_CHECKED_PREFIX.length,
                replacement = CHECKLIST_CHECKED_PREFIX
            )
            line.startsWith(LEGACY_CHECKLIST_UNCHECKED_PREFIX) -> ChecklistPrefixReplacement(
                start = lineStart,
                end = lineStart + LEGACY_CHECKLIST_UNCHECKED_PREFIX.length,
                replacement = CHECKLIST_UNCHECKED_PREFIX
            )
            line.startsWith(LEGACY_SHORT_CHECKED_PREFIX, ignoreCase = true) -> ChecklistPrefixReplacement(
                start = lineStart,
                end = lineStart + LEGACY_SHORT_CHECKED_PREFIX.length,
                replacement = CHECKLIST_CHECKED_PREFIX
            )
            line.startsWith(LEGACY_SHORT_UNCHECKED_PREFIX) -> ChecklistPrefixReplacement(
                start = lineStart,
                end = lineStart + LEGACY_SHORT_UNCHECKED_PREFIX.length,
                replacement = CHECKLIST_UNCHECKED_PREFIX
            )
            else -> null
        }
        if (replacement != null) {
            replacements.add(replacement)
        }
        if (lineEnd >= text.length) break
        lineStart = lineEnd + 1
    }

    replacements.asReversed().forEach { replacement ->
        editable.replace(replacement.start, replacement.end, replacement.replacement)
    }
}

private data class ChecklistPrefixReplacement(
    val start: Int,
    val end: Int,
    val replacement: String
)

private fun toggleStyleSpan(
    editable: Editable,
    selectionStart: Int,
    selectionEnd: Int,
    requestedStyle: Int
) {
    val (start, end) = normalizeRange(selectionStart, selectionEnd, editable.length)
    if (start == end) {
        if (hasStyleAt(editable, start, requestedStyle)) {
            removeCursorMatchingStyle(editable, start, requestedStyle)
        } else {
            editable.setSpan(
                StyleSpan(requestedStyle),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_INCLUSIVE
            )
        }
        return
    }

    val fullyApplied = isStyleAppliedAcrossRange(editable, start, end, requestedStyle)
    if (fullyApplied) {
        removeStyleFromRange(editable, start, end, requestedStyle)
    } else {
        removeStyleFromRange(editable, start, end, requestedStyle)
        editable.setSpan(
            StyleSpan(requestedStyle),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_INCLUSIVE
        )
    }
}

private fun toggleDecorationSpan(
    editable: Editable,
    selectionStart: Int,
    selectionEnd: Int,
    isAppliedAcrossRange: (Editable, Int, Int) -> Boolean,
    hasAt: (Editable, Int) -> Boolean,
    removeRange: (Editable, Int, Int) -> Unit,
    applyRange: (Editable, Int, Int) -> Unit,
    removeCursorSpans: (Editable, Int) -> Unit
) {
    val (start, end) = normalizeRange(selectionStart, selectionEnd, editable.length)
    if (start == end) {
        if (hasAt(editable, start)) {
            removeCursorSpans(editable, start)
        } else {
            applyRange(editable, start, end)
        }
        return
    }

    val fullyApplied = isAppliedAcrossRange(editable, start, end)
    if (fullyApplied) {
        removeRange(editable, start, end)
    } else {
        removeRange(editable, start, end)
        applyRange(editable, start, end)
    }
}

private fun normalizeRange(start: Int, end: Int, length: Int): Pair<Int, Int> {
    val safeStart = start.coerceIn(0, length)
    val safeEnd = end.coerceIn(0, length)
    return min(safeStart, safeEnd) to max(safeStart, safeEnd)
}

private fun hasStyleAt(editable: Editable, index: Int, requestedStyle: Int): Boolean {
    val probe = index.coerceIn(0, editable.length)
    val spans = editable.getSpans(probe, probe, StyleSpan::class.java)
    return spans.any { span -> (span.style and requestedStyle) == requestedStyle }
}

private fun isStyleAppliedAcrossRange(
    editable: Editable,
    start: Int,
    end: Int,
    requestedStyle: Int
): Boolean {
    if (start >= end) return hasStyleAt(editable, start, requestedStyle)
    for (index in start until end) {
        if (!hasStyleAt(editable, index, requestedStyle)) return false
    }
    return true
}

private fun removeStyleFromRange(editable: Editable, start: Int, end: Int, requestedStyle: Int) {
    val spans = editable.getSpans(start, end, StyleSpan::class.java)
    spans.forEach { span ->
        val spanStart = editable.getSpanStart(span)
        val spanEnd = editable.getSpanEnd(span)
        if (spanStart == -1 || spanEnd == -1) return@forEach
        val flags = editable.getSpanFlags(span)
        editable.removeSpan(span)

        val overlapStart = max(spanStart, start)
        val overlapEnd = min(spanEnd, end)
        val overlapStyle = span.style and requestedStyle.inv()

        if (spanStart < overlapStart) {
            editable.setSpan(StyleSpan(span.style), spanStart, overlapStart, flags)
        }
        if (overlapStyle != 0 && overlapStart < overlapEnd) {
            editable.setSpan(StyleSpan(overlapStyle), overlapStart, overlapEnd, flags)
        }
        if (overlapEnd < spanEnd) {
            editable.setSpan(StyleSpan(span.style), overlapEnd, spanEnd, flags)
        }
    }
}

private fun removeCursorMatchingStyle(editable: Editable, index: Int, requestedStyle: Int) {
    val spans = editable.getSpans(index, index, StyleSpan::class.java)
    spans.forEach { span ->
        if ((span.style and requestedStyle) != requestedStyle) return@forEach
        val spanStart = editable.getSpanStart(span)
        val spanEnd = editable.getSpanEnd(span)
        val flags = editable.getSpanFlags(span)
        editable.removeSpan(span)

        val remainingStyle = span.style and requestedStyle.inv()
        if (remainingStyle == 0) {
            if (spanStart < index) {
                editable.setSpan(StyleSpan(span.style), spanStart, index, flags)
            }
            if (spanEnd > index) {
                editable.setSpan(StyleSpan(span.style), index, spanEnd, flags)
            }
        } else {
            editable.setSpan(StyleSpan(remainingStyle), spanStart, spanEnd, flags)
        }
    }
}

private fun hasUnderlineAt(editable: Editable, index: Int): Boolean {
    return editable.getSpans(index, index, UnderlineSpan::class.java).isNotEmpty()
}

private fun hasStrikethroughAt(editable: Editable, index: Int): Boolean {
    val strikethrough = editable.getSpans(index, index, StrikethroughSpan::class.java)
    val checklistStrike = editable.getSpans(index, index, ChecklistStrikeThroughSpan::class.java)
    return strikethrough.isNotEmpty() || checklistStrike.isNotEmpty()
}

private fun isUnderlineAcrossRange(editable: Editable, start: Int, end: Int): Boolean {
    if (start >= end) return hasUnderlineAt(editable, start)
    for (index in start until end) {
        if (!hasUnderlineAt(editable, index)) return false
    }
    return true
}

private fun isStrikethroughAcrossRange(editable: Editable, start: Int, end: Int): Boolean {
    if (start >= end) return hasStrikethroughAt(editable, start)
    for (index in start until end) {
        if (!hasStrikethroughAt(editable, index)) return false
    }
    return true
}

private fun colorAt(editable: Editable, index: Int, defaultColor: Int): Int {
    val spans = editable.getSpans(index, index, ForegroundColorSpan::class.java)
    return spans.lastOrNull()?.foregroundColor ?: defaultColor
}

private fun fontSizeAt(editable: Editable, index: Int, defaultSizeSp: Int): Int {
    val spans = editable.getSpans(index, index, AbsoluteSizeSpan::class.java)
    return spans.lastOrNull()?.let { if (it.dip) it.size else defaultSizeSp } ?: defaultSizeSp
}

private fun <T> removeSpanFromRange(
    editable: Editable,
    start: Int,
    end: Int,
    clazz: Class<T>,
    clone: (T) -> Any
) {
    val spans = editable.getSpans(start, end, clazz)
    spans.forEach { span ->
        val spanStart = editable.getSpanStart(span)
        val spanEnd = editable.getSpanEnd(span)
        if (spanStart == -1 || spanEnd == -1) return@forEach
        val flags = editable.getSpanFlags(span)
        editable.removeSpan(span)
        if (spanStart < start) {
            editable.setSpan(clone(span), spanStart, start, flags)
        }
        if (spanEnd > end) {
            editable.setSpan(clone(span), end, spanEnd, flags)
        }
    }
}

private fun <T> removeCursorSpan(editable: Editable, index: Int, clazz: Class<T>) {
    val spans = editable.getSpans(index, index, clazz)
    spans.forEach { span ->
        val spanStart = editable.getSpanStart(span)
        val spanEnd = editable.getSpanEnd(span)
        if (spanStart <= index && spanEnd >= index) {
            editable.removeSpan(span)
        }
    }
}

private class ChecklistStrikeThroughSpan : StrikethroughSpan()
