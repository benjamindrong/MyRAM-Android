package com.apexcoretechs.myram.data.dao

import androidx.room.*
import com.apexcoretechs.myram.data.Folder
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM Folder ORDER BY modifiedAt DESC, name ASC")
    fun getAll(): Flow<List<Folder>>

    @Insert
    suspend fun insert(folder: Folder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(folder: Folder)

    @Update
    suspend fun update(folder: Folder)

    @Query("SELECT * FROM Folder WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Folder?

    @Delete
    suspend fun delete(folder: Folder)
}
