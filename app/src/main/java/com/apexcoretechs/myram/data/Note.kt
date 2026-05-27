package com.apexcoretechs.myram.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
