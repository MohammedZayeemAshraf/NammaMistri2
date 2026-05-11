package com.example.nammamistri2.repository

import com.example.nammamistri2.data.*
import com.example.nammamistri2.data.dao.*
import kotlinx.coroutines.flow.Flow

class NammaMistriRepository(
    private val siteDao: SiteDao,
    private val workerDao: WorkerDao,
    private val laborEntryDao: LaborEntryDao,
    private val materialRateDao: MaterialRateDao,
    private val photoDao: PhotoDao
) {
    // Sites
    suspend fun insertSite(site: Site) = siteDao.insert(site)
    suspend fun updateSite(site: Site) = siteDao.update(site)
    fun getAllSites() = siteDao.getAllSites()
    suspend fun getSiteById(id: Long) = siteDao.getSiteById(id)

    // Workers
    suspend fun insertWorker(worker: Worker) = workerDao.insert(worker)
    fun getWorkersBySite(siteId: Long) = workerDao.getWorkersBySite(siteId)
    suspend fun getWorkerById(id: Long) = workerDao.getWorkerById(id)
    suspend fun deleteWorker(workerId: Long) = workerDao.deleteById(workerId)

    // Labor Entries
    suspend fun insertLaborEntry(entry: LaborEntry) = laborEntryDao.insert(entry)
    fun getEntriesByWorker(workerId: Long) = laborEntryDao.getEntriesByWorker(workerId)
    suspend fun getTotalAdvance(workerId: Long) = laborEntryDao.getTotalAdvance(workerId) ?: 0.0
    suspend fun getTotalDaysWorked(workerId: Long) = laborEntryDao.getTotalDaysWorked(workerId)
    fun getTotalAdvanceFlow(workerId: Long) = laborEntryDao.getTotalAdvanceFlow(workerId)
    fun getTotalDaysWorkedFlow(workerId: Long) = laborEntryDao.getTotalDaysWorkedFlow(workerId)

    // Material Rates
    suspend fun insertMaterialRate(rate: MaterialRate) = materialRateDao.insert(rate)
    suspend fun updateMaterialRate(rate: MaterialRate) = materialRateDao.update(rate)
    fun getAllMaterialRates() = materialRateDao.getAllRates()
    suspend fun getMaterialRateById(id: Long) = materialRateDao.getRateById(id)

    // Photos
    suspend fun insertPhoto(photo: Photo) = photoDao.insert(photo)
    fun getPhotosBySite(siteId: Long) = photoDao.getPhotosBySite(siteId)
    suspend fun deletePhoto(photoId: Long) = photoDao.deleteById(photoId)
}