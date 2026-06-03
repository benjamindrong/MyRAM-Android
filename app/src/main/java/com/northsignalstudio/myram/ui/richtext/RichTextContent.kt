package com.northsignalstudio.myram.ui.richtext

import android.os.Parcel
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.util.Base64
import androidx.core.text.HtmlCompat

private const val RICH_TEXT_PREFIX = "[myram-rich-text-v2]\n"
private const val LEGACY_RICH_TEXT_PREFIX = "[myram-rich-text-v1]\n"

fun encodeRichTextContent(content: CharSequence): String {
    val parcel = Parcel.obtain()
    return try {
        TextUtils.writeToParcel(content, parcel, 0)
        val bytes = parcel.marshall()
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        RICH_TEXT_PREFIX + encoded
    } finally {
        parcel.recycle()
    }
}

fun decodeRichTextContent(content: String): Spanned {
    if (!content.startsWith(RICH_TEXT_PREFIX) && !content.startsWith(LEGACY_RICH_TEXT_PREFIX)) {
        return SpannableStringBuilder(content)
    }

    if (content.startsWith(RICH_TEXT_PREFIX)) {
        val encoded = content.removePrefix(RICH_TEXT_PREFIX)
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val parcel = Parcel.obtain()
        return try {
            parcel.unmarshall(bytes, 0, bytes.size)
            parcel.setDataPosition(0)
            val decoded = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel)
            SpannableStringBuilder.valueOf(decoded ?: "")
        } finally {
            parcel.recycle()
        }
    }

    val html = content.removePrefix(LEGACY_RICH_TEXT_PREFIX)
    return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
}

fun plainTextFromStoredContent(content: String): String {
    return if (content.startsWith(RICH_TEXT_PREFIX) || content.startsWith(LEGACY_RICH_TEXT_PREFIX)) {
        decodeRichTextContent(content).toString()
    } else {
        content
    }
}

fun isRichTextContent(content: String): Boolean {
    return content.startsWith(RICH_TEXT_PREFIX) || content.startsWith(LEGACY_RICH_TEXT_PREFIX)
}
