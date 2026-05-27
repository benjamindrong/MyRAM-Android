package com.apexcoretechs.myram.export

import com.apexcoretechs.myram.data.Note
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ExportArtifact(
    val file: File,
    val mimeType: String
)

object NoteExporter {
    private const val EMPTY_BODY = "(No content)"

    fun exportNotes(
        notes: List<Note>,
        exportDirectory: File,
        nowMillis: Long = System.currentTimeMillis()
    ): ExportArtifact {
        val activeNotes = notes.filter { it.deletedAt == null }
        require(activeNotes.isNotEmpty()) { "No notes selected for export." }

        if (!exportDirectory.exists()) {
            exportDirectory.mkdirs()
        }

        return if (activeNotes.size == 1) {
            val note = activeNotes.first()
            val filename = "${safeFileStem(note.title, "Note")}-${timestampToken(nowMillis)}.txt"
            val file = File(exportDirectory, filename)
            file.writeText(
                buildNoteExportText(note = note, exportedAtMillis = nowMillis),
                Charsets.UTF_8
            )
            ExportArtifact(file = file, mimeType = "text/plain")
        } else {
            val batchDirectory = File(exportDirectory, "batch-${UUID.randomUUID()}")
            val notesDirectory = File(batchDirectory, "Notes")
            notesDirectory.mkdirs()

            val usedNames = mutableSetOf<String>()
            activeNotes.forEachIndexed { index, note ->
                val defaultStem = "Note-${index + 1}"
                val stem = uniqueStem(safeFileStem(note.title, defaultStem), usedNames)
                val file = File(notesDirectory, "$stem.txt")
                file.writeText(
                    buildNoteExportText(note = note, exportedAtMillis = nowMillis),
                    Charsets.UTF_8
                )
            }

            val zipFile = File(exportDirectory, "MyRAM-Notes-${timestampToken(nowMillis)}.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
                notesDirectory.listFiles()
                    ?.sortedBy { it.name }
                    .orEmpty()
                    .forEach { txtFile ->
                        val entry = ZipEntry("Notes/${txtFile.name}")
                        zip.putNextEntry(entry)
                        txtFile.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
            }

            ExportArtifact(file = zipFile, mimeType = "application/zip")
        }
    }

    fun buildNoteExportText(
        note: Note,
        exportedAtMillis: Long,
        dateFormatter: (Long) -> String = ::defaultDateFormatter
    ): String {
        val title = note.title.ifBlank { "Untitled" }
        val body = note.content.ifBlank { EMPTY_BODY }
        val createdAt = if (note.createdAt > 0L) note.createdAt else note.lastModified

        return buildString {
            appendLine("MyRAM Notes Export")
            appendLine("Exported: ${dateFormatter(exportedAtMillis)}")
            appendLine()
            appendLine("Title: $title")
            appendLine("Created: ${dateFormatter(createdAt)}")
            appendLine("Modified: ${dateFormatter(note.lastModified)}")
            appendLine("Body:")
            appendLine(body)
        }
    }

    fun defaultDateFormatter(epochMillis: Long): String {
        val format = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.US)
        return format.format(Date(epochMillis))
    }

    private fun timestampToken(epochMillis: Long): String {
        val format = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        return format.format(Date(epochMillis))
    }

    private fun safeFileStem(rawTitle: String, fallback: String): String {
        val cleaned = rawTitle.trim()
            .replace("/", "-")
            .replace(":", "-")
            .replace("\n", " ")
            .take(40)
            .trim()
        return if (cleaned.isBlank()) fallback else cleaned
    }

    private fun uniqueStem(base: String, used: MutableSet<String>): String {
        if (used.add(base)) return base
        var index = 2
        while (true) {
            val candidate = "$base-$index"
            if (used.add(candidate)) return candidate
            index += 1
        }
    }
}
