package com.example.nammamistri2.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class Photo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val siteId: Long,
    val uri: String,
    val description: String,
    val date: Long = System.currentTimeMillis()
)