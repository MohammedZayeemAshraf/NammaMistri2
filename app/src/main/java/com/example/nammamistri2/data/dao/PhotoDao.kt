package com.example.nammamistri2.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.nammamistri2.data.Photo
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Insert
    suspend fun insert(photo: Photo): Long

    @Query("SELECT * FROM photos WHERE siteId = :siteId ORDER BY date DESC")
    fun getPhotosBySite(siteId: Long): Flow<List<Photo>>

    @Query("DELETE FROM photos WHERE id = :photoId")
    suspend fun deleteById(photoId: Long)
}