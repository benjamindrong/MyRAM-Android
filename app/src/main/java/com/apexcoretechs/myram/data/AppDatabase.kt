package com.apexcoretechs.myram.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.apexcoretechs.myram.data.dao.FolderDao
import com.apexcoretechs.myram.data.dao.NoteDao

@Database(entities = [Note::class, NotePhotoAttachment::class, Folder::class], version = 6)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
}
