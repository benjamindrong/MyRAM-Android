package com.apexcoretechs.myram.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.apexcoretechs.myram.data.dao.NoteDao

@Database(entities = [Note::class, NotePhotoAttachment::class], version = 5)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
