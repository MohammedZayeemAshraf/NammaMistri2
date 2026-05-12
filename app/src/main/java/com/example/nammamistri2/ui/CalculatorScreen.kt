package com.example.nammamistri2.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.nammamistri2.data.MaterialRate
import com.example.nammamistri2.viewmodel.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────
// COLOUR HELPERS
// ─────────────────────────────────────────────────────────────────
private val OrangePrimary = Color(0xFFE65100)
private val OrangeLight   = Color(0xFFFFF3E0)
private val BluePrimary   = Color(0xFF1565C0)
private val GreenDark     = Color(0xFF2E7D32)
private val PurpleDark    = Color(0xFF6A1B9A)

// ─────────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel) {
    val rates   by viewModel.materialRates.collectAsState()
    var selTab  by remember { mutableIntStateOf(0) }
    var selUnit by remember { mutableStateOf(LengthUnit.FEET) }
    val tabs = listOf("Wall", "Room", "Slab", "Column")

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Modern Unit Selection
        ModernFormCard(
            title = "Measurement Unit",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ModernPillToggle(
                options = listOf(LengthUnit.FEET.shortLabel, LengthUnit.METERS.shortLabel, LengthUnit.INCHES.shortLabel),
                selectedIndex = listOf(LengthUnit.FEET, LengthUnit.METERS, LengthUnit.INCHES).indexOf(selUnit),
                onSelectionChanged = { index ->
                    selUnit = listOf(LengthUnit.FEET, LengthUnit.METERS, LengthUnit.INCHES)[index]
                }
            )
        }
        
        // Modern Material 3 Tabs
        TabRow(
            selectedTabIndex = selTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                if (selTab < tabPositions.size) {
                    Box(
                        Modifier
                            .tabIndicatorOffset(tabPositions[selTab])
                            .height(4.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                    )
                }
            }
        ) {
            tabs.forEachIndexed { i, t ->
                Tab(
                    selected = selTab == i,
                    onClick = { selTab = i },
                    text = {
                        Text(
                            t,
                            fontWeight = if (selTab == i) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }
        
        when (selTab) {
            0 -> WallTab(viewModel, rates, selUnit)
            1 -> RoomTab(viewModel, rates, selUnit)
            2 -> SlabTab(viewModel, rates, selUnit)
            3 -> ColumnTab(viewModel, rates, selUnit)
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// BRICK PICKER DIALOG
// ─────────────────────────────────────────────────────────────────

@Composable
fun BrickPickerDialog(initial: BrickType, onDismiss: () -> Unit, onSelect: (BrickType) -> Unit) {
    var selected by remember { mutableStateOf(initial) }
    var query    by remember { mutableStateOf("") }

    val filtered = BrickType.entries.filter {
        query.isBlank() || it.label.contains(query, true) ||
        it.description.contains(query, true) || it.sizeLabel.contains(query, true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Select Brick / Block Type",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    label = { Text("Search bricks\u2026") },
                    leadingIcon  = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Default.Clear, null, Modifier.size(18.dp)) } },
                    singleLine = true, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 380.dp)) {
                    filtered.forEach { bt ->
                        val isSel = selected == bt
                        val bg by animateColorAsState(if (isSel) OrangeLight else MaterialTheme.colorScheme.surface, tween(200), label = "bb")
                        Surface(shape = RoundedCornerShape(16.dp), color = bg,
                            modifier = Modifier.fillMaxWidth()
                                .border(if (isSel) 2.dp else 1.dp,
                                    if (isSel) OrangePrimary else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(16.dp))
                                .clickable { selected = bt }) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(52.dp).clip(CircleShape)
                                        .background(if (isSel) OrangePrimary.copy(.12f) else MaterialTheme.colorScheme.surfaceVariant)) {
                                    Text(bt.emoji, fontSize = 26.sp, textAlign = TextAlign.Center)
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(bt.label, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                        color = if (isSel) OrangePrimary else MaterialTheme.colorScheme.onSurface)
                                    Text(bt.sizeLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold)
                                    Text(bt.description, fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(.6f), lineHeight = 14.sp)
                                }
                                if (isSel) Icon(Icons.Default.CheckCircle, null, tint = OrangePrimary, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                    Button(onClick = { onSelect(selected); onDismiss() }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)) {
                        Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Select Brick")
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// ILLUSTRATIONS
// ─────────────────────────────────────────────────────────────────

private fun android.graphics.Canvas.drawDim(
    x1: Float, y1: Float, x2: Float, y2: Float,
    label: String, p: android.graphics.Paint, ap: android.graphics.Paint
) {
    drawLine(x1, y1, x2, y2, ap)
    val dx = x2 - x1; val dy = y2 - y1
    val len = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    if (len == 0f) return
    val ux = dx / len; val uy = dy / len; val a = 10f
    drawLine(x1, y1, x1 + ux * a - uy * a * .4f, y1 + uy * a + ux * a * .4f, ap)
    drawLine(x1, y1, x1 + ux * a + uy * a * .4f, y1 + uy * a - ux * a * .4f, ap)
    drawLine(x2, y2, x2 - ux * a - uy * a * .4f, y2 - uy * a + ux * a * .4f, ap)
    drawLine(x2, y2, x2 - ux * a + uy * a * .4f, y2 - uy * a - ux * a * .4f, ap)
    drawText(label, (x1 + x2) / 2f, (y1 + y2) / 2f - 6f, p)
}

@Composable
fun WallIllustration() {
    Canvas(modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFF8F0))) {
        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas; val w = size.width; val h = size.height
            val wL = 44f + 20f; val wT = 44f; val wR = w - 44f - 20f; val wB = h - 44f
            nc.drawRect(wL, wT, wR, wB, android.graphics.Paint().apply { color = Color(0xFFE8D5B7).toArgb() })
            val bp = android.graphics.Paint().apply { color = Color(0xFFBCAAA4).toArgb(); strokeWidth = 1f }
            val bH = 26f; val bW = 58f
            var by = wT + bH; while (by < wB) { nc.drawLine(wL, by, wR, by, bp); by += bH }
            var row = 0; var cy = wT
            while (cy < wB) {
                val off = if (row % 2 == 0) 0f else bW / 2f; var bx = wL + off
                while (bx < wR) { nc.drawLine(bx, cy, bx, minOf(cy + bH, wB), bp); bx += bW }
                cy += bH; row++
            }
            nc.drawRect(wL, wT, wR, wB, android.graphics.Paint().apply { color = Color(0xFF8D6E63).toArgb(); strokeWidth = 2.5f; style = android.graphics.Paint.Style.STROKE })
            val dp = android.graphics.Paint().apply { color = BluePrimary.toArgb(); strokeWidth = 1.8f }
            val lp = android.graphics.Paint().apply { color = Color(0xFF263238).toArgb(); textSize = 28f; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD }
            nc.drawDim(wL, wB + 16f, wR, wB + 16f, "Length (L)", lp, dp)
            val wHt = wB - wT
            nc.save(); nc.rotate(-90f, wL - 16f, (wT + wB) / 2f)
            nc.drawDim(wL - 16f - wHt / 2, (wT + wB) / 2f, wL - 16f + wHt / 2, (wT + wB) / 2f, "Height (H)", lp, dp)
            nc.restore()
        }
    }
}

@Composable
fun RoomIllustration() {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = com.example.nammamistri2.R.drawable.room),
        contentDescription = "Room diagram",
        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp))
    )
}

@Composable
fun RoomIllustrationOld() {
    Canvas(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF0F4FF))) {
        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas; val w = size.width; val h = size.height
            val m = 52f; val wt = 20f
            val oL = m; val oT = m; val oR = w - m; val oB = h - m
            val iL = oL + wt; val iT = oT + wt; val iR = oR - wt; val iB = oB - wt

            // ── Floor tiles fill ──────────────────────────────────
            val tileFill = android.graphics.Paint().apply { color = Color(0xFFFFF9C4).toArgb() }
            nc.drawRect(iL, iT, iR, iB, tileFill)
            // tile grout lines
            val tileSize = 28f
            val grout = android.graphics.Paint().apply { color = android.graphics.Color.argb(80, 120, 110, 50); strokeWidth = 1f }
            var gx = iL + tileSize; while (gx < iR) { nc.drawLine(gx, iT, gx, iB, grout); gx += tileSize }
            var gy = iT + tileSize; while (gy < iB) { nc.drawLine(iL, gy, iR, gy, grout); gy += tileSize }

            // ── Walls (cross-hatched) ──────────────────────────────
            val wallFill = android.graphics.Paint().apply { color = Color(0xFFCFD8DC).toArgb() }
            nc.drawRect(oL, oT, oR, iT, wallFill)
            nc.drawRect(oL, iB, oR, oB, wallFill)
            nc.drawRect(oL, oT, iL, oB, wallFill)
            nc.drawRect(iR, oT, oR, oB, wallFill)
            // hatch pattern on walls
            val hatch = android.graphics.Paint().apply { color = android.graphics.Color.argb(70, 55, 71, 79); strokeWidth = 1f }
            val hs = 7f
            // top wall
            var hi = oL; while (hi < oR + h) { nc.drawLine(hi, oT, hi - wt, oT + wt, hatch); hi += hs }
            // bottom wall
            hi = oL; while (hi < oR + h) { nc.drawLine(hi, iB, hi - wt, oB, hatch); hi += hs }
            // left wall
            hi = oT; while (hi < oB + w) { nc.drawLine(oL, hi, iL, hi + wt, hatch); hi += hs }
            // right wall
            hi = oT; while (hi < oB + w) { nc.drawLine(iR, hi, oR, hi + wt, hatch); hi += hs }

            // Door opening on bottom wall
            val doorW = 36f
            val dL = (oL + iL) / 2f; val dR = dL + doorW
            // blank door gap
            nc.drawRect(dL, iB, dR, oB, android.graphics.Paint().apply { color = Color(0xFFF0F4FF).toArgb() })
            // door swing arc
            val doorArc = android.graphics.Paint().apply { color = Color(0xFFFF8F00).toArgb(); strokeWidth = 1.8f; style = android.graphics.Paint.Style.STROKE }
            val doorRect = android.graphics.RectF(dL, iB - doorW, dL + doorW * 2, iB + doorW)
            nc.drawArc(doorRect, 180f, 90f, false, doorArc)
            nc.drawLine(dL, iB, dR, iB, doorArc)

            // Window opening on right wall
            val winH2 = 28f; val winY = (oT + oB) / 2f - winH2 / 2
            nc.drawRect(iR, winY, oR, winY + winH2, android.graphics.Paint().apply { color = Color(0xFFF0F4FF).toArgb() })
            val winPaint = android.graphics.Paint().apply { color = Color(0xFF2196F3).toArgb(); strokeWidth = 1.5f; style = android.graphics.Paint.Style.STROKE }
            nc.drawRect(iR, winY, oR, winY + winH2, winPaint)
            nc.drawLine((iR + oR) / 2f, winY, (iR + oR) / 2f, winY + winH2, winPaint)

            // Outer + inner border
            val border = android.graphics.Paint().apply { color = Color(0xFF37474F).toArgb(); strokeWidth = 2.2f; style = android.graphics.Paint.Style.STROKE }
            nc.drawRect(oL, oT, oR, oB, border)
            val innerBorder = android.graphics.Paint().apply { color = Color(0xFF546E7A).toArgb(); strokeWidth = 1f; style = android.graphics.Paint.Style.STROKE }
            nc.drawRect(iL, iT, iR, iB, innerBorder)

            // Label inside room
            val roomLabelP = android.graphics.Paint().apply {
                color = Color(0xFF78909C).toArgb(); textSize = 24f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
            }
            nc.drawText("Room (Plan View)", (iL + iR) / 2f, (iT + iB) / 2f + 8f, roomLabelP)

            // Dimension arrows
            val dp = android.graphics.Paint().apply { color = BluePrimary.toArgb(); strokeWidth = 1.8f }
            val lp = android.graphics.Paint().apply { color = Color(0xFF263238).toArgb(); textSize = 24f; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD }
            nc.drawDim(oL, oB + 14f, oR, oB + 14f, "Length (L)", lp, dp)
            val rH = oB - oT
            nc.save(); nc.rotate(-90f, oL - 14f, (oT + oB) / 2f)
            nc.drawDim(oL - 14f - rH / 2, (oT + oB) / 2f, oL - 14f + rH / 2, (oT + oB) / 2f, "Width (W)", lp, dp)
            nc.restore()
        }
    }
}

@Composable
fun SlabIllustration() {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = com.example.nammamistri2.R.drawable.slab),
        contentDescription = "Slab diagram",
        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp))
    )
}

@Composable
fun SlabIllustrationOld() {
    Canvas(modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF4F4EE))) {
        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas; val w = size.width; val h = size.height
            val cover = 20f          // concrete cover px
            val sL = 48f; val sR = w - 48f; val sT = 52f; val sB = h - 60f
            val slabH = sB - sT

            // ── 1. Concrete body ─────────────────────────────────
            val concFill = android.graphics.Paint().apply { color = Color(0xFFB0BEC5).toArgb() }
            nc.drawRect(sL, sT, sR, sB, concFill)
            // aggregate dots (realistic concrete texture)
            val aggPaint = android.graphics.Paint().apply { color = Color(0xFF90A4AE).toArgb() }
            val rng = java.util.Random(42L)
            for (i in 0 until 55) {
                val ax = sL + cover + rng.nextFloat() * (sR - sL - cover * 2)
                val ay = sT + cover + rng.nextFloat() * (slabH - cover * 2)
                nc.drawCircle(ax, ay, 3f + rng.nextFloat() * 3f, aggPaint)
            }

            // ── 2. Cover zone lines (dashed) ─────────────────────
            val coverPaint = android.graphics.Paint().apply {
                color = Color(0xFF546E7A).toArgb(); strokeWidth = 1.2f
                style = android.graphics.Paint.Style.STROKE
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f, 4f), 0f)
            }
            nc.drawRect(sL + cover, sT + cover, sR - cover, sB - cover, coverPaint)

            // ── 3. Main reinforcement bars (bottom zone, horizontal) ─
            val barPaint = android.graphics.Paint().apply { color = Color(0xFFBF360C).toArgb(); strokeWidth = 4.5f; style = android.graphics.Paint.Style.STROKE; strokeCap = android.graphics.Paint.Cap.ROUND }
            val mainBarY1 = sB - cover - 5f
            val mainBarY2 = sB - cover - 16f
            val barSpacing = (sR - sL - cover * 2) / 5f
            for (i in 0..5) {
                val bx = sL + cover + barSpacing * i
                // draw bar circle (cross-section view)
                nc.drawCircle(bx, mainBarY1, 5f, android.graphics.Paint().apply { color = Color(0xFFBF360C).toArgb() })
                nc.drawCircle(bx, mainBarY2, 3.5f, android.graphics.Paint().apply { color = Color(0xFFE57373).toArgb() })
            }
            // horizontal bar lines (front view bars)
            val barLinePaint = android.graphics.Paint().apply { color = Color(0xFFBF360C).toArgb(); strokeWidth = 3.5f; strokeCap = android.graphics.Paint.Cap.ROUND }
            nc.drawLine(sL + cover, mainBarY1, sR - cover, mainBarY1, barLinePaint)
            nc.drawLine(sL + cover, mainBarY2, sR - cover, mainBarY2, barLinePaint)

            // ── 4. Distribution bars (top zone, thinner) ─────────
            val distBarPaint = android.graphics.Paint().apply { color = Color(0xFFEF6C00).toArgb(); strokeWidth = 2f; strokeCap = android.graphics.Paint.Cap.ROUND }
            val distBarY1 = sT + cover + 5f
            val distBarY2 = sT + cover + 16f
            nc.drawLine(sL + cover, distBarY1, sR - cover, distBarY1, distBarPaint)
            nc.drawLine(sL + cover, distBarY2, sR - cover, distBarY2, distBarPaint)
            // distribution bar circles
            val distSpacing = (sR - sL - cover * 2) / 8f
            for (i in 0..8) {
                val bx = sL + cover + distSpacing * i
                nc.drawCircle(bx, distBarY1, 3f, android.graphics.Paint().apply { color = Color(0xFFEF6C00).toArgb() })
            }

            // ── 5. Thickness callout arrow ────────────────────────
            val thickPaint = android.graphics.Paint().apply { color = Color(0xFF37474F).toArgb(); strokeWidth = 1.5f }
            val thickLabelP = android.graphics.Paint().apply { color = Color(0xFF37474F).toArgb(); textSize = 21f; textAlign = android.graphics.Paint.Align.LEFT; typeface = android.graphics.Typeface.DEFAULT_BOLD }
            nc.drawLine(sR + 12f, sT, sR + 12f, sB, thickPaint)
            nc.drawLine(sR + 8f, sT, sR + 16f, sT, thickPaint)
            nc.drawLine(sR + 8f, sB, sR + 16f, sB, thickPaint)
            nc.drawText("D", sR + 16f, (sT + sB) / 2f + 8f, thickLabelP)

            // ── 6. Outer border ───────────────────────────────────
            val border = android.graphics.Paint().apply { color = Color(0xFF263238).toArgb(); strokeWidth = 2.5f; style = android.graphics.Paint.Style.STROKE }
            nc.drawRect(sL, sT, sR, sB, border)

            // ── 7. Legend ─────────────────────────────────────────
            val legP = android.graphics.Paint().apply { color = Color(0xFF37474F).toArgb(); textSize = 20f }
            nc.drawCircle(sL + 8f, sB + 20f, 5f, android.graphics.Paint().apply { color = Color(0xFFBF360C).toArgb() })
            nc.drawText("Main bars", sL + 18f, sB + 25f, legP)
            nc.drawCircle(sL + 105f, sB + 20f, 3.5f, android.graphics.Paint().apply { color = Color(0xFFEF6C00).toArgb() })
            nc.drawText("Dist. bars", sL + 115f, sB + 25f, legP)

            // ── 8. Dimension arrows ───────────────────────────────
            val dp = android.graphics.Paint().apply { color = BluePrimary.toArgb(); strokeWidth = 1.8f }
            val lp = android.graphics.Paint().apply { color = Color(0xFF263238).toArgb(); textSize = 24f; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD }
            nc.drawDim(sL, sT - 16f, sR, sT - 16f, "Length (L)", lp, dp)
        }
    }
}

@Composable
fun ColumnIllustration() {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = com.example.nammamistri2.R.drawable.column),
        contentDescription = "Column diagram",
        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(16.dp))
    )
}

@Composable
fun ColumnIllustrationOld() {
    Canvas(modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF3E5F5))) {
        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas; val w = size.width; val h = size.height
            val cover = 14f

            // ════════════════════════════════════════════════════
            // LEFT: ELEVATION VIEW
            // ════════════════════════════════════════════════════
            val eL = 44f; val eR = w * 0.47f; val eT = 28f; val eB = h - 48f
            val colW = eR - eL; val colH = eB - eT

            // concrete body
            val concFill = android.graphics.Paint().apply { color = Color(0xFFB0BEC5).toArgb() }
            nc.drawRect(eL, eT, eR, eB, concFill)
            // aggregate texture
            val aggP = android.graphics.Paint().apply { color = Color(0xFF90A4AE).toArgb() }
            val rng = java.util.Random(7L)
            for (i in 0 until 30) {
                val ax = eL + cover + rng.nextFloat() * (colW - cover * 2)
                val ay = eT + cover + rng.nextFloat() * (colH - cover * 2)
                nc.drawCircle(ax, ay, 2.5f + rng.nextFloat() * 2f, aggP)
            }

            // 4 corner main bars (elevation — 2 visible lines)
            val barPaint = android.graphics.Paint().apply { color = Color(0xFFBF360C).toArgb(); strokeWidth = 4f; strokeCap = android.graphics.Paint.Cap.ROUND }
            nc.drawLine(eL + cover, eT + cover, eL + cover, eB - cover, barPaint)  // left bar
            nc.drawLine(eR - cover, eT + cover, eR - cover, eB - cover, barPaint)  // right bar

            // stirrups (rectangular links every ~colH/6)
            val stirrupPaint = android.graphics.Paint().apply { color = Color(0xFFE53935).toArgb(); strokeWidth = 2.2f; style = android.graphics.Paint.Style.STROKE }
            val sCount = 7; val sStep = colH / (sCount + 1)
            for (i in 1..sCount) {
                val sy = eT + sStep * i
                nc.drawRect(eL + cover - 2f, sy - 5f, eR - cover + 2f, sy + 5f, stirrupPaint)
            }
            // stirrup hook tails at top
            val hookP = android.graphics.Paint().apply { color = Color(0xFFE53935).toArgb(); strokeWidth = 2f; strokeCap = android.graphics.Paint.Cap.ROUND }
            nc.drawLine(eL + cover - 2f, eT + sStep - 5f, eL + cover + 8f, eT + sStep - 14f, hookP)
            nc.drawLine(eR - cover + 2f, eT + sStep - 5f, eR - cover - 8f, eT + sStep - 14f, hookP)

            // outer border
            nc.drawRect(eL, eT, eR, eB, android.graphics.Paint().apply { color = Color(0xFF37474F).toArgb(); strokeWidth = 2.5f; style = android.graphics.Paint.Style.STROKE })

            // cover dashed lines
            val cvP = android.graphics.Paint().apply { color = Color(0xFF546E7A).toArgb(); strokeWidth = 1f; style = android.graphics.Paint.Style.STROKE; pathEffect = android.graphics.DashPathEffect(floatArrayOf(5f, 4f), 0f) }
            nc.drawRect(eL + cover, eT + cover, eR - cover, eB - cover, cvP)

            // ════════════════════════════════════════════════════
            // RIGHT: CROSS-SECTION (PLAN VIEW)
            // ════════════════════════════════════════════════════
            val csL = w * 0.55f; val csR = w - 20f; val csSize = minOf(csR - csL, eB - eT)
            val csT = eT + (colH - csSize) / 2; val csB = csT + csSize
            val csCx = (csL + csR) / 2f; val csCy = (csT + csB) / 2f

            // concrete fill
            nc.drawRect(csL, csT, csR, csB, concFill)
            // cross-hatch
            val hatch = android.graphics.Paint().apply { color = android.graphics.Color.argb(55, 55, 71, 79); strokeWidth = 1f }
            var hi = csL; while (hi < csR + csSize) { nc.drawLine(hi, csT, hi - csSize, csT + csSize, hatch); hi += 7f }

            // 4 corner main bars (circles)
            val barDot = android.graphics.Paint().apply { color = Color(0xFFBF360C).toArgb() }
            val barOutline = android.graphics.Paint().apply { color = Color(0xFF263238).toArgb(); strokeWidth = 1f; style = android.graphics.Paint.Style.STROKE }
            listOf(csL + cover to csT + cover, csR - cover to csT + cover,
                   csL + cover to csB - cover, csR - cover to csB - cover).forEach {
                nc.drawCircle(it.first, it.second, 7f, barDot)
                nc.drawCircle(it.first, it.second, 7f, barOutline)
            }
            // mid bars on each face
            val midBarPaint = android.graphics.Paint().apply { color = Color(0xFFE57373).toArgb() }
            listOf(csCx to csT + cover, csCx to csB - cover,
                   csL + cover to csCy, csR - cover to csCy).forEach {
                nc.drawCircle(it.first, it.second, 5f, midBarPaint)
                nc.drawCircle(it.first, it.second, 5f, barOutline)
            }
            // stirrup rectangle
            nc.drawRect(csL + cover - 2f, csT + cover - 2f, csR - cover + 2f, csB - cover + 2f, stirrupPaint)
            // outer border
            nc.drawRect(csL, csT, csR, csB, android.graphics.Paint().apply { color = Color(0xFF37474F).toArgb(); strokeWidth = 2.5f; style = android.graphics.Paint.Style.STROKE })
            // section label
            val secLabelP = android.graphics.Paint().apply { color = Color(0xFF546E7A).toArgb(); textSize = 18f; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD }
            nc.drawText("Section A-A", csCx, csB + 16f, secLabelP)

            // section cut line on elevation
            val cutLinePaint = android.graphics.Paint().apply { color = Color(0xFF263238).toArgb(); strokeWidth = 1.8f; pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f, 4f, 2f, 4f), 0f) }
            val cutY = csCy  // midpoint
            nc.drawLine(eL - 10f, cutY, eR + 10f, cutY, cutLinePaint)
            val cutLabelP2 = android.graphics.Paint().apply { color = Color(0xFF263238).toArgb(); textSize = 18f; textAlign = android.graphics.Paint.Align.LEFT; typeface = android.graphics.Typeface.DEFAULT_BOLD }
            nc.drawText("A", eL - 10f, cutY - 4f, cutLabelP2)
            nc.drawText("A", eR + 2f, cutY - 4f, cutLabelP2)

            // ════════════════════════════════════════════════════
            // DIMENSION ARROWS (elevation)
            // ════════════════════════════════════════════════════
            val dp = android.graphics.Paint().apply { color = PurpleDark.toArgb(); strokeWidth = 1.8f }
            val lp = android.graphics.Paint().apply { color = Color(0xFF263238).toArgb(); textSize = 22f; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD }
            // height on left of elevation
            nc.save(); nc.rotate(-90f, eL - 16f, (eT + eB) / 2f)
            nc.drawDim(eL - 16f - colH / 2, (eT + eB) / 2f, eL - 16f + colH / 2, (eT + eB) / 2f, "Height (H)", lp, dp)
            nc.restore()
            // width below elevation
            nc.drawDim(eL, eB + 16f, eR, eB + 16f, "Width (B)", lp, dp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// SHARED HELPERS
// ─────────────────────────────────────────────────────────────────

@Composable
fun CalcField(label: String, value: String, onChange: (String) -> Unit, unitLabel: String, modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text("$label ($unitLabel)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
        modifier = modifier, shape = RoundedCornerShape(12.dp))
}

@Composable
fun CountField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
        modifier = modifier, shape = RoundedCornerShape(12.dp))
}

@Composable
fun SectionCard(title: String, accent: Color = MaterialTheme.colorScheme.primary,
                content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accent)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThicknessDropdown(label: String, value: String, onChange: (String) -> Unit, unit: LengthUnit, modifier: Modifier = Modifier) {
    data class P(val t: String, val m: Double)
    val presets = listOf(P("4.5\"(115mm)", .115), P("9\"(230mm)", .230), P("13.5\"(345mm)", .345),
        P("5\"(125mm)", .125), P("6\"(150mm)", .150), P("8\"(200mm)", .200), P("10\"(250mm)", .250), P("12\"(300mm)", .300))
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(value = value, onValueChange = onChange, label = { Text("$label (${unit.shortLabel})") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            presets.forEach { p ->
                val cv = String.format("%.3f", p.m / unit.factor)
                DropdownMenuItem(text = { Text("${p.t}  \u2192  $cv ${unit.shortLabel}", fontSize = 12.sp) },
                    onClick = { onChange(cv); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Enum<T>> GenericDropdown(label: String, entries: List<T>, selected: T,
                                   onSelect: (T) -> Unit, labelOf: (T) -> String,
                                   modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(value = labelOf(selected), onValueChange = {}, readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            singleLine = true, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            entries.forEach { e ->
                DropdownMenuItem(text = { Text(labelOf(e), fontSize = 13.sp) },
                    onClick = { onSelect(e); expanded = false })
            }
        }
    }
}

@Composable
fun WastageSlider(wastage: Int, onChange: (Int) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Wastage Factor", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text("+$wastage%", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 13.sp)
            }
        }
        Slider(value = wastage.toFloat(), onValueChange = { onChange(it.toInt()) }, valueRange = 0f..20f, steps = 19, modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("5% recommended", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
            Text("20%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CalcButton(label: String = "Calculate", onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
        Icon(Icons.Default.Calculate, null)
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun OpeningsCard(doorW: String, onDW: (String) -> Unit, doorH: String, onDH: (String) -> Unit,
                 doorCount: String, onDC: (String) -> Unit,
                 winW: String, onWW: (String) -> Unit, winH: String, onWH: (String) -> Unit,
                 winCount: String, onWC: (String) -> Unit, unit: LengthUnit) {
    SectionCard("Deductions \u2013 Doors & Windows") {
        Text("Doors", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.6f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalcField("Width", doorW, onDW, unit.shortLabel, Modifier.weight(1f))
            CalcField("Height", doorH, onDH, unit.shortLabel, Modifier.weight(1f))
            CountField("Count", doorCount, onDC, Modifier.weight(.7f))
        }
        Text("Windows", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.6f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalcField("Width", winW, onWW, unit.shortLabel, Modifier.weight(1f))
            CalcField("Height", winH, onWH, unit.shortLabel, Modifier.weight(1f))
            CountField("Count", winCount, onWC, Modifier.weight(.7f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// HISTORY SECTION
// ─────────────────────────────────────────────────────────────────

@Composable
fun HistorySection(viewModel: CalculatorViewModel) {
    val history by viewModel.history.collectAsState()
    val sdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    if (history.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Calculation History", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            TextButton({ viewModel.clearHistory() }) { Text("Clear All", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        }
        history.forEach { entry ->
            var expanded by remember { mutableStateOf(false) }
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.result.label.ifBlank { entry.result.structureType }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(sdf.format(Date(entry.id)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(.5f))
                        }
                        Text(String.format("%.2f m\u00b3", entry.result.netVolume), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                        entry.result.grandTotal?.let { Text("\u20b9${String.format("%,.0f", it)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenDark, modifier = Modifier.padding(end = 6.dp)) }
                        IconButton({ expanded = !expanded }, modifier = Modifier.size(32.dp)) { Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(18.dp)) }
                        IconButton({ viewModel.deleteHistory(entry.id) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                    }
                    AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            HorizontalDivider(); Spacer(Modifier.height(8.dp))
                            Text("Wastage: +${entry.result.wastagePercent}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(.6f))
                            Spacer(Modifier.height(4.dp))
                            entry.result.materials.forEach { mat ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(mat.name, fontSize = 13.sp)
                                    Text(String.format("%.1f ${mat.unit}", mat.quantity), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// WALL TAB
// ─────────────────────────────────────────────────────────────────

@Composable
fun WallTab(viewModel: CalculatorViewModel, rates: List<MaterialRate>, unit: LengthUnit) {
    var length by remember { mutableStateOf("") }; var height by remember { mutableStateOf("") }; var thickness by remember { mutableStateOf("") }
    var wallType by remember { mutableStateOf(WallType.BRICK_WALL) }; var mortarRatio by remember { mutableStateOf(MortarRatio.RATIO_1_6) }
    var wastage by remember { mutableIntStateOf(5) }
    var doorW by remember { mutableStateOf("3") }; var doorH by remember { mutableStateOf("7") }; var doorCount by remember { mutableStateOf("1") }
    var winW by remember { mutableStateOf("4") }; var winH by remember { mutableStateOf("4") }; var winCount by remember { mutableStateOf("2") }
    var result by remember { mutableStateOf<StructureResult?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { WallIllustration() }
        item {
            SectionCard("Wall Type & Mix", OrangePrimary) {
                GenericDropdown("Wall Type", WallType.entries, wallType, { wallType = it }, { it.label })
                GenericDropdown("Mortar Ratio", MortarRatio.entries, mortarRatio, { mortarRatio = it }, { it.label })
                WastageSlider(wastage) { wastage = it }
            }
        }
        item {
            SectionCard("Wall Dimensions") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalcField("Length", length, { length = it }, unit.shortLabel, Modifier.weight(1f))
                    CalcField("Height", height, { height = it }, unit.shortLabel, Modifier.weight(1f))
                }
                ThicknessDropdown("Thickness", thickness, { thickness = it }, unit, Modifier.fillMaxWidth())
            }
        }
        item { OpeningsCard(doorW, { doorW = it }, doorH, { doorH = it }, doorCount, { doorCount = it }, winW, { winW = it }, winH, { winH = it }, winCount, { winCount = it }, unit) }
        item {
            CalcButton {
                val r = viewModel.calculateWall(
                    length.toDoubleOrNull() ?: 0.0, height.toDoubleOrNull() ?: 0.0, thickness.toDoubleOrNull() ?: 0.0,
                    doorW.toDoubleOrNull() ?: 0.0, doorH.toDoubleOrNull() ?: 0.0, doorCount.toIntOrNull() ?: 0,
                    winW.toDoubleOrNull() ?: 0.0, winH.toDoubleOrNull() ?: 0.0, winCount.toIntOrNull() ?: 0,
                    unit, rates, wallType, mortarRatio, wastage)
                result = r; viewModel.addToHistory(r)
            }
        }
        result?.let { item { WallResultSection(it) } }
        item { HistorySection(viewModel) }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun WallResultSection(result: StructureResult) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = OrangePrimary)) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ResultStat("Net Volume", String.format("%.3f m\u00b3", result.netVolume), Color.White)
                ResultStat("Net Area",   String.format("%.2f m\u00b2", result.netArea), Color.White)
                result.grandTotal?.let { ResultStat("Est. Cost", "\u20b9${String.format("%,.0f", it)}", Color(0xFFFFEB3B)) }
            }
        }
        Text("Materials Required", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        result.materials.forEach { MatRow(it) }
    }
}

// ─────────────────────────────────────────────────────────────────
// SLAB TAB
// ─────────────────────────────────────────────────────────────────

@Composable
fun SlabTab(viewModel: CalculatorViewModel, rates: List<MaterialRate>, unit: LengthUnit) {
    var length by remember { mutableStateOf("") }; var width by remember { mutableStateOf("") }; var thickness by remember { mutableStateOf("") }
    var slabType by remember { mutableStateOf(SlabType.ROOF_SLAB) }
    var grade by remember { mutableStateOf(ConcreteGrade.M20) }
    var steelPct by remember { mutableStateOf("1.0") }
    var wastage by remember { mutableIntStateOf(5) }
    var result by remember { mutableStateOf<SlabResult?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SlabIllustration() }
        // Slab type tabs
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SlabType.entries.forEach { st ->
                    val sel = slabType == st
                    val bg by animateColorAsState(if (sel) BluePrimary else MaterialTheme.colorScheme.surfaceVariant, label = "sl")
                    val tc by animateColorAsState(if (sel) Color.White else MaterialTheme.colorScheme.onSurface, label = "slT")
                    Surface(shape = RoundedCornerShape(12.dp), color = bg,
                        modifier = Modifier.weight(1f).clickable { slabType = st }) {
                        Text(st.label, textAlign = TextAlign.Center, color = tc,
                            fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(10.dp))
                    }
                }
            }
        }
        item {
            SectionCard("Concrete & Steel", BluePrimary) {
                GenericDropdown("Concrete Grade", ConcreteGrade.entries, grade, { grade = it }, { "${it.label} (${it.mixLabel})" })
                OutlinedTextField(value = steelPct, onValueChange = { steelPct = it },
                    label = { Text("Steel % of concrete volume") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                WastageSlider(wastage) { wastage = it }
            }
        }
        item {
            SectionCard("Slab Dimensions") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalcField("Length", length, { length = it }, unit.shortLabel, Modifier.weight(1f))
                    CalcField("Width",  width,  { width = it  }, unit.shortLabel, Modifier.weight(1f))
                }
                ThicknessDropdown("Slab Thickness", thickness, { thickness = it }, unit, Modifier.fillMaxWidth())
            }
        }
        item {
            CalcButton {
                result = viewModel.calculateSlab(
                    length.toDoubleOrNull() ?: 0.0, width.toDoubleOrNull() ?: 0.0,
                    thickness.toDoubleOrNull() ?: 0.0, unit, rates, slabType, grade,
                    steelPct.toDoubleOrNull() ?: 1.0, wastage)
            }
        }
        result?.let { r ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    r.totalCost?.let {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = GreenDark)) {
                            Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Estimated Cost", fontSize = 12.sp, color = Color.White.copy(.8f))
                                Text("\u20b9${String.format("%,.0f", it)}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                Text("Including +${r.wastagePercent}% wastage", fontSize = 11.sp, color = Color.White.copy(.7f))
                            }
                        }
                    }
                    SectionCard("Input Summary", BluePrimary) {
                        StatRow("Slab Type",  r.slabType.label)
                        StatRow("Grade",      "${r.grade.label} (${r.grade.mixLabel})")
                        StatRow("Steel %",    "${r.steelPercent}% of volume")
                        StatRow("Wastage",    "+${r.wastagePercent}%")
                    }
                    SectionCard("Quantities") {
                        StatRow("Slab Area",        String.format("%.2f m\u00b2", r.area))
                        StatRow("Slab Volume",       String.format("%.3f m\u00b3", r.volume))
                        StatRow("Cement",            String.format("%.1f bags", r.cementBags))
                        StatRow("Sand (Fine)",       String.format("%.3f m\u00b3", r.sandM3))
                        StatRow("Aggregate (CA)",    String.format("%.3f m\u00b3", r.aggM3))
                        StatRow("Steel / Rebar",     String.format("%.1f kg", r.steelKg))
                        StatRow("Water (approx.)",   String.format("%.0f litres", r.waterLitres))
                    }
                    r.totalCost?.let {
                        SectionCard("Cost Breakdown", GreenDark) {
                            CostRow("Cement",    r.cementBags, "bags", r.cementCost)
                            CostRow("Sand",      r.sandM3,     "m\u00b3",    r.sandCost)
                            CostRow("Aggregate", r.aggM3,      "m\u00b3",    r.aggCost)
                            CostRow("Steel",     r.steelKg,    "kg",   r.steelCost)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TOTAL", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                Text("\u20b9${String.format("%,.0f", it)}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = GreenDark)
                            }
                        }
                    }
                }
            }
        }
        item { HistorySection(viewModel) }
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
    var roomType    by remember { mutableStateOf(RoomType.BEDROOM) }
    var brickType   by remember { mutableStateOf(BrickType.RED_BRICK) }
    var mortarRatio by remember { mutableStateOf(MortarRatio.RATIO_1_6) }
    var flooringType by remember { mutableStateOf(FlooringType.VITRIFIED) }
    var wastage     by remember { mutableIntStateOf(5) }
    var doorW by remember { mutableStateOf("3") }; var doorH by remember { mutableStateOf("7") }; var doorCount by remember { mutableStateOf("1") }
    var winW  by remember { mutableStateOf("4") }; var winH  by remember { mutableStateOf("4") }; var winCount  by remember { mutableStateOf("2") }
    var showPicker  by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<RoomResult?>(null) }

    if (showPicker) BrickPickerDialog(brickType, { showPicker = false }, { brickType = it })

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { RoomIllustration() }
        // Room type row
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RoomType.entries) { rt ->
                    val sel = roomType == rt
                    val bg by animateColorAsState(if (sel) OrangePrimary else MaterialTheme.colorScheme.surfaceVariant, label = "rt")
                    val tc by animateColorAsState(if (sel) Color.White else MaterialTheme.colorScheme.onSurface, label = "rtT")
                    Surface(shape = RoundedCornerShape(12.dp), color = bg, modifier = Modifier.clickable { roomType = rt }) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(rt.icon, fontSize = 20.sp)
                            Text(rt.label, color = tc, fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
        item {
            SectionCard("Room Dimensions") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalcField("Length", length, { length = it }, unit.shortLabel, Modifier.weight(1f))
                    CalcField("Width",  width,  { width = it  }, unit.shortLabel, Modifier.weight(1f))
                }
                CalcField("Height", height, { height = it }, unit.shortLabel, Modifier.fillMaxWidth())
                ThicknessDropdown("Wall Thickness", thickness, { thickness = it }, unit, Modifier.fillMaxWidth())
            }
        }
        item { OpeningsCard(doorW, { doorW = it }, doorH, { doorH = it }, doorCount, { doorCount = it }, winW, { winW = it }, winH, { winH = it }, winCount, { winCount = it }, unit) }
        item {
            SectionCard("Material Settings", OrangePrimary) {
                // Brick picker button
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().clickable { showPicker = true }) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(brickType.emoji, fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Brick Type", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(brickType.label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(brickType.sizeLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp))
                    }
                }
                GenericDropdown("Mortar Ratio", MortarRatio.entries, mortarRatio, { mortarRatio = it }, { it.label })
                GenericDropdown("Flooring Type", FlooringType.entries, flooringType, { flooringType = it }, { it.label })
                WastageSlider(wastage) { wastage = it }
            }
        }
        item {
            CalcButton("Calculate Room") {
                result = viewModel.calculateRoom(
                    length.toDoubleOrNull() ?: 0.0, width.toDoubleOrNull() ?: 0.0,
                    height.toDoubleOrNull() ?: 0.0, thickness.toDoubleOrNull() ?: 0.0,
                    doorW.toDoubleOrNull() ?: 0.0, doorH.toDoubleOrNull() ?: 0.0, doorCount.toIntOrNull() ?: 0,
                    winW.toDoubleOrNull() ?: 0.0, winH.toDoubleOrNull() ?: 0.0, winCount.toIntOrNull() ?: 0,
                    unit, rates, roomType, brickType, mortarRatio, flooringType, wastage)
            }
        }
        result?.let { r ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    r.totalCost?.let {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = GreenDark)) {
                            Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Estimated Cost", fontSize = 12.sp, color = Color.White.copy(.8f))
                                Text("\u20b9${String.format("%,.0f", it)}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                Text("${r.roomType.icon} ${r.roomType.label}  \u00b7  +${r.wastagePercent}% wastage", fontSize = 11.sp, color = Color.White.copy(.7f))
                            }
                        }
                    }
                    SectionCard("Areas") {
                        StatRow("Total Wall Area",  String.format("%.2f m\u00b2", r.wallArea))
                        StatRow("Floor Area",       String.format("%.2f m\u00b2", r.floorArea))
                        StatRow("Ceiling Area",     String.format("%.2f m\u00b2", r.ceilingArea))
                        StatRow("Paint Area",       String.format("%.2f m\u00b2", r.paintArea))
                        StatRow("Wall Volume",      String.format("%.3f m\u00b3", r.wallVolume))
                    }
                    SectionCard("Masonry Quantities") {
                        StatRow(r.brickType.unitName, String.format("%.0f pieces", r.brickCount))
                        StatRow("Cement",            String.format("%.1f bags",    r.cementBags))
                        StatRow("Sand",              String.format("%.3f m\u00b3", r.sandM3))
                        StatRow("Water (approx.)",   String.format("%.0f litres",  r.waterLitres))
                        if (r.flooringType != FlooringType.NONE)
                            StatRow("Flooring Tiles", String.format("%.0f tiles", r.flooringTiles))
                    }
                    SectionCard("Cost Breakdown", GreenDark) {
                        CostRow(r.brickType.unitName, r.brickCount,    "pieces", r.brickCost)
                        CostRow("Cement",             r.cementBags,    "bags",   r.cementCost)
                        CostRow("Sand",               r.sandM3,        "m\u00b3",      r.sandCost)
                        if (r.flooringType != FlooringType.NONE)
                            CostRow("Flooring", r.flooringTiles, "tiles", r.flooringCost)
                        r.paintCost?.let { CostRow("Paint", r.paintArea, "m\u00b2", r.paintCost) }
                        CostRow("Labor (est.)", r.wallArea, "m\u00b2", r.laborCost)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        r.totalCost?.let {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TOTAL", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                Text("\u20b9${String.format("%,.0f", it)}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = GreenDark)
                            }
                        }
                    }
                }
            }
        }
        item { HistorySection(viewModel) }
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
    var isCircular by remember { mutableStateOf(false) }; var wastage by remember { mutableIntStateOf(5) }
    var result by remember { mutableStateOf<StructureResult?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ColumnIllustration() }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text("M20 RCC mix (1:1.5:3)  \u00b7  Steel ~160 kg/m\u00b3", fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
        item { SectionCard("Wastage") { WastageSlider(wastage) { wastage = it } } }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(false to "Rectangular", true to "Circular").forEach { (circ, lbl) ->
                    FilterChip(selected = isCircular == circ, onClick = { isCircular = circ },
                        label = { Text(lbl, fontWeight = if (isCircular == circ) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(if (circ) Icons.Default.RadioButtonChecked else Icons.Default.CropSquare, null, Modifier.size(16.dp)) })
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
                val r = viewModel.calculateColumn(
                    dim1.toDoubleOrNull() ?: 0.0, dim2.toDoubleOrNull() ?: 0.0,
                    height.toDoubleOrNull() ?: 0.0, count.toIntOrNull() ?: 1, isCircular, unit, rates, wastage)
                result = r; viewModel.addToHistory(r)
            }
        }
        result?.let { item { WallResultSection(it) } }
        item { HistorySection(viewModel) }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────
// SMALL RESULT HELPERS
// ─────────────────────────────────────────────────────────────────

@Composable
fun ResultStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = color.copy(.8f))
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = color)
    }
}

@Composable
fun MatRow(mat: MaterialResult) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(mat.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (mat.ratePerUnit != null)
                    Text("\u20b9${mat.ratePerUnit.toInt()} / ${mat.unit}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(.7f))
                else
                    Text("Add rate in Standard Rates", fontSize = 11.sp, color = MaterialTheme.colorScheme.error.copy(.7f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(when { mat.quantity >= 1000 -> String.format("%.0f", mat.quantity)
                    mat.quantity >= 10 -> String.format("%.1f", mat.quantity)
                    else -> String.format("%.2f", mat.quantity) },
                    fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Text(mat.unit, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                mat.totalCost?.let { Text("\u20b9${String.format("%,.0f", it)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenDark) }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(.7f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun CostRow(name: String, qty: Double, unit: String, cost: Double?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(name, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(String.format("%.1f", qty) + " $unit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(.55f), modifier = Modifier.padding(end = 12.dp))
        Text(cost?.let { "\u20b9${String.format("%,.0f", it)}" } ?: "\u2014",
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = if (cost != null) GreenDark else MaterialTheme.colorScheme.onSurface.copy(.4f))
    }
}
