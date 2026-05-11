package com.example.nammamistri2.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.nammamistri2.data.dao.*

@Database(
    entities = [Site::class, Worker::class, LaborEntry::class, MaterialRate::class, Photo::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun siteDao(): SiteDao
    abstract fun workerDao(): WorkerDao
    abstract fun laborEntryDao(): LaborEntryDao
    abstract fun materialRateDao(): MaterialRateDao
    abstract fun photoDao(): PhotoDao
}