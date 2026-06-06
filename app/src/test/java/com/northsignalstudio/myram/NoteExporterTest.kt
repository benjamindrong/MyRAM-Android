package com.northsignalstudio.myram

import com.northsignalstudio.myram.data.Note
import com.northsignalstudio.myram.data.NotePhotoAttachment
import com.northsignalstudio.myram.data.PinnedText
import com.northsignalstudio.myram.export.NoteExporter
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteExporterTest {

    @Test
    fun exportNotes_singleNote_createsJsonWithContentAndNoAttachments() {
        val directory = makeTempDirectory()
        try {
            val note = Note(
                id = 1,
                title = "Daily Log",
                content = "Body content",
                createdAt = 1_000L,
                lastModified = 2_000L
            )

            val artifact = NoteExporter.exportNotes(
                notes = listOf(note),
                attachmentsByNoteId = emptyMap(),
                folderPathProvider = { emptyList() },
                exportDirectory = directory,
                nowMillis = 4_000L
            )

            assertEquals("*/*", artifact.mimeType)
            val jsonFile = artifact.files.first { it.extension.lowercase() == "json" }
            val textFile = artifact.files.first { it.extension.lowercase() == "txt" }
            val json = jsonFile.readText(Charsets.UTF_8)
            val text = textFile.readText(Charsets.UTF_8)
            assertTrue(json.contains("\"title\": \"Daily Log\""))
            assertTrue(json.contains("\"content\": \"Body content\""))
            assertTrue(json.contains("\"attachments\": []"))
            assertTrue(text.contains("Title: Daily Log"))
            assertTrue(text.contains("Pinned:\n(None)"))
            assertTrue(text.contains("Content:\nBody content"))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun exportNotes_includesAttachmentFilesAndMetadata() {
        val directory = makeTempDirectory()
        try {
            val note = Note(
                id = 5,
                title = "Trip",
                content = "Remember this",
                createdAt = 1_000L,
                lastModified = 2_000L
            )
            val jpeg = byteArrayOf(
                0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(),
                0x00, 0x01, 0x02
            )
            val attachment = NotePhotoAttachment(
                id = 7,
                noteId = 5,
                imageData = jpeg,
                createdAt = 3_000L
            )

            val artifact = NoteExporter.exportNotes(
                notes = listOf(note),
                attachmentsByNoteId = mapOf(5 to listOf(attachment)),
                folderPathProvider = { listOf("Work", "Ideas") },
                exportDirectory = directory,
                nowMillis = 5_000L
            )

            val jsonFile = artifact.files.first { it.extension.lowercase() == "json" }
            val attachmentFiles = artifact.files.filter { it.extension.lowercase() == "jpg" }
            val textFiles = artifact.files.filter { it.extension.lowercase() == "txt" }

            assertEquals(1, attachmentFiles.size)
            assertEquals(1, textFiles.size)
            assertTrue(attachmentFiles.first().name.endsWith(".jpg"))
            assertTrue(attachmentFiles.first().readBytes().contentEquals(jpeg))

            val json = jsonFile.readText(Charsets.UTF_8)
            assertTrue(json.contains("\"folderPath\": [\"Work\",\"Ideas\"]"))
            assertTrue(json.contains("\"mimeType\": \"image/jpeg\""))
            assertTrue(json.contains("\"filename\": \"${attachmentFiles.first().name}\""))
            assertFalse(json.contains("\"body\""))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun exportNotes_includesPinnedTextInOrder() {
        val directory = makeTempDirectory()
        try {
            val note = Note(
                id = 8,
                title = "Plan",
                content = "Body stays here",
                createdAt = 1_000L,
                lastModified = 2_000L
            )
            val pinned = listOf(
                PinnedText(id = 2, noteId = 8, text = "Second", sortOrder = 1, createdAt = 11L, lastModified = 12L),
                PinnedText(id = 1, noteId = 8, text = "First", sortOrder = 0, createdAt = 9L, lastModified = 10L)
            )

            val artifact = NoteExporter.exportNotes(
                notes = listOf(note),
                attachmentsByNoteId = emptyMap(),
                pinnedTextByNoteId = mapOf(8 to pinned),
                folderPathProvider = { emptyList() },
                exportDirectory = directory,
                nowMillis = 4_000L
            )

            val json = artifact.files.first { it.extension.lowercase() == "json" }.readText(Charsets.UTF_8)
            val text = artifact.files.first { it.extension.lowercase() == "txt" }.readText(Charsets.UTF_8)
            assertTrue(json.contains("\"pinnedText\": ["))
            assertTrue(json.indexOf("\"text\": \"First\"") < json.indexOf("\"text\": \"Second\""))
            assertTrue(json.contains("\"content\": \"Body stays here\""))
            assertTrue(text.contains("Title: Plan"))
            assertTrue(text.contains("Pinned:\n- First\n- Second"))
            assertTrue(text.contains("Content:\nBody stays here"))
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun makeTempDirectory(): File {
        return Files.createTempDirectory("myram-export-test").toFile()
    }
}
