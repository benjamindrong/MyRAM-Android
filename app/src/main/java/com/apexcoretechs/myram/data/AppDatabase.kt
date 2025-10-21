package com.apexcoretechs.myram.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.apexcoretechs.myram.data.dao.*

@Database(entities = [Folder::class, Note::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun noteDao(): NoteDao
}
