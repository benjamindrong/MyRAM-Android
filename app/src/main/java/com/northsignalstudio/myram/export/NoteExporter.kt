package com.northsignalstudio.myram.export

import com.northsignalstudio.myram.data.Note
import com.northsignalstudio.myram.data.NotePhotoAttachment
import com.northsignalstudio.myram.data.PinnedText
import com.northsignalstudio.myram.ui.richtext.plainTextFromStoredContent
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ExportArtifact(
    val files: List<File>,
    val mimeType: String
)

object NoteExporter {
    fun exportNotes(
        notes: List<Note>,
        attachmentsByNoteId: Map<Int, List<NotePhotoAttachment>>,
        pinnedTextByNoteId: Map<Int, List<PinnedText>> = emptyMap(),
        folderPathProvider: (Note) -> List<String>,
        exportDirectory: File,
        nowMillis: Long = System.currentTimeMillis()
    ): ExportArtifact {
        val activeNotes = notes.filter { it.deletedAt == null }
        require(activeNotes.isNotEmpty()) { "No notes selected for export." }

        if (!exportDirectory.exists()) {
            exportDirectory.mkdirs()
        }

        val batchDirectory = File(exportDirectory, "batch-${UUID.randomUUID()}")
        batchDirectory.mkdirs()
        val attachmentsDirectory = File(batchDirectory, "attachments")
        attachmentsDirectory.mkdirs()

        val exportedFiles = mutableListOf<File>()
        writeNoteTextFiles(
            notes = activeNotes,
            pinnedTextByNoteId = pinnedTextByNoteId,
            batchDirectory = batchDirectory,
            exportedFiles = exportedFiles
        )
        val notesJson = buildNotesJson(
            notes = activeNotes,
            exportedAtMillis = nowMillis,
            folderPathProvider = folderPathProvider,
            attachmentsByNoteId = attachmentsByNoteId,
            pinnedTextByNoteId = pinnedTextByNoteId,
            attachmentsDirectory = attachmentsDirectory,
            exportedFiles = exportedFiles
        )

        val jsonFile = File(batchDirectory, "MyRAM-Notes-${timestampToken(nowMillis)}.json")
        jsonFile.writeText(notesJson, Charsets.UTF_8)

        return ExportArtifact(
            files = listOf(jsonFile) + exportedFiles.sortedBy { it.relativeTo(batchDirectory).path },
            mimeType = "*/*"
        )
    }

    private fun writeNoteTextFiles(
        notes: List<Note>,
        pinnedTextByNoteId: Map<Int, List<PinnedText>>,
        batchDirectory: File,
        exportedFiles: MutableList<File>
    ) {
        val usedFilenames = mutableSetOf<String>()
        notes.forEachIndexed { index, note ->
            val baseName = safePathSegment(note.title.ifBlank { "Note-${index + 1}" })
            val filename = "${uniqueFilename(baseName, usedFilenames)}.txt"
            val textFile = File(batchDirectory, filename)
            textFile.writeText(
                buildNoteText(
                    note = note,
                    pinnedText = pinnedTextByNoteId[note.id].orEmpty()
                ),
                Charsets.UTF_8
            )
            exportedFiles += textFile
        }
    }

    private fun buildNoteText(note: Note, pinnedText: List<PinnedText>): String {
        val title = note.title.trim().ifBlank { "Untitled" }
        val body = plainTextFromStoredContent(note.content).ifBlank { "(No content)" }
        val pinnedLines = pinnedText
            .sortedWith(compareBy<PinnedText> { it.sortOrder }.thenBy { it.createdAt })
            .map { it.text.trim() }
            .filter { it.isNotEmpty() }

        return buildString {
            appendLine("Title: $title")
            appendLine()
            appendLine("Pinned:")
            if (pinnedLines.isEmpty()) {
                appendLine("(None)")
            } else {
                pinnedLines.forEach { appendLine("- $it") }
            }
            appendLine()
            appendLine("Content:")
            appendLine(body)
        }.trimEnd() + "\n"
    }

    private fun buildNotesJson(
        notes: List<Note>,
        exportedAtMillis: Long,
        folderPathProvider: (Note) -> List<String>,
        attachmentsByNoteId: Map<Int, List<NotePhotoAttachment>>,
        pinnedTextByNoteId: Map<Int, List<PinnedText>>,
        attachmentsDirectory: File,
        exportedFiles: MutableList<File>
    ): String {
        val notesJson = notes.joinToString(separator = ",\n") { note ->
            val folderPath = folderPathProvider(note)
            val attachments = attachmentsByNoteId[note.id].orEmpty()
            val noteAttachmentDir = File(
                attachmentsDirectory,
                "note-${note.id}-${safePathSegment(note.title.ifBlank { "untitled" })}"
            )
            noteAttachmentDir.mkdirs()

            val attachmentJson = attachments.mapIndexed { index, attachment ->
                val attachmentFile = writeAttachmentFile(
                    attachment = attachment,
                    index = index,
                    noteAttachmentDir = noteAttachmentDir
                )
                exportedFiles += attachmentFile

                val relativePath = attachmentFile
                    .relativeTo(attachmentsDirectory.parentFile!!)
                    .path
                    .replace(File.separatorChar, '/')
                """
                {
                  "filename": "${jsonString(attachmentFile.name)}",
                  "relativePath": "${jsonString(relativePath)}",
                  "mimeType": "${jsonString(detectMimeType(attachment.imageData))}",
                  "sizeBytes": ${attachment.imageData.size},
                  "createdAt": ${attachment.createdAt}
                }
                """.trimIndent()
            }.joinToString(separator = ",\n")

            val folderPathJson = folderPath.joinToString(separator = ",") { "\"${jsonString(it)}\"" }
            val pinnedTextJson = pinnedTextByNoteId[note.id].orEmpty()
                .sortedWith(compareBy<PinnedText> { it.sortOrder }.thenBy { it.createdAt })
                .joinToString(separator = ",\n") { pinnedText ->
                    """
                    {
                      "id": ${pinnedText.id},
                      "text": "${jsonString(pinnedText.text)}",
                      "sourceContent": "${jsonString(pinnedText.sourceContent)}",
                      "sourceStart": ${pinnedText.sourceStart},
                      "sortOrder": ${pinnedText.sortOrder},
                      "createdAt": ${pinnedText.createdAt},
                      "lastModified": ${pinnedText.lastModified}
                    }
                    """.trimIndent()
                }
            """
            {
              "id": ${note.id},
              "title": "${jsonString(note.title)}",
              "content": "${jsonString(note.content)}",
              "createdAt": ${note.createdAt},
              "lastModified": ${note.lastModified},
              "folderPath": [$folderPathJson],
              "pinnedText": [${if (pinnedTextJson.isNotBlank()) "\n$pinnedTextJson\n  " else ""}],
              "attachments": [${if (attachmentJson.isNotBlank()) "\n$attachmentJson\n  " else ""}]
            }
            """.trimIndent()
        }
        return """
        {
          "formatVersion": 1,
          "exportedAt": $exportedAtMillis,
          "notes": [
        $notesJson
          ]
        }
        """.trimIndent()
    }

    private fun writeAttachmentFile(
        attachment: NotePhotoAttachment,
        index: Int,
        noteAttachmentDir: File
    ): File {
        val extension = extensionForMime(detectMimeType(attachment.imageData))
        val file = File(noteAttachmentDir, "attachment-${index + 1}.$extension")
        file.writeBytes(attachment.imageData)
        return file
    }

    private fun jsonString(value: String): String {
        return buildString(value.length + 8) {
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
        }
    }

    private fun safePathSegment(value: String): String {
        val cleaned = value.trim()
            .replace(Regex("[^a-zA-Z0-9._-]+"), "-")
            .trim('-')
            .take(40)
        return if (cleaned.isBlank()) "item" else cleaned
    }

    private fun uniqueFilename(baseName: String, used: MutableSet<String>): String {
        if (used.add(baseName)) return baseName
        var index = 2
        while (!used.add("$baseName-$index")) {
            index += 1
        }
        return "$baseName-$index"
    }

    private fun detectMimeType(data: ByteArray): String {
        if (data.size >= 3 &&
            data[0] == 0xFF.toByte() &&
            data[1] == 0xD8.toByte() &&
            data[2] == 0xFF.toByte()
        ) {
            return "image/jpeg"
        }
        if (data.size >= 8 &&
            data[0] == 0x89.toByte() &&
            data[1] == 0x50.toByte() &&
            data[2] == 0x4E.toByte() &&
            data[3] == 0x47.toByte()
        ) {
            return "image/png"
        }
        if (data.size >= 6) {
            val header = String(data.copyOfRange(0, 6), Charsets.US_ASCII)
            if (header == "GIF87a" || header == "GIF89a") {
                return "image/gif"
            }
        }
        if (data.size >= 12) {
            val riff = String(data.copyOfRange(0, 4), Charsets.US_ASCII)
            val webp = String(data.copyOfRange(8, 12), Charsets.US_ASCII)
            if (riff == "RIFF" && webp == "WEBP") {
                return "image/webp"
            }
        }
        return "application/octet-stream"
    }

    private fun extensionForMime(mimeType: String): String {
        return when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            else -> "bin"
        }
    }

    private fun timestampToken(epochMillis: Long): String {
        val format = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        return format.format(Date(epochMillis))
    }
}
