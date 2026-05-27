package com.apexcoretechs.myram

import com.apexcoretechs.myram.data.Note
import com.apexcoretechs.myram.export.NoteExporter
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteExporterTest {

    @Test
    fun buildNoteExportText_includesTitleTimestampsAndBody() {
        val note = Note(
            id = 1,
            title = "Trip Plan",
            content = "Book flights",
            createdAt = 1_000L,
            lastModified = 2_000L
        )

        val text = NoteExporter.buildNoteExportText(
            note = note,
            exportedAtMillis = 3_000L,
            dateFormatter = { millis -> "TS-$millis" }
        )

        assertTrue(text.contains("MyRAM Notes Export"))
        assertTrue(text.contains("Exported: TS-3000"))
        assertTrue(text.contains("Title: Trip Plan"))
        assertTrue(text.contains("Created: TS-1000"))
        assertTrue(text.contains("Modified: TS-2000"))
        assertTrue(text.contains("Body:\nBook flights"))
    }

    @Test
    fun exportNotes_singleNote_createsUtf8TextFile() {
        val directory = makeTempDirectory()
        try {
            val note = Note(
                id = 1,
                title = "Daily Log",
                content = "UTF-8 test ✅",
                createdAt = 1_000L,
                lastModified = 2_000L
            )

            val artifact = NoteExporter.exportNotes(
                notes = listOf(note),
                exportDirectory = directory,
                nowMillis = 4_000L
            )

            assertEquals("text/plain", artifact.mimeType)
            assertEquals("txt", artifact.file.extension.lowercase())
            val text = artifact.file.readText(Charsets.UTF_8)
            assertTrue(text.contains("Title: Daily Log"))
            assertTrue(text.contains("UTF-8 test ✅"))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun exportNotes_multipleNotes_createsZipWithSeparateTextFiles() {
        val directory = makeTempDirectory()
        try {
            val noteA = Note(
                id = 1,
                title = "First Note",
                content = "Body A",
                createdAt = 1_000L,
                lastModified = 2_000L
            )
            val noteB = Note(
                id = 2,
                title = "Second Note",
                content = "Body B",
                createdAt = 1_500L,
                lastModified = 2_500L
            )

            val artifact = NoteExporter.exportNotes(
                notes = listOf(noteA, noteB),
                exportDirectory = directory,
                nowMillis = 5_000L
            )

            assertEquals("application/zip", artifact.mimeType)
            assertEquals("zip", artifact.file.extension.lowercase())

            val entries = unzipEntries(artifact.file)
            assertTrue(entries.containsKey("Notes/First Note.txt"))
            assertTrue(entries.containsKey("Notes/Second Note.txt"))
            assertTrue(entries["Notes/First Note.txt"]!!.contains("Body A"))
            assertTrue(entries["Notes/Second Note.txt"]!!.contains("Body B"))
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun unzipEntries(zipFile: File): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        ZipInputStream(zipFile.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val bytes = zip.readBytes()
                entries[entry.name] = String(bytes, StandardCharsets.UTF_8)
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return entries
    }

    private fun makeTempDirectory(): File {
        return Files.createTempDirectory("myram-export-test").toFile()
    }
}
