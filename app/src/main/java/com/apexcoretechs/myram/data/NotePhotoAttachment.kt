package com.apexcoretechs.myram.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class NotePhotoAttachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Int,
    val imageData: ByteArray,
    val createdAt: Long = System.currentTimeMillis()
)
