package com.example.nammamistri2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nammamistri2.data.MaterialRate
import com.example.nammamistri2.repository.NammaMistriRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RatesViewModel(private val repository: NammaMistriRepository) : ViewModel() {

    val materialRates = repository.getAllMaterialRates()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addMaterialRate(name: String, unit: String, rate: Double) {
        viewModelScope.launch {
            repository.insertMaterialRate(MaterialRate(materialName = name, unit = unit, rate = rate))
        }
    }

    fun updateMaterialRate(rate: MaterialRate) {
        viewModelScope.launch {
            repository.updateMaterialRate(rate)
        }
    }
}