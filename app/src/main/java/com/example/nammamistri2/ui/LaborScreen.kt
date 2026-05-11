package com.example.nammamistri2.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nammamistri2.ui.theme.*
import com.example.nammamistri2.viewmodel.LaborViewModel
import com.example.nammamistri2.viewmodel.TeamSummary
import com.example.nammamistri2.viewmodel.WorkerState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaborScreen(viewModel: LaborViewModel = viewModel()) {
    val workerStates by viewModel.workerStates.collectAsState(initial = emptyList())
    val teamSummary by viewModel.teamSummary.collectAsState(initial = TeamSummary())
    val selectedDate by viewModel.selectedDate.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Attendance", "Payments", "Summary")
    val context = LocalContext.current
    
    var showAddWorkerDialog by remember { mutableStateOf(false) }

    val dateStr = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date(selectedDate))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Labor Diary", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 20.sp)
                        Text(dateStr, fontSize = 12.sp, color = TextGray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { /* Handle back */ }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Filter */ }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = PrimaryOrange)
                    }
                    IconButton(onClick = {
                        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newCal = Calendar.getInstance().apply { set(y, m, d) }
                                viewModel.selectDate(newCal.timeInMillis)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Calendar", tint = PrimaryOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundCream)
            )
        },
        floatingActionButton = {
            Button(
                onClick = { showAddWorkerDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(56.dp)
                    .padding(horizontal = 4.dp)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp), spotColor = PrimaryOrange)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Worker", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = BackgroundCream
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = BackgroundCream,
                contentColor = PrimaryOrange,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryOrange
                    )
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp) 
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SummaryCard(teamSummary)
                }

                when (selectedTab) {
                    0, 1 -> {
                        items(workerStates) { state ->
                            WorkerCard(
                                workerState = state,
                                viewModel = viewModel,
                                isPaymentMode = selectedTab == 1
                            )
                        }
                    }
                    2 -> {
                        item { SummaryDetailSection(teamSummary) }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showAddWorkerDialog) {
        AddWorkerDialog(
            onDismiss = { showAddWorkerDialog = false },
            onAdd = { name, wage, role ->
                viewModel.addWorker(name, wage, role)
                showAddWorkerDialog = false
            }
        )
    }
}

@Composable
fun SummaryCard(summary: TeamSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(20.dp), spotColor = Color.LightGray.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SummaryItem("Workers", "${summary.totalWorkers}", Modifier.weight(1f))
            SummaryItem("Earned", "₹${summary.totalEarnings.toInt()}", Modifier.weight(1.2f))
            SummaryItem("Paid", "₹${summary.totalPaid.toInt()}", Modifier.weight(1f))
            SummaryItem("Balance", "₹${summary.totalBalance.toInt()}", Modifier.weight(1.2f), PrimaryOrange)
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = TextDark) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
        Text(label, fontSize = 11.sp, color = TextGray, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun WorkerCard(workerState: WorkerState, viewModel: LaborViewModel, isPaymentMode: Boolean) {
    val worker = workerState.worker
    val initials = worker.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp), spotColor = Color.LightGray.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(PrimaryOrange.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials, color = PrimaryOrange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(worker.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                    Text(worker.role, fontSize = 11.sp, color = TextGray)
                }

                if (!isPaymentMode) {
                    StatusChip(
                        status = when (workerState.todayEntry?.attendance) {
                            1.0 -> "Present"
                            0.5 -> "Half Day"
                            0.0 -> "Absent"
                            else -> "Mark"
                        },
                        onClick = {
                            val next = when(workerState.todayEntry?.attendance) {
                                1.0 -> 0.5
                                0.5 -> 0.0
                                else -> 1.0
                            }
                            viewModel.markAttendance(worker.id, next)
                        }
                    )
                } else {
                    IconButton(onClick = { /* Open Payment Dialog */ }) {
                        Icon(Icons.Default.Payment, contentDescription = "Pay", tint = PrimaryOrange)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = BackgroundCream, thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WorkerStat("Daily Wage", "₹${worker.dailyWage.toInt()}")
                WorkerStat("Earned", "₹${workerState.totalEarned.toInt()}")
                WorkerStat("Paid", "₹${workerState.totalPaid.toInt()}", AttendanceAbsent)
                WorkerStat("Balance", "₹${workerState.balance.toInt()}", PrimaryOrange)
            }
        }
    }
}

@Composable
fun WorkerStat(label: String, value: String, color: Color = TextDark) {
    Column {
        Text(label, fontSize = 10.sp, color = TextGray)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun StatusChip(status: String, onClick: () -> Unit) {
    val (bgColor, textColor) = when (status) {
        "Present" -> AttendancePresent.copy(alpha = 0.15f) to AttendancePresent
        "Absent" -> AttendanceAbsent.copy(alpha = 0.15f) to AttendanceAbsent
        "Half Day" -> AttendanceHalfDay.copy(alpha = 0.15f) to AttendanceHalfDay
        else -> Color.LightGray.copy(alpha = 0.2f) to TextGray
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(status, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

@Composable
fun SummaryDetailSection(summary: TeamSummary) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text("Financial Overview", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ReportRow("Total Earnings", "₹${summary.totalEarnings.toInt()}")
                ReportRow("Total Paid", "₹${summary.totalPaid.toInt()}", AttendanceAbsent)
                HorizontalDivider(color = BackgroundCream)
                ReportRow("Total Outstanding", "₹${summary.totalBalance.toInt()}", PrimaryOrange, isBold = true)
            }
        }
    }
}

@Composable
fun ReportRow(label: String, value: String, valueColor: Color = TextDark, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = TextGray)
        Text(value, fontSize = 15.sp, fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Bold, color = valueColor)
    }
}

@Composable
fun AddWorkerDialog(onDismiss: () -> Unit, onAdd: (String, Double, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var wage by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Worker", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role (e.g. Mason)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = wage, onValueChange = { wage = it }, label = { Text("Wage") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(name, wage.toDoubleOrNull() ?: 0.0, role) }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)) { Text("Save") }
        }
    )
}
