package com.northsignalstudio.myram.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.northsignalstudio.myram.data.dao.FolderDao
import com.northsignalstudio.myram.data.dao.NoteDao

class Repository private constructor(context: Context) {
    private val db = Room.databaseBuilder(
        context,
        AppDatabase::class.java, "myram.db"
    )
        .addMigrations(
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10
        )
        .build()

    val noteDao: NoteDao = db.noteDao()
    val folderDao: FolderDao = db.folderDao()

    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Note ADD COLUMN deletedAt INTEGER")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `NotePhotoAttachment` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `noteId` INTEGER NOT NULL,
                        `imageData` BLOB NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`noteId`) REFERENCES `Note`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_NotePhotoAttachment_noteId` ON `NotePhotoAttachment` (`noteId`)"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Note ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    UPDATE Note
                    SET createdAt = CASE
                        WHEN lastModified > 0 THEN lastModified
                        ELSE CAST(strftime('%s','now') AS INTEGER) * 1000
                    END
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `Folder` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `parentFolderId` INTEGER,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `modifiedAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_Folder_parentFolderId` ON `Folder` (`parentFolderId`)"
                )
                db.execSQL("ALTER TABLE Note ADD COLUMN folderId INTEGER")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_Note_folderId` ON `Note` (`folderId`)"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Note ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `PinnedText` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `noteId` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `isCollapsed` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `lastModified` INTEGER NOT NULL,
                        FOREIGN KEY(`noteId`) REFERENCES `Note`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_PinnedText_noteId` ON `PinnedText` (`noteId`)"
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE PinnedText ADD COLUMN sourceContent TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE PinnedText ADD COLUMN sourceStart INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile private var INSTANCE: Repository? = null
        fun get(context: Context): Repository =
            INSTANCE ?: synchronized(this) {
                Repository(context).also { INSTANCE = it }
            }
    }
}
