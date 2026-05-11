package com.example.nammamistri2.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "material_rates")
data class MaterialRate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val materialName: String,
    val unit: String, // e.g., "bag", "brick", "load"
    val rate: Double
)