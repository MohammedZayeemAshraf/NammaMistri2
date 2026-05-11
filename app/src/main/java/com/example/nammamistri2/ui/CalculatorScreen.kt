package com.example.nammamistri2.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nammamistri2.data.MaterialRate
import com.example.nammamistri2.viewmodel.CalculatorViewModel
import com.example.nammamistri2.viewmodel.LengthUnit
import com.example.nammamistri2.viewmodel.MaterialResult
import com.example.nammamistri2.viewmodel.StructureResult

// ─────────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel) {
    val rates by viewModel.materialRates.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedUnit by remember { mutableStateOf(LengthUnit.FEET) }
    val tabs = listOf("Wall", "Room", "Slab", "Column")

    Column(modifier = Modifier.fillMaxSize()) {
        // Unit selector
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Select Unit", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LengthUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = selectedUnit == unit,
                            onClick = { selectedUnit = unit },
                            label = {
                                Text(unit.shortLabel, fontSize = 12.sp,
                                    fontWeight = if (selectedUnit == unit) FontWeight.Bold else FontWeight.Normal)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Tab row
        TabRow(selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) })
            }
        }

        when (selectedTab) {
            0 -> WallTab(viewModel, rates, selectedUnit)
            1 -> RoomTab(viewModel, rates, selectedUnit)
            2 -> SlabTab(viewModel, rates, selectedUnit)
            3 -> ColumnTab(viewModel, rates, selectedUnit)
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// ILLUSTRATIONS (Canvas-based)
// ─────────────────────────────────────────────────────────────────

@Composable
fun WallIllustration() {
    val brickColor = Color(0xFFBF5700)
    val mortarColor = Color(0xFFF5EED8)
    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFF3E0))) {
        val bW = size.width / 7f; val bH = size.height / 6f; val gap = 4f
        for (row in 0..6) {
            val offset = if (row % 2 == 0) 0f else bW / 2f
            var x = -offset
            while (x < size.width + bW) {
                drawRoundRect(brickColor, topLeft = Offset(x + gap / 2, row * bH + gap / 2), size = Size(bW - gap, bH - gap), cornerRadius = CornerRadius(4f))
                x += bW
            }
        }
        val dW = bW * 1.5f; val dH = bH * 3f; val dX = size.width / 2f - dW / 2f
        drawRect(mortarColor, topLeft = Offset(dX, size.height - dH), size = Size(dW, dH))
        val wW = bW * 1.2f; val wH = bH * 1.5f
        drawRect(mortarColor, topLeft = Offset(size.width * 0.18f, size.height * 0.25f), size = Size(wW, wH))
        drawRect(Color(0xFF90CAF9).copy(alpha = 0.6f), topLeft = Offset(size.width * 0.18f, size.height * 0.25f), size = Size(wW, wH))
    }
}

@Composable
fun RoomIllustration() {
    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0D47A1))) {
        val m = 30f; val wT = 14f
        val gridColor = Color.White.copy(alpha = 0.08f)
        var gx = 0f; while (gx < size.width) { drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height)); gx += 20f }
        var gy = 0f; while (gy < size.height) { drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy)); gy += 20f }
        val wc = Color.White
        drawLine(wc, Offset(m, size.height - m), Offset(size.width - m, size.height - m), wT)
        drawLine(wc, Offset(m, m), Offset(size.width - m, m), wT)
        drawLine(wc, Offset(m, m), Offset(m, size.height - m), wT)
        val dGap = 50f; val dMid = size.height * 0.7f
        drawLine(wc, Offset(size.width - m, m), Offset(size.width - m, dMid - dGap / 2), wT)
        drawLine(wc, Offset(size.width - m, dMid + dGap / 2), Offset(size.width - m, size.height - m), wT)
        drawArc(Color.White.copy(alpha = 0.5f), 180f, 90f, false, Offset(size.width - m - dGap, dMid - dGap / 2), Size(dGap, dGap), style = Stroke(2f))
        listOf(0.3f, 0.6f).forEach { xf -> drawLine(Color(0xFF42A5F5), Offset(size.width * xf - 20f, m), Offset(size.width * xf + 20f, m), wT + 4f) }
        drawRect(Color.White.copy(alpha = 0.05f), topLeft = Offset(m + wT, m + wT), size = Size(size.width - 2 * (m + wT), size.height - 2 * (m + wT)))
        drawLine(Color(0xFFFFEB3B), Offset(m + wT, size.height - 8f), Offset(size.width - m - wT, size.height - 8f), 2f)
        drawLine(Color(0xFFFFEB3B), Offset(8f, m + wT), Offset(8f, size.height - m - wT), 2f)
    }
}

@Composable
fun SlabIllustration() {
    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF1F8E9))) {
        val cx = size.width / 2f; val slabW = size.width * 0.75f
        val slabH = size.height * 0.38f; val slabT = 36f; val offX = size.width * 0.12f
        val topPath = Path().apply {
            moveTo(cx - slabW / 2 + offX, slabH - slabT / 2); lineTo(cx + slabW / 2 + offX, slabH - slabT / 2)
            lineTo(cx + slabW / 2, slabH + slabT / 2); lineTo(cx - slabW / 2, slabH + slabT / 2); close()
        }
        drawPath(topPath, Color(0xFFBDBDBD))
        drawRect(Color(0xFF9E9E9E), topLeft = Offset(cx - slabW / 2, slabH + slabT / 2), size = Size(slabW, slabT))
        drawRect(Color(0xFF757575), topLeft = Offset(cx - slabW / 2, slabH + slabT * 1.5f), size = Size(slabW, 6f))
        val rY = slabH + slabT * 1.1f; val step = slabW / 6f
        for (i in 0..5) drawLine(Color(0xFFBF5700), Offset(cx - slabW / 2 + step * i, rY - 8f), Offset(cx - slabW / 2 + step * i, rY + 8f), 3f)
        drawLine(Color(0xFFBF5700), Offset(cx - slabW / 2 + 4f, rY), Offset(cx + slabW / 2 - 4f, rY), 3f)
        listOf(cx - slabW / 2 + 8f, cx + slabW / 2 - 8f).forEach { px ->
            drawRect(Color(0xFF78909C), topLeft = Offset(px - 8f, slabH + slabT * 1.5f), size = Size(16f, size.height - slabH - slabT * 1.5f - 8f))
        }
    }
}

@Composable
fun ColumnIllustration() {
    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF3E5F5))) {
        val cx = size.width / 2f; val colW = 60f; val baseW = 100f
        val baseH = 16f; val capH = 16f
        val colH = size.height - baseH * 2 - capH * 2 - 16f
        val colTop = (size.height - colH - baseH - capH) / 2f
        val cc = Color(0xFF90A4AE); val rc = Color(0xFFBF5700)
        drawRoundRect(cc, topLeft = Offset(cx - baseW / 2, colTop - capH), size = Size(baseW, capH), cornerRadius = CornerRadius(6f))
        drawRoundRect(cc, topLeft = Offset(cx - colW / 2, colTop), size = Size(colW, colH), cornerRadius = CornerRadius(4f))
        drawRoundRect(cc, topLeft = Offset(cx - baseW / 2, colTop + colH), size = Size(baseW, baseH), cornerRadius = CornerRadius(6f))
        val sStep = colH / 6f
        for (i in 1..5) {
            val sy = colTop + sStep * i
            drawRect(rc.copy(alpha = 0.9f), topLeft = Offset(cx - colW / 2 + 6f, sy - 6f), size = Size(colW - 12f, 12f), style = Stroke(2.5f))
        }
        listOf(Offset(cx - colW / 2 + 10f, colTop + 8f), Offset(cx + colW / 2 - 10f, colTop + 8f),
            Offset(cx - colW / 2 + 10f, colTop + colH - 8f), Offset(cx + colW / 2 - 10f, colTop + colH - 8f)
        ).forEach { drawCircle(rc, 5f, it) }
    }
}

// ─────────────────────────────────────────────────────────────────
// SHARED INPUT HELPERS
// ─────────────────────────────────────────────────────────────────

@Composable
fun CalcField(label: String, value: String, onValueChange: (String) -> Unit, unitLabel: String, modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text("$label ($unitLabel)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
        modifier = modifier, shape = RoundedCornerShape(12.dp))
}

@Composable
fun CountField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
        modifier = modifier, shape = RoundedCornerShape(12.dp))
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
fun OpeningsCard(
    doorW: String, onDoorW: (String) -> Unit, doorH: String, onDoorH: (String) -> Unit,
    doorCount: String, onDoorCount: (String) -> Unit,
    winW: String, onWinW: (String) -> Unit, winH: String, onWinH: (String) -> Unit,
    winCount: String, onWinCount: (String) -> Unit, unit: LengthUnit
) {
    SectionCard("Deductions – Doors & Windows") {
        Text("Doors", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalcField("Width", doorW, onDoorW, unit.shortLabel, Modifier.weight(1f))
            CalcField("Height", doorH, onDoorH, unit.shortLabel, Modifier.weight(1f))
            CountField("Count", doorCount, onDoorCount, Modifier.weight(0.7f))
        }
        Text("Windows", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalcField("Width", winW, onWinW, unit.shortLabel, Modifier.weight(1f))
            CalcField("Height", winH, onWinH, unit.shortLabel, Modifier.weight(1f))
            CountField("Count", winCount, onWinCount, Modifier.weight(0.7f))
        }
    }
}

@Composable
fun CalcButton(onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
        Icon(Icons.Default.Calculate, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text("Calculate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────────────────────────
// RESULTS
// ─────────────────────────────────────────────────────────────────

@Composable
fun ResultsSection(result: StructureResult) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Summary header
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Net Volume", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary.copy(0.8f))
                    Text(String.format("%.3f m³", result.netVolume), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onPrimary)
                }
                Column {
                    Text("Net Area", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary.copy(0.8f))
                    Text(String.format("%.2f m²", result.netArea), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onPrimary)
                }
                result.grandTotal?.let {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Est. Cost", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary.copy(0.8f))
                        Text("₹${String.format("%,.0f", it)}", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
        Text("Materials Required", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        result.materials.forEach { MaterialResultRow(it) }
    }
}

@Composable
fun MaterialResultRow(mat: MaterialResult) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(mat.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (mat.ratePerUnit != null)
                    Text("₹${mat.ratePerUnit.toInt()} / ${mat.unit}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                else
                    Text("Add rate in Standard Rates for cost", fontSize = 11.sp, color = MaterialTheme.colorScheme.error.copy(0.7f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    when { mat.quantity >= 1000 -> String.format("%.0f", mat.quantity)
                        mat.quantity >= 10 -> String.format("%.1f", mat.quantity)
                        else -> String.format("%.2f", mat.quantity) },
                    fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Text(mat.unit, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                mat.totalCost?.let { Text("₹${String.format("%,.0f", it)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// WALL TAB
// ─────────────────────────────────────────────────────────────────

@Composable
fun WallTab(viewModel: CalculatorViewModel, rates: List<MaterialRate>, unit: LengthUnit) {
    var length by remember { mutableStateOf("") }; var height by remember { mutableStateOf("") }
    var thickness by remember { mutableStateOf("") }
    var doorW by remember { mutableStateOf("3") }; var doorH by remember { mutableStateOf("7") }; var doorCount by remember { mutableStateOf("1") }
    var winW by remember { mutableStateOf("4") }; var winH by remember { mutableStateOf("4") }; var winCount by remember { mutableStateOf("2") }
    var result by remember { mutableStateOf<StructureResult?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { WallIllustration() }
        item {
            SectionCard("Wall Dimensions") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalcField("Length", length, { length = it }, unit.shortLabel, Modifier.weight(1f))
                    CalcField("Height", height, { height = it }, unit.shortLabel, Modifier.weight(1f))
                }
                CalcField("Thickness", thickness, { thickness = it }, unit.shortLabel, Modifier.fillMaxWidth())
            }
        }
        item { OpeningsCard(doorW,{doorW=it},doorH,{doorH=it},doorCount,{doorCount=it},winW,{winW=it},winH,{winH=it},winCount,{winCount=it},unit) }
        item {
            CalcButton {
                result = viewModel.calculateWall(
                    length.toDoubleOrNull()?:0.0, height.toDoubleOrNull()?:0.0, thickness.toDoubleOrNull()?:0.0,
                    doorW.toDoubleOrNull()?:0.0, doorH.toDoubleOrNull()?:0.0, doorCount.toIntOrNull()?:0,
                    winW.toDoubleOrNull()?:0.0, winH.toDoubleOrNull()?:0.0, winCount.toIntOrNull()?:0, unit, rates)
            }
        }
        result?.let { item { ResultsSection(it) } }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────
// ROOM TAB
// ─────────────────────────────────────────────────────────────────

@Composable
fun RoomTab(viewModel: CalculatorViewModel, rates: List<MaterialRate>, unit: LengthUnit) {
    var length by remember { mutableStateOf("") }; var width by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }; var thickness by remember { mutableStateOf("") }
    var doorW by remember { mutableStateOf("3") }; var doorH by remember { mutableStateOf("7") }; var doorCount by remember { mutableStateOf("1") }
    var winW by remember { mutableStateOf("4") }; var winH by remember { mutableStateOf("4") }; var winCount by remember { mutableStateOf("2") }
    var result by remember { mutableStateOf<StructureResult?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { RoomIllustration() }
        item {
            SectionCard("Room Dimensions") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalcField("Length", length, { length = it }, unit.shortLabel, Modifier.weight(1f))
                    CalcField("Width", width, { width = it }, unit.shortLabel, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalcField("Height", height, { height = it }, unit.shortLabel, Modifier.weight(1f))
                    CalcField("Wall Thickness", thickness, { thickness = it }, unit.shortLabel, Modifier.weight(1f))
                }
            }
        }
        item { OpeningsCard(doorW,{doorW=it},doorH,{doorH=it},doorCount,{doorCount=it},winW,{winW=it},winH,{winH=it},winCount,{winCount=it},unit) }
        item {
            CalcButton {
                result = viewModel.calculateRoom(
                    length.toDoubleOrNull()?:0.0, width.toDoubleOrNull()?:0.0,
                    height.toDoubleOrNull()?:0.0, thickness.toDoubleOrNull()?:0.0,
                    doorW.toDoubleOrNull()?:0.0, doorH.toDoubleOrNull()?:0.0, doorCount.toIntOrNull()?:0,
                    winW.toDoubleOrNull()?:0.0, winH.toDoubleOrNull()?:0.0, winCount.toIntOrNull()?:0, unit, rates)
            }
        }
        result?.let { item { ResultsSection(it) } }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────
// SLAB TAB
// ─────────────────────────────────────────────────────────────────

@Composable
fun SlabTab(viewModel: CalculatorViewModel, rates: List<MaterialRate>, unit: LengthUnit) {
    var length by remember { mutableStateOf("") }; var width by remember { mutableStateOf("") }
    var thickness by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<StructureResult?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SlabIllustration() }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("M20 RCC mix (1:1.5:3)  ·  Steel ~80 kg/m³", fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
        item {
            SectionCard("Slab Dimensions") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalcField("Length", length, { length = it }, unit.shortLabel, Modifier.weight(1f))
                    CalcField("Width", width, { width = it }, unit.shortLabel, Modifier.weight(1f))
                }
                CalcField("Thickness (e.g. 5 in / 125 mm)", thickness, { thickness = it }, unit.shortLabel, Modifier.fillMaxWidth())
            }
        }
        item { CalcButton { result = viewModel.calculateSlab(length.toDoubleOrNull()?:0.0, width.toDoubleOrNull()?:0.0, thickness.toDoubleOrNull()?:0.0, unit, rates) } }
        result?.let { item { ResultsSection(it) } }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────
// COLUMN TAB
// ─────────────────────────────────────────────────────────────────

@Composable
fun ColumnTab(viewModel: CalculatorViewModel, rates: List<MaterialRate>, unit: LengthUnit) {
    var dim1 by remember { mutableStateOf("") }; var dim2 by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }; var count by remember { mutableStateOf("1") }
    var isCircular by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<StructureResult?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ColumnIllustration() }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("M20 RCC mix (1:1.5:3)  ·  Steel ~160 kg/m³", fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(false to "Rectangular", true to "Circular").forEach { (circ, label) ->
                    FilterChip(selected = isCircular == circ, onClick = { isCircular = circ },
                        label = { Text(label, fontWeight = if (isCircular == circ) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(if (circ) Icons.Default.RadioButtonChecked else Icons.Default.CropSquare, contentDescription = null, modifier = Modifier.size(16.dp)) })
                }
            }
        }
        item {
            SectionCard("Column Dimensions") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalcField(if (isCircular) "Diameter" else "Length", dim1, { dim1 = it }, unit.shortLabel, Modifier.weight(1f))
                    if (!isCircular) CalcField("Width", dim2, { dim2 = it }, unit.shortLabel, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalcField("Height", height, { height = it }, unit.shortLabel, Modifier.weight(1f))
                    CountField("No. of Columns", count, { count = it }, Modifier.weight(1f))
                }
            }
        }
        item {
            CalcButton {
                result = viewModel.calculateColumn(dim1.toDoubleOrNull()?:0.0, dim2.toDoubleOrNull()?:0.0,
                    height.toDoubleOrNull()?:0.0, count.toIntOrNull()?:1, isCircular, unit, rates)
            }
        }
        result?.let { item { ResultsSection(it) } }
        item { Spacer(Modifier.height(80.dp)) }
    }
}