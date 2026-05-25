package com.apexcoretechs.myram.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.apexcoretechs.myram.data.dao.NoteDao

class Repository private constructor(context: Context) {
    private val db = Room.databaseBuilder(
        context,
        AppDatabase::class.java, "myram.db"
    )
        .addMigrations(MIGRATION_2_3)
        .build()

    val noteDao: NoteDao = db.noteDao()

    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Note ADD COLUMN deletedAt INTEGER")
            }
        }

        @Volatile private var INSTANCE: Repository? = null
        fun get(context: Context): Repository =
            INSTANCE ?: synchronized(this) {
                Repository(context).also { INSTANCE = it }
            }
    }
}
