package com.apexcoretechs.myram.data.dao

import androidx.room.*
import com.apexcoretechs.myram.data.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM Note ORDER BY lastModified DESC")
    fun getAll(): Flow<List<Note>>

    @Query("SELECT * FROM Note WHERE id = :id")
    fun getById(id: Int): Flow<Note?>

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)
}