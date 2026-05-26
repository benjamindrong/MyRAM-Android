package com.apexcoretechs.myram.ui.screens

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

internal fun copySelectedText(value: TextFieldValue): String? {
    if (value.selection.collapsed) return null
    val start = value.selection.min.coerceIn(0, value.text.length)
    val end = value.selection.max.coerceIn(0, value.text.length)
    if (start >= end) return null
    return value.text.substring(start, end)
}

internal fun cutSelectedText(value: TextFieldValue): Pair<TextFieldValue, String?> {
    val selected = copySelectedText(value) ?: return value to null
    val start = value.selection.min.coerceIn(0, value.text.length)
    val end = value.selection.max.coerceIn(0, value.text.length)
    val updated = value.text.removeRange(start, end)
    return value.copy(text = updated, selection = TextRange(start)) to selected
}

internal fun pasteIntoSelection(value: TextFieldValue, pastedText: String): TextFieldValue {
    if (pastedText.isEmpty()) return value
    val start = value.selection.min.coerceIn(0, value.text.length)
    val end = value.selection.max.coerceIn(0, value.text.length)
    val updated = buildString(value.text.length + pastedText.length) {
        append(value.text.substring(0, start))
        append(pastedText)
        append(value.text.substring(end))
    }
    val cursor = start + pastedText.length
    return value.copy(text = updated, selection = TextRange(cursor))
}

internal fun selectAllText(value: TextFieldValue): TextFieldValue {
    return value.copy(selection = TextRange(0, value.text.length))
}
