package com.example.nammamistri2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nammamistri2.data.MaterialRate
import com.example.nammamistri2.repository.NammaMistriRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlin.math.PI
import kotlin.math.pow

enum class LengthUnit(val label: String, val shortLabel: String, val factor: Double) {
    METERS("Meters", "m", 1.0),
    FEET("Feet", "ft", 0.3048),
    INCHES("Inches", "in", 0.0254),
    CENTIMETERS("Centimeters", "cm", 0.01),
    MILLIMETERS("Millimeters", "mm", 0.001)
}

data class MaterialResult(
    val name: String,
    val quantity: Double,
    val unit: String,
    val ratePerUnit: Double? = null
) {
    val totalCost: Double? get() = ratePerUnit?.let { it * quantity }
}

data class StructureResult(
    val structureType: String,
    val netVolume: Double,
    val netArea: Double,
    val materials: List<MaterialResult>
) {
    val grandTotal: Double? get() {
        val costs = materials.mapNotNull { it.totalCost }
        return if (costs.isNotEmpty()) costs.sum() else null
    }
}

class CalculatorViewModel(private val repository: NammaMistriRepository) : ViewModel() {

    val materialRates = repository.getAllMaterialRates()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private fun toM(v: Double, u: LengthUnit) = v * u.factor

    private fun rate(rates: List<MaterialRate>, vararg keywords: String): Double? =
        keywords.firstNotNullOfOrNull { kw ->
            rates.firstOrNull { it.materialName.contains(kw, ignoreCase = true) }?.rate
        }

    // Brick masonry materials (1:6 mortar)
    private fun brickMaterials(volM3: Double, rates: List<MaterialRate>): List<MaterialResult> {
        val bricks    = volM3 * 420.0           // ~420 bricks/m³ with mortar
        val cement    = bricks / 35.0           // 1 bag per 35 bricks
        val sand      = volM3 * 0.35            // 0.35 m³ per m³ brickwork
        return listOf(
            MaterialResult("Bricks",  bricks, "pieces", rate(rates, "brick")),
            MaterialResult("Cement",  cement, "bags",   rate(rates, "cement")),
            MaterialResult("Sand",    sand,   "m³",     rate(rates, "sand"))
        )
    }

    // RCC concrete materials (M20 mix 1:1.5:3)
    private fun concreteMaterials(volM3: Double, steelKgM3: Double, rates: List<MaterialRate>): List<MaterialResult> {
        val dry   = volM3 * 1.54              // 54% bulkage factor
        val parts = 1.0 + 1.5 + 3.0
        val cement = (dry * 1.0 / parts) / 0.035   // 1 bag = 0.035 m³
        val sand   = dry * 1.5 / parts
        val agg    = dry * 3.0 / parts
        val steel  = volM3 * steelKgM3
        return listOf(
            MaterialResult("Cement",         cement, "bags", rate(rates, "cement")),
            MaterialResult("Sand (Fine)",    sand,   "m³",   rate(rates, "sand")),
            MaterialResult("Aggregate",      agg,    "m³",   rate(rates, "aggregate", "gravel", "jelly", "coarse")),
            MaterialResult("Steel / Rebar",  steel,  "kg",   rate(rates, "steel", "rod", "iron", "tmt", "rebar"))
        )
    }

    fun calculateWall(
        length: Double, height: Double, thickness: Double,
        doorW: Double, doorH: Double, doorCount: Int,
        windowW: Double, windowH: Double, windowCount: Int,
        unit: LengthUnit, rates: List<MaterialRate>
    ): StructureResult {
        val l = toM(length, unit); val h = toM(height, unit); val t = toM(thickness, unit)
        val dW = toM(doorW, unit); val dH = toM(doorH, unit)
        val wW = toM(windowW, unit); val wH = toM(windowH, unit)
        val netArea = (l * h - dW * dH * doorCount - wW * wH * windowCount).coerceAtLeast(0.0)
        val vol = netArea * t
        return StructureResult("Wall", vol, netArea, brickMaterials(vol, rates))
    }

    fun calculateRoom(
        length: Double, width: Double, height: Double, thickness: Double,
        doorW: Double, doorH: Double, doorCount: Int,
        windowW: Double, windowH: Double, windowCount: Int,
        unit: LengthUnit, rates: List<MaterialRate>
    ): StructureResult {
        val l = toM(length, unit); val w = toM(width, unit)
        val h = toM(height, unit); val t = toM(thickness, unit)
        val dW = toM(doorW, unit); val dH = toM(doorH, unit)
        val wW = toM(windowW, unit); val wH = toM(windowH, unit)
        val wallArea = (2.0 * (l + w) * h - dW * dH * doorCount - wW * wH * windowCount).coerceAtLeast(0.0)
        val vol = wallArea * t
        return StructureResult("Room", vol, wallArea, brickMaterials(vol, rates))
    }

    fun calculateSlab(
        length: Double, width: Double, thickness: Double,
        unit: LengthUnit, rates: List<MaterialRate>
    ): StructureResult {
        val l = toM(length, unit); val w = toM(width, unit); val t = toM(thickness, unit)
        val area = l * w
        val vol  = area * t
        return StructureResult("Slab", vol, area, concreteMaterials(vol, 80.0, rates))
    }

    fun calculateColumn(
        dim1: Double, dim2: Double, height: Double,
        count: Int, isCircular: Boolean,
        unit: LengthUnit, rates: List<MaterialRate>
    ): StructureResult {
        val d1 = toM(dim1, unit); val d2 = toM(dim2, unit); val h = toM(height, unit)
        val volPerCol = if (isCircular) PI * (d1 / 2.0).pow(2) * h else d1 * d2 * h
        val totalVol  = volPerCol * count
        val area      = if (isCircular) PI * (d1 / 2.0).pow(2) * count else d1 * d2 * count
        return StructureResult("Column", totalVol, area, concreteMaterials(totalVol, 160.0, rates))
    }

    // Legacy kept for compatibility
    fun calculateMaterials(length: Double, width: Double, height: Double, thickness: Double): MaterialCalculation {
        val vol = length * height * thickness
        return MaterialCalculation(vol * 420.0, vol * 420.0 / 35.0, vol * 0.35)
    }
}

data class MaterialCalculation(
    val bricks: Double,
    val cementBags: Double,
    val sandCubicMeters: Double
)