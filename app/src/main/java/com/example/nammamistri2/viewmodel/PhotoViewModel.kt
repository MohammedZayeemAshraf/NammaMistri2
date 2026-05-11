package com.example.nammamistri2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nammamistri2.data.Photo
import com.example.nammamistri2.repository.NammaMistriRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PhotoViewModel(private val repository: NammaMistriRepository) : ViewModel() {

    private val currentSiteId: Long = 1

    val photos = repository.getPhotosBySite(currentSiteId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addPhoto(uri: String, description: String) {
        viewModelScope.launch {
            repository.insertPhoto(Photo(siteId = currentSiteId, uri = uri, description = description))
        }
    }

    fun deletePhoto(photoId: Long) {
        viewModelScope.launch {
            repository.deletePhoto(photoId)
        }
    }
}