package com.apexcoretechs.myram.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)
