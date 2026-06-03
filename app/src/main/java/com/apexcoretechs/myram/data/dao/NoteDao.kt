package com.apexcoretechs.myram.data.dao

import androidx.room.*
import com.apexcoretechs.myram.data.Note
import com.apexcoretechs.myram.data.NotePhotoAttachment
import com.apexcoretechs.myram.data.PinnedText
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM Note WHERE deletedAt IS NULL ORDER BY isPinned DESC, lastModified DESC")
    fun getAll(): Flow<List<Note>>

    @Query("SELECT * FROM Note ORDER BY isPinned DESC, lastModified DESC")
    suspend fun getAllIncludingDeleted(): List<Note>

    @Query("SELECT * FROM Note WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    suspend fun getRecentlyDeleted(): List<Note>

    @Query("SELECT * FROM Note WHERE id = :id AND deletedAt IS NULL")
    fun getById(id: Int): Flow<Note?>

    @Query("SELECT * FROM Note WHERE id = :id LIMIT 1")
    suspend fun getByIdIncludingDeleted(id: Int): Note?

    @Query("SELECT * FROM NotePhotoAttachment WHERE noteId = :noteId ORDER BY createdAt ASC")
    fun getAttachmentsForNote(noteId: Int): Flow<List<NotePhotoAttachment>>

    @Query("SELECT * FROM NotePhotoAttachment WHERE noteId IN (:noteIds) ORDER BY createdAt ASC")
    suspend fun getAttachmentsForNotes(noteIds: List<Int>): List<NotePhotoAttachment>

    @Query("SELECT * FROM PinnedText WHERE noteId = :noteId ORDER BY sortOrder ASC, createdAt ASC")
    fun getPinnedTextForNote(noteId: Int): Flow<List<PinnedText>>

    @Query("SELECT * FROM PinnedText WHERE noteId IN (:noteIds) ORDER BY noteId ASC, sortOrder ASC, createdAt ASC")
    fun getPinnedTextForNotes(noteIds: List<Int>): Flow<List<PinnedText>>

    @Query("SELECT * FROM PinnedText WHERE noteId IN (:noteIds) ORDER BY noteId ASC, sortOrder ASC, createdAt ASC")
    suspend fun getPinnedTextForNotesOnce(noteIds: List<Int>): List<PinnedText>

    @Query("DELETE FROM PinnedText WHERE noteId IN (:noteIds)")
    suspend fun deletePinnedTextForNotes(noteIds: List<Int>)

    @Query("DELETE FROM Note WHERE id IN (:noteIds)")
    suspend fun deleteNotesById(noteIds: List<Int>)

    @Query("DELETE FROM Note WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeDeletedBefore(cutoff: Long)

    @Query("UPDATE Note SET folderId = NULL, lastModified = :timestamp WHERE folderId IN (:folderIds)")
    suspend fun moveNotesInFoldersToTopLevel(folderIds: List<Int>, timestamp: Long)

    @Query(
        "UPDATE Note SET folderId = NULL, deletedAt = :timestamp, lastModified = :timestamp WHERE folderId IN (:folderIds)"
    )
    suspend fun softDeleteNotesInFolders(folderIds: List<Int>, timestamp: Long)

    @Insert
    suspend fun insert(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: Note)

    @Insert
    suspend fun insertAttachment(attachment: NotePhotoAttachment): Long

    @Insert
    suspend fun insertPinnedText(pinnedText: PinnedText): Long

    @Transaction
    suspend fun replaceNotesWithPinnedText(notes: List<Note>, pinnedText: List<PinnedText>) {
        val noteIds = notes.map { it.id }
        if (noteIds.isEmpty()) return

        deletePinnedTextForNotes(noteIds)
        deleteNotesById(noteIds)
        notes.forEach { upsert(it) }
        pinnedText.forEach { insertPinnedText(it) }
    }

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Delete
    suspend fun deleteAttachment(attachment: NotePhotoAttachment)

    @Update
    suspend fun updatePinnedText(pinnedText: PinnedText)

    @Delete
    suspend fun deletePinnedText(pinnedText: PinnedText)
}
