package com.example.nammamistri2.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.nammamistri2.data.Worker
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Insert
    suspend fun insert(worker: Worker): Long

    @Query("SELECT * FROM workers WHERE siteId = :siteId")
    fun getWorkersBySite(siteId: Long): Flow<List<Worker>>

    @Query("SELECT * FROM workers WHERE id = :id")
    suspend fun getWorkerById(id: Long): Worker?

    @Query("DELETE FROM workers WHERE id = :workerId")
    suspend fun deleteById(workerId: Long)
}