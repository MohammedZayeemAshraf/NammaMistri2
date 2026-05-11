package com.example.nammamistri2.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.nammamistri2.data.MaterialRate
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialRateDao {
    @Insert
    suspend fun insert(rate: MaterialRate): Long

    @Update
    suspend fun update(rate: MaterialRate)

    @Query("SELECT * FROM material_rates")
    fun getAllRates(): Flow<List<MaterialRate>>

    @Query("SELECT * FROM material_rates WHERE id = :id")
    suspend fun getRateById(id: Long): MaterialRate?

    @Delete
    suspend fun delete(rate: MaterialRate)
}