package com.example.nammamistri2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nammamistri2.repository.NammaMistriRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class CalculatorViewModel(private val repository: NammaMistriRepository) : ViewModel() {

    val materialRates = repository.getAllMaterialRates()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun calculateMaterials(length: Double, width: Double, height: Double, thickness: Double): MaterialCalculation {
        // Standard formulas for brick wall
        // Volume of wall = length * height * thickness
        val volume = length * height * thickness

        // Bricks: assuming standard brick size 9" x 4.5" x 3" (0.2286m x 0.1143m x 0.0762m)
        // Bricks per cubic meter: 1 / (0.2286 * 0.1143 * 0.0762) ≈ 500 bricks/m³
        // Including mortar, about 400-450 bricks/m³
        val bricksPerCubicMeter = 420.0
        val bricks = volume * bricksPerCubicMeter

        // Cement: 1 bag (50kg) per 35-40 bricks, so for bricks: bricks / 35
        val cementBags = bricks / 35.0

        // Sand: 0.3-0.4 cubic meters per cubic meter of brickwork
        val sandCubicMeters = volume * 0.35

        return MaterialCalculation(bricks, cementBags, sandCubicMeters)
    }
}

data class MaterialCalculation(
    val bricks: Double,
    val cementBags: Double,
    val sandCubicMeters: Double
)