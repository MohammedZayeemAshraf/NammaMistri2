package com.example.nammamistri2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nammamistri2.data.MaterialRate
import com.example.nammamistri2.repository.NammaMistriRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.math.PI
import kotlin.math.pow

// ─────────────────────────────────────────────────────────────────
// UNITS
// ─────────────────────────────────────────────────────────────────
enum class LengthUnit(val label: String, val shortLabel: String, val factor: Double) {
    FEET("Feet", "ft", 0.3048),
    METERS("Meters", "m", 1.0),
    INCHES("Inches", "in", 0.0254),
    CENTIMETERS("cm", "cm", 0.01),
    MILLIMETERS("mm", "mm", 0.001)
}

// ─────────────────────────────────────────────────────────────────
// BRICK TYPE  (sizes in inches: L×W×H)
// ─────────────────────────────────────────────────────────────────
enum class BrickType(
    val label: String,
    val description: String,
    val sizeLabel: String,            // display
    val lengthIn: Double, val widthIn: Double, val heightIn: Double,
    val emoji: String,
    val mortarFraction: Double,       // wet mortar vol fraction per m³ brickwork
    val unitName: String              // display name for the unit (Bricks / Blocks)
) {
    RED_BRICK(
        label = "Red Brick",
        description = "Traditional fired clay brick, excellent strength & thermal mass",
        sizeLabel = "9 × 4.5 × 3 inch",
        lengthIn = 9.0, widthIn = 4.5, heightIn = 3.0,
        emoji = "🧱",
        mortarFraction = 0.25,
        unitName = "Bricks"
    ),
    FLY_ASH_BRICK(
        label = "Fly Ash Brick",
        description = "Eco-friendly, lighter & stronger than red brick, less water absorption",
        sizeLabel = "9 × 4 × 3 inch",
        lengthIn = 9.0, widthIn = 4.0, heightIn = 3.0,
        emoji = "🪨",
        mortarFraction = 0.22,
        unitName = "Bricks"
    ),
    CONCRETE_BLOCK(
        label = "Concrete Block",
        description = "Heavy solid block, ideal for load-bearing walls & basements",
        sizeLabel = "16 × 8 × 8 inch",
        lengthIn = 16.0, widthIn = 8.0, heightIn = 8.0,
        emoji = "◼",
        mortarFraction = 0.12,
        unitName = "Blocks"
    ),
    AAC_BLOCK(
        label = "AAC Block",
        description = "Lightweight, excellent insulation, fast construction, less cement",
        sizeLabel = "24 × 8 × 6 inch",
        lengthIn = 24.0, widthIn = 8.0, heightIn = 6.0,
        emoji = "🪶",
        mortarFraction = 0.07,
        unitName = "AAC Blocks"
    ),
    HOLLOW_BLOCK(
        label = "Hollow Block",
        description = "Reduces dead load, good thermal & sound insulation, faster laying",
        sizeLabel = "16 × 8 × 8 inch",
        lengthIn = 16.0, widthIn = 8.0, heightIn = 8.0,
        emoji = "⬜",
        mortarFraction = 0.15,
        unitName = "Blocks"
    );

    // units per m³ based on actual dimension + 10mm joint
    val unitsPerM3: Double get() {
        val lM = (lengthIn * 0.0254) + 0.01
        val wM = (widthIn * 0.0254) + 0.01
        val hM = (heightIn * 0.0254) + 0.01
        return 1.0 / (lM * wM * hM)
    }
}

// ─────────────────────────────────────────────────────────────────
// WALL TYPE  (legacy for WallTab)
// ─────────────────────────────────────────────────────────────────
enum class WallType(
    val label: String,
    val unitsPerM3: Double,
    val unitName: String,
    val mortarFraction: Double
) {
    BRICK_WALL("Red Brick Wall (9×4.5×3\")", 494.0, "Bricks", 0.25),
    FLY_ASH_WALL("Fly Ash Brick (9×4×3\")", 520.0, "Bricks", 0.22),
    CONCRETE_BLOCK("Concrete Block (16×8×8\")", 62.5, "Blocks", 0.12),
    AAC_BLOCK("AAC Block (24×8×6\")", 41.7, "AAC Blocks", 0.07),
    HOLLOW_BLOCK("Hollow Block (16×8×8\")", 62.5, "Blocks", 0.15),
    STONE_WALL("Stone Wall (Rubble)", 0.0, "Stone", 0.35)
}

// ─────────────────────────────────────────────────────────────────
// MORTAR RATIO
// ─────────────────────────────────────────────────────────────────
enum class MortarRatio(val label: String, val cementParts: Double, val sandParts: Double) {
    RATIO_1_3("1:3  – Rich (plaster grade)", 1.0, 3.0),
    RATIO_1_4("1:4  – Strong masonry",        1.0, 4.0),
    RATIO_1_5("1:5  – Standard IS 2116",      1.0, 5.0),
    RATIO_1_6("1:6  – Normal brickwork",      1.0, 6.0),
    RATIO_1_8("1:8  – Lean / economy",        1.0, 8.0)
}

// ─────────────────────────────────────────────────────────────────
// CONCRETE GRADE & MIX
// ─────────────────────────────────────────────────────────────────
enum class ConcreteGrade(
    val label: String,
    val mixLabel: String,
    val cParts: Double, val sParts: Double, val aParts: Double
) {
    M20("M20", "1:1.5:3",  1.0, 1.5, 3.0),
    M25("M25", "1:1:2",    1.0, 1.0, 2.0),
    M30("M30", "1:1:1.5",  1.0, 1.0, 1.5)
}

// ─────────────────────────────────────────────────────────────────
// SLAB TYPE
// ─────────────────────────────────────────────────────────────────
enum class SlabType(val label: String, val steelKgPerM3: Double) {
    ROOF_SLAB(     "Roof Slab",      80.0),
    FLOOR_SLAB(    "Floor Slab",     70.0),
    STAIRCASE_SLAB("Staircase Slab", 120.0)
}

// ─────────────────────────────────────────────────────────────────
// ROOM TYPE
// ─────────────────────────────────────────────────────────────────
enum class RoomType(val label: String, val icon: String) {
    BEDROOM ("Bedroom",  "🛏"),
    HALL    ("Hall",     "🛋"),
    KITCHEN ("Kitchen",  "🍳"),
    BATHROOM("Bathroom", "🚿"),
    CUSTOM  ("Custom",   "📐")
}

// ─────────────────────────────────────────────────────────────────
// FLOORING TYPE  (tiles per m² for 60×60cm standard tile)
// ─────────────────────────────────────────────────────────────────
enum class FlooringType(val label: String, val tilesPerM2: Double) {
    VITRIFIED ("Vitrified Tile (60×60cm)", 2.78),
    CERAMIC   ("Ceramic Tile (30×30cm)",   11.11),
    MARBLE    ("Marble Slab (60×120cm)",   1.39),
    GRANITE   ("Granite Slab (60×60cm)",   2.78),
    NONE      ("No Flooring",             0.0)
}

// ─────────────────────────────────────────────────────────────────
// RESULT DATA CLASSES
// ─────────────────────────────────────────────────────────────────
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
    val materials: List<MaterialResult>,
    val wastagePercent: Int = 5,
    val label: String = "",
    // Extended fields for Slab / Room detailed result
    val extraStats: Map<String, String> = emptyMap()
) {
    val grandTotal: Double? get() {
        val costs = materials.mapNotNull { it.totalCost }
        return if (costs.isNotEmpty()) costs.sum() else null
    }
}

data class SlabResult(
    val slabType: SlabType,
    val length: Double, val width: Double, val thickness: Double, // in m
    val grade: ConcreteGrade,
    val steelPercent: Double,
    val wastagePercent: Int,
    val area: Double,
    val volume: Double,
    // quantities (with wastage)
    val cementBags: Double,
    val sandM3: Double,
    val aggM3: Double,
    val steelKg: Double,
    val waterLitres: Double,
    // costs
    val cementCost: Double?,
    val sandCost: Double?,
    val aggCost: Double?,
    val steelCost: Double?,
    val totalCost: Double?
)

data class RoomResult(
    val roomType: RoomType,
    val length: Double, val width: Double, val height: Double, val thickness: Double, // in m
    val brickType: BrickType,
    val mortarRatio: MortarRatio,
    val flooringType: FlooringType,
    val wastagePercent: Int,
    // areas
    val wallArea: Double,
    val floorArea: Double,
    val ceilingArea: Double,
    val paintArea: Double,
    // volumes
    val wallVolume: Double,
    // masonry
    val brickCount: Double,
    val cementBags: Double,
    val sandM3: Double,
    val waterLitres: Double,
    // flooring
    val flooringTiles: Double,
    // costs
    val brickCost: Double?,
    val cementCost: Double?,
    val sandCost: Double?,
    val flooringCost: Double?,
    val paintCost: Double?,
    val laborCost: Double?,
    val totalCost: Double?
)

// History entry
data class CalcHistoryEntry(
    val id: Long = System.currentTimeMillis(),
    val result: StructureResult
)

// ─────────────────────────────────────────────────────────────────
// VIEWMODEL
// ─────────────────────────────────────────────────────────────────
class CalculatorViewModel(private val repository: NammaMistriRepository) : ViewModel() {

    val materialRates = repository.getAllMaterialRates()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _history = MutableStateFlow<List<CalcHistoryEntry>>(emptyList())
    val history: StateFlow<List<CalcHistoryEntry>> = _history.asStateFlow()

    fun addToHistory(result: StructureResult) {
        _history.update { listOf(CalcHistoryEntry(result = result)) + it }
    }
    fun deleteHistory(id: Long) { _history.update { it.filter { e -> e.id != id } } }
    fun clearHistory() { _history.value = emptyList() }

    // ── helpers ──────────────────────────────────────────────────
    private fun toM(v: Double, u: LengthUnit) = v * u.factor

    private fun rate(rates: List<MaterialRate>, vararg keywords: String): Double? =
        keywords.firstNotNullOfOrNull { kw ->
            rates.firstOrNull { it.materialName.contains(kw, ignoreCase = true) }?.rate
        }

    // ── Masonry (generic: wall type + mortar ratio + wastage) ────
    private fun masonryMaterials(
        volM3: Double, wallType: WallType, mortarRatio: MortarRatio,
        wastagePercent: Int, rates: List<MaterialRate>
    ): List<MaterialResult> {
        val wf = 1.0 + wastagePercent / 100.0
        val totalParts = mortarRatio.cementParts + mortarRatio.sandParts
        val dryMortarVol = volM3 * wallType.mortarFraction * 1.30
        val cementBags = (dryMortarVol * mortarRatio.cementParts / totalParts / 0.035) * wf
        val sandM3 = (dryMortarVol * mortarRatio.sandParts / totalParts) * wf
        return buildList {
            if (wallType.unitsPerM3 > 0) {
                val rk = when (wallType) {
                    WallType.AAC_BLOCK     -> arrayOf("aac", "block")
                    WallType.CONCRETE_BLOCK, WallType.HOLLOW_BLOCK -> arrayOf("block", "concrete block")
                    WallType.FLY_ASH_WALL  -> arrayOf("fly ash", "flyash", "brick")
                    else                   -> arrayOf("brick")
                }
                add(MaterialResult(wallType.unitName, volM3 * wallType.unitsPerM3 * wf, "pieces",
                    rate(rates, *rk)))
            } else {
                add(MaterialResult("Stone (Rubble)", volM3 * (1.0 - wallType.mortarFraction) * wf,
                    "m³", rate(rates, "stone")))
            }
            add(MaterialResult("Cement", cementBags, "bags", rate(rates, "cement")))
            add(MaterialResult("Sand", sandM3, "m³", rate(rates, "sand")))
        }
    }

    // ── Masonry using BrickType (for Room/Wall with picker) ──────
    private fun masonryWithBrick(
        volM3: Double, brickType: BrickType, mortarRatio: MortarRatio,
        wastagePercent: Int, rates: List<MaterialRate>
    ): Triple<Double, Double, Double> { // brickCount, cementBags, sandM3
        val wf = 1.0 + wastagePercent / 100.0
        val totalParts = mortarRatio.cementParts + mortarRatio.sandParts
        val dryMortarVol = volM3 * brickType.mortarFraction * 1.30
        val bricks = volM3 * brickType.unitsPerM3 * wf
        val cementBags = (dryMortarVol * mortarRatio.cementParts / totalParts / 0.035) * wf
        val sandM3 = (dryMortarVol * mortarRatio.sandParts / totalParts) * wf
        return Triple(bricks, cementBags, sandM3)
    }

    // ── RCC concrete ─────────────────────────────────────────────
    private fun concreteMaterials(
        volM3: Double, steelKgM3: Double, wastagePercent: Int, rates: List<MaterialRate>
    ): List<MaterialResult> {
        val wf = 1.0 + wastagePercent / 100.0
        val dry = volM3 * 1.54
        val totalParts = 5.5 // 1+1.5+3 for M20
        val cement = ((dry / totalParts) / 0.035) * wf
        val sand = (dry * 1.5 / totalParts) * wf
        val agg = (dry * 3.0 / totalParts) * wf
        val steel = volM3 * steelKgM3 * wf
        return listOf(
            MaterialResult("Cement", cement, "bags", rate(rates, "cement")),
            MaterialResult("Sand (Fine)", sand, "m³", rate(rates, "sand")),
            MaterialResult("Aggregate (CA)", agg, "m³", rate(rates, "aggregate", "gravel", "jelly", "coarse")),
            MaterialResult("Steel / Rebar", steel, "kg", rate(rates, "steel", "rod", "iron", "tmt", "rebar"))
        )
    }

    // ─────────────────────────────────────────────────────────────
    // WALL CALCULATE
    // ─────────────────────────────────────────────────────────────
    fun calculateWall(
        length: Double, height: Double, thickness: Double,
        doorW: Double, doorH: Double, doorCount: Int,
        windowW: Double, windowH: Double, windowCount: Int,
        unit: LengthUnit, rates: List<MaterialRate>,
        wallType: WallType = WallType.BRICK_WALL,
        mortarRatio: MortarRatio = MortarRatio.RATIO_1_6,
        wastagePercent: Int = 5
    ): StructureResult {
        val l = toM(length, unit); val h = toM(height, unit); val t = toM(thickness, unit)
        val dW = toM(doorW, unit); val dH = toM(doorH, unit)
        val wW = toM(windowW, unit); val wH = toM(windowH, unit)
        val netArea = (l * h - dW * dH * doorCount - wW * wH * windowCount).coerceAtLeast(0.0)
        val vol = netArea * t
        val lbl = "Wall ${String.format("%.1f", length)}×${String.format("%.1f", height)} ${unit.shortLabel}"
        return StructureResult("Wall", vol, netArea,
            masonryMaterials(vol, wallType, mortarRatio, wastagePercent, rates),
            wastagePercent, lbl)
    }

    // ─────────────────────────────────────────────────────────────
    // SLAB CALCULATE
    // ─────────────────────────────────────────────────────────────
    fun calculateSlab(
        length: Double, width: Double, thickness: Double,
        unit: LengthUnit, rates: List<MaterialRate>,
        slabType: SlabType = SlabType.ROOF_SLAB,
        grade: ConcreteGrade = ConcreteGrade.M20,
        steelPercent: Double = 1.0,          // % of concrete volume
        wastagePercent: Int = 5
    ): SlabResult {
        val l = toM(length, unit); val w = toM(width, unit); val t = toM(thickness, unit)
        val area = l * w; val vol = area * t
        val wf = 1.0 + wastagePercent / 100.0

        // Concrete M20 dry factor 1.54; grade adjusts total parts
        val dry = vol * 1.54
        val tp = grade.cParts + grade.sParts + grade.aParts
        val cement = ((dry * grade.cParts / tp) / 0.035) * wf
        val sand   = (dry * grade.sParts / tp) * wf
        val agg    = (dry * grade.aParts / tp) * wf

        // Steel: steelPercent% of concrete volume, density 7850 kg/m³
        val steelKg = vol * (steelPercent / 100.0) * 7850.0 * wf

        // Water: W/C ratio ~0.5 for M20 → water = cement bags × 50kg × 0.5 litre/kg
        val waterL = cement * 50.0 * 0.5

        val cR = rate(rates, "cement")
        val sR = rate(rates, "sand")
        val aR = rate(rates, "aggregate", "gravel", "jelly", "coarse")
        val stR = rate(rates, "steel", "rod", "iron", "tmt", "rebar")

        val cCost  = cR?.let { it * cement }
        val sCost  = sR?.let { it * sand }
        val aCost  = aR?.let { it * agg }
        val stCost = stR?.let { it * steelKg }
        val total  = listOfNotNull(cCost, sCost, aCost, stCost).takeIf { it.isNotEmpty() }?.sum()

        return SlabResult(slabType, l, w, t, grade, steelPercent, wastagePercent,
            area, vol, cement, sand, agg, steelKg, waterL,
            cCost, sCost, aCost, stCost, total)
    }

    // ─────────────────────────────────────────────────────────────
    // ROOM CALCULATE
    // ─────────────────────────────────────────────────────────────
    fun calculateRoom(
        length: Double, width: Double, height: Double, thickness: Double,
        doorW: Double, doorH: Double, doorCount: Int,
        windowW: Double, windowH: Double, windowCount: Int,
        unit: LengthUnit, rates: List<MaterialRate>,
        roomType: RoomType = RoomType.BEDROOM,
        brickType: BrickType = BrickType.RED_BRICK,
        mortarRatio: MortarRatio = MortarRatio.RATIO_1_6,
        flooringType: FlooringType = FlooringType.VITRIFIED,
        wastagePercent: Int = 5
    ): RoomResult {
        val l = toM(length, unit); val w = toM(width, unit)
        val h = toM(height, unit); val t = toM(thickness, unit)
        val dW = toM(doorW, unit); val dH = toM(doorH, unit)
        val wW = toM(windowW, unit); val wH = toM(windowH, unit)

        val totalWallArea = 2.0 * (l + w) * h
        val openings = dW * dH * doorCount + wW * wH * windowCount
        val netWallArea = (totalWallArea - openings).coerceAtLeast(0.0)
        val floorArea = l * w
        val ceilingArea = l * w
        val paintArea = netWallArea + ceilingArea  // walls + ceiling, not floor
        val wallVol = netWallArea * t

        val (bricks, cementBags, sandM3) =
            masonryWithBrick(wallVol, brickType, mortarRatio, wastagePercent, rates)

        // Water: ~20 litres per 50 bags (rough estimate)
        val waterL = cementBags * 0.4

        // Flooring tiles
        val wfTile = 1.0 + wastagePercent / 100.0
        val tiles = if (flooringType != FlooringType.NONE)
            floorArea * flooringType.tilesPerM2 * wfTile else 0.0

        // Costs
        val bR  = rate(rates, "brick", "fly ash", "aac", "block")
        val cR  = rate(rates, "cement")
        val sR  = rate(rates, "sand")
        val fR  = rate(rates, "tile", "flooring", "marble", "granite", "ceramic")
        val pR  = rate(rates, "paint", "distemper")
        // Labor: rough estimate ₹50/m² wall area
        val laborCost = netWallArea * 50.0

        val bCost  = bR?.let { it * bricks }
        val cCost  = cR?.let { it * cementBags }
        val sCost  = sR?.let { it * sandM3 }
        val fCost  = if (flooringType != FlooringType.NONE) fR?.let { it * tiles } else null
        val pCost  = pR?.let { it * paintArea }

        val costItems = listOfNotNull(bCost, cCost, sCost, fCost, pCost, laborCost)
        val total = if (costItems.isNotEmpty()) costItems.sum() else null

        return RoomResult(
            roomType, l, w, h, t, brickType, mortarRatio, flooringType, wastagePercent,
            netWallArea, floorArea, ceilingArea, paintArea, wallVol,
            bricks, cementBags, sandM3, waterL, tiles,
            bCost, cCost, sCost, fCost, pCost, laborCost, total
        )
    }

    // ─────────────────────────────────────────────────────────────
    // COLUMN CALCULATE
    // ─────────────────────────────────────────────────────────────
    fun calculateColumn(
        dim1: Double, dim2: Double, height: Double,
        count: Int, isCircular: Boolean,
        unit: LengthUnit, rates: List<MaterialRate>,
        wastagePercent: Int = 5
    ): StructureResult {
        val d1 = toM(dim1, unit); val d2 = toM(dim2, unit); val h = toM(height, unit)
        val volPerCol = if (isCircular) PI * (d1 / 2.0).pow(2) * h else d1 * d2 * h
        val totalVol = volPerCol * count
        val area = if (isCircular) PI * (d1 / 2.0).pow(2) * count else d1 * d2 * count
        val lbl = "Column ×$count  H=${String.format("%.1f", height)} ${unit.shortLabel}"
        return StructureResult("Column", totalVol, area,
            concreteMaterials(totalVol, 160.0, wastagePercent, rates), wastagePercent, lbl)
    }

    // Legacy
    fun calculateMaterials(length: Double, width: Double, height: Double, thickness: Double): MaterialCalculation {
        val vol = length * height * thickness
        return MaterialCalculation(vol * 500.0, (vol * 0.3066 / 7.0) / 0.035, vol * 0.3066 * 6.0 / 7.0)
    }
}

data class MaterialCalculation(val bricks: Double, val cementBags: Double, val sandCubicMeters: Double)

