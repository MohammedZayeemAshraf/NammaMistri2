package com.example.nammamistri2.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "labor_entries")
data class LaborEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workerId: Long,
    val date: Long,
    val attendance: Double = 1.0, // 1.0 = Full Day, 0.5 = Half Day, 0.0 = Absent
    val advance: Double = 0.0,
    val paymentMode: String? = null
)
