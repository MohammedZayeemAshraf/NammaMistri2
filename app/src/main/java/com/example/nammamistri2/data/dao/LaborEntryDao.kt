package com.example.nammamistri2.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.nammamistri2.data.LaborEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface LaborEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LaborEntry): Long

    @Query("SELECT * FROM labor_entries WHERE workerId = :workerId ORDER BY date DESC")
    fun getEntriesByWorker(workerId: Long): Flow<List<LaborEntry>>

    @Query("SELECT SUM(advance) FROM labor_entries WHERE workerId = :workerId")
    suspend fun getTotalAdvance(workerId: Long): Double?

    @Query("SELECT SUM(attendance) FROM labor_entries WHERE workerId = :workerId")
    suspend fun getTotalDaysWorked(workerId: Long): Double?

    @Query("SELECT COALESCE(SUM(advance), 0.0) FROM labor_entries WHERE workerId = :workerId")
    fun getTotalAdvanceFlow(workerId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(attendance), 0.0) FROM labor_entries WHERE workerId = :workerId")
    fun getTotalDaysWorkedFlow(workerId: Long): Flow<Double>
}
