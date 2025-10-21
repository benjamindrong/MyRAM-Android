package com.apexcoretechs.myram.data.dao

import androidx.room.*
import com.apexcoretechs.myram.data.Folder
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM Folder ORDER BY name")
    fun getAll(): Flow<List<Folder>>

    @Insert
    suspend fun insert(folder: Folder)

    @Delete
    suspend fun delete(folder: Folder)
}
