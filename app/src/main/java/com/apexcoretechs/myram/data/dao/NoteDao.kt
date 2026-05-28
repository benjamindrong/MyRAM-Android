package com.apexcoretechs.myram.data.dao

import androidx.room.*
import com.apexcoretechs.myram.data.Note
import com.apexcoretechs.myram.data.NotePhotoAttachment
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM Note WHERE deletedAt IS NULL ORDER BY lastModified DESC")
    fun getAll(): Flow<List<Note>>

    @Query("SELECT * FROM Note ORDER BY lastModified DESC")
    suspend fun getAllIncludingDeleted(): List<Note>

    @Query("SELECT * FROM Note WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    suspend fun getRecentlyDeleted(): List<Note>

    @Query("SELECT * FROM Note WHERE id = :id AND deletedAt IS NULL")
    fun getById(id: Int): Flow<Note?>

    @Query("SELECT * FROM NotePhotoAttachment WHERE noteId = :noteId ORDER BY createdAt ASC")
    fun getAttachmentsForNote(noteId: Int): Flow<List<NotePhotoAttachment>>

    @Query("SELECT * FROM NotePhotoAttachment WHERE noteId IN (:noteIds) ORDER BY createdAt ASC")
    suspend fun getAttachmentsForNotes(noteIds: List<Int>): List<NotePhotoAttachment>

    @Query("DELETE FROM Note WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeDeletedBefore(cutoff: Long)

    @Query("DELETE FROM Note WHERE folderId IN (:folderIds)")
    suspend fun deleteNotesInFolders(folderIds: List<Int>)

    @Query("UPDATE Note SET folderId = NULL, lastModified = :timestamp WHERE folderId IN (:folderIds)")
    suspend fun moveNotesInFoldersToTopLevel(folderIds: List<Int>, timestamp: Long)

    @Insert
    suspend fun insert(note: Note): Long

    @Insert
    suspend fun insertAttachment(attachment: NotePhotoAttachment): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Delete
    suspend fun deleteAttachment(attachment: NotePhotoAttachment)
}
