package com.example.nammamistri2.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class Site(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val location: String,
    val progress: Int = 0,
    val createdDate: Long = System.currentTimeMillis()
)