package com.apexcoretechs.myram.data.dao

import androidx.room.*
import com.apexcoretechs.myram.data.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM Note WHERE deletedAt IS NULL ORDER BY lastModified DESC")
    fun getAll(): Flow<List<Note>>

    @Query("SELECT * FROM Note WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    suspend fun getRecentlyDeleted(): List<Note>

    @Query("SELECT * FROM Note WHERE id = :id AND deletedAt IS NULL")
    fun getById(id: Int): Flow<Note?>

    @Query("DELETE FROM Note WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeDeletedBefore(cutoff: Long)

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)
}
