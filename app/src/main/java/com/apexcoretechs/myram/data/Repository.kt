package com.apexcoretechs.myram.data

import android.content.Context
import androidx.room.Room
import com.apexcoretechs.myram.data.dao.NoteDao

class Repository private constructor(context: Context) {
    private val db = Room.databaseBuilder(
        context,
        AppDatabase::class.java, "myram.db"
    ).build()

    val noteDao: NoteDao = db.noteDao()

    companion object {
        @Volatile private var INSTANCE: Repository? = null
        fun get(context: Context): Repository =
            INSTANCE ?: synchronized(this) {
                Repository(context).also { INSTANCE = it }
            }
    }
}