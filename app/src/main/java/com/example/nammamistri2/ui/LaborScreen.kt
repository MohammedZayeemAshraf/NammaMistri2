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
fun LaborScreen(viewModel: LaborViewModel = viewModel(), onBack: () -> Unit = {}) {
    val workerStates by viewModel.workerStates.collectAsState(initial = emptyList())
    val teamSummary by viewModel.teamSummary.collectAsState(initial = TeamSummary())
    val selectedDate by viewModel.selectedDate.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Attendance", "Payments", "Summary")
    val context = LocalContext.current
    
    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf("Name A-Z") }
    val sortOptions = listOf("Name A-Z", "Name Z-A", "Balance High-Low", "Balance Low-High", "Earned High-Low")

    val sortedWorkerStates = remember(workerStates, sortOption) {
        when (sortOption) {
            "Name A-Z" -> workerStates.sortedBy { it.worker.name }
            "Name Z-A" -> workerStates.sortedByDescending { it.worker.name }
            "Balance High-Low" -> workerStates.sortedByDescending { it.balance }
            "Balance Low-High" -> workerStates.sortedBy { it.balance }
            "Earned High-Low" -> workerStates.sortedByDescending { it.totalEarned }
            else -> workerStates
        }
    }

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
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Sort", tint = PrimaryOrange)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            Text(
                                "Sort Workers By",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextGray,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            sortOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (sortOption == option) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(8.dp))
                                            } else {
                                                Spacer(Modifier.width(24.dp))
                                            }
                                            Text(option, fontSize = 14.sp)
                                        }
                                    },
                                    onClick = {
                                        sortOption = option
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
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
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            
            // Modern Tabs with custom indicator
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        Box(
                            Modifier
                                .tabIndicatorOffset(tabPositions[selectedTab])
                                .height(4.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                        )
                    }
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
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
                    ModernHeaderBanner(
                        title = "Labor Diary",
                        subtitle = "Manage your workforce and track daily records",
                        backgroundColor = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ModernStatsCard(
                                label = "Workers",
                                value = teamSummary.totalWorkers.toString(),
                                icon = Icons.Default.Person,
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                                iconTint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            ModernStatsCard(
                                label = "Earned",
                                value = "₹${teamSummary.totalEarnings.toInt()}",
                                icon = Icons.Default.AttachMoney,
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                                iconTint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ModernStatsCard(
                                label = "Paid",
                                value = "₹${teamSummary.totalPaid.toInt()}",
                                icon = Icons.Default.AccountBalance,
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                                iconTint = Color(0xFF1565C0),
                                modifier = Modifier.weight(1f)
                            )
                            ModernStatsCard(
                                label = "Balance",
                                value = "₹${teamSummary.totalBalance.toInt()}",
                                icon = Icons.Default.TrendingUp,
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                                iconTint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                when (selectedTab) {
                    0, 1 -> {
                        if (selectedTab == 0) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
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
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(selectedDate)),
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                dateStr,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (sortedWorkerStates.isEmpty()) {
                            item {
                                ModernEmptyState(
                                    icon = Icons.Default.Person,
                                    title = "No Workers Added",
                                    subtitle = "Add workers to track attendance and manage daily labor records",
                                    actionLabel = "Add Worker",
                                    onAction = { showAddWorkerDialog = true }
                                )
                            }
                        } else {
                            items(sortedWorkerStates) { state ->
                                WorkerCard(
                                    workerState = state,
                                    viewModel = viewModel,
                                    isPaymentMode = selectedTab == 1,
                                    selectedDate = selectedDate
                                )
                            }
                        }
                    }
                    2 -> {
                        item { SummaryDetailSection(teamSummary) }
                        if (sortedWorkerStates.isEmpty()) {
                            item {
                                ModernEmptyState(
                                    icon = Icons.Default.Person,
                                    title = "No Workers",
                                    subtitle = "Add workers to see summary details"
                                )
                            }
                        } else {
                            items(sortedWorkerStates) { state ->
                                WorkerDetailCard(state)
                            }
                        }
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
fun AttendanceDateBanner(selectedDate: Long, onTap: () -> Unit) {
    val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(selectedDate))
    val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(selectedDate))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        colors = CardDefaults.cardColors(containerColor = PrimaryOrange),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(dayName, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                Text(dateStr, fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tap to change", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun WorkerCard(workerState: WorkerState, viewModel: LaborViewModel, isPaymentMode: Boolean, selectedDate: Long = System.currentTimeMillis()) {
    val worker = workerState.worker
    val initials = worker.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    val attendanceEntries = remember(workerState.allEntries) {
        workerState.allEntries.filter { it.paymentMode == null }.sortedByDescending { it.date }
    }
    val sdfDay = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

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
                }
            }

            // Show selected date + marked status in attendance mode
            if (!isPaymentMode) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        sdfDay.format(Date(selectedDate)),
                        fontSize = 11.sp,
                        color = TextGray
                    )
                    if (workerState.todayEntry == null) {
                        Text("Not marked yet", fontSize = 11.sp, color = TextGray)
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

            if (isPaymentMode) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showPaymentDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Payment / Advance", fontWeight = FontWeight.Bold)
                }
            }

            // Attendance history toggle (attendance tab only)
            if (!isPaymentMode && attendanceEntries.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { showHistory = !showHistory },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        if (showHistory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (showHistory) "Hide history" else "View attendance history (${attendanceEntries.size} days)",
                        fontSize = 12.sp,
                        color = PrimaryOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (showHistory) {
                    Spacer(Modifier.height(4.dp))
                    attendanceEntries.forEach { entry ->
                        val (statusLabel, statusColor) = when (entry.attendance) {
                            1.0 -> "Present" to AttendancePresent
                            0.5 -> "Half Day" to AttendanceHalfDay
                            else -> "Absent" to AttendanceAbsent
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sdfDay.format(Date(entry.date)), fontSize = 12.sp, color = TextDark)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPaymentDialog) {
        PaymentDialog(
            workerName = worker.name,
            balance = workerState.balance,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount, mode, date ->
                viewModel.addPayment(worker.id, amount, mode, date)
                showPaymentDialog = false
            }
        )
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
fun PaymentDialog(
    workerName: String,
    balance: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, Long) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("Cash") }
    var paymentDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val paymentModes = listOf("Cash", "UPI", "Scanner")
    val context = LocalContext.current
    val dateStr = remember(paymentDate) {
        SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date(paymentDate))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Add Payment", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                Text(workerName, fontSize = 13.sp, color = TextGray)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Balance info
                Card(
                    colors = CardDefaults.cardColors(containerColor = PrimaryOrange.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Outstanding Balance", fontSize = 13.sp, color = TextGray)
                        Text("₹${balance.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryOrange)
                    }
                }

                // Date picker row
                OutlinedCard(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = paymentDate }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newCal = Calendar.getInstance().apply { set(y, m, d) }
                                paymentDate = newCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Payment Date", fontSize = 13.sp, color = TextGray)
                        }
                        Text(dateStr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                }

                // Amount field
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Payment mode selection
                Text("Payment Mode", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paymentModes.forEach { mode ->
                        val isSelected = selectedMode == mode
                        val (icon, label) = when (mode) {
                            "Cash" -> Icons.Default.AttachMoney to "Cash"
                            "UPI" -> Icons.Default.PhoneAndroid to "UPI"
                            else -> Icons.Default.QrCodeScanner to "Scanner"
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedMode = mode },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PrimaryOrange else BackgroundCream
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = mode,
                                    tint = if (isSelected) Color.White else TextGray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextGray
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onConfirm(amt, selectedMode, paymentDate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirm Payment", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextGray) }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun WorkerDetailCard(workerState: WorkerState) {
    val worker = workerState.worker
    val initials = worker.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    var expanded by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

    // Separate attendance records from payment records
    val attendanceEntries = workerState.allEntries
        .filter { it.paymentMode == null }
        .sortedByDescending { it.date }
    val paymentEntries = workerState.allEntries
        .filter { it.paymentMode != null }
        .sortedByDescending { it.date }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp), spotColor = Color.LightGray.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
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
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = PrimaryOrange
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = BackgroundCream, thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            // Stats row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WorkerStat("Daily Wage", "₹${worker.dailyWage.toInt()}")
                WorkerStat("Earned", "₹${workerState.totalEarned.toInt()}")
                WorkerStat("Paid", "₹${workerState.totalPaid.toInt()}", AttendanceAbsent)
                WorkerStat("Balance", "₹${workerState.balance.toInt()}", PrimaryOrange)
            }

            if (expanded) {
                Spacer(Modifier.height(16.dp))

                // Attendance History
                if (attendanceEntries.isNotEmpty()) {
                    Text("Attendance History", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                    Spacer(Modifier.height(8.dp))
                    attendanceEntries.forEach { entry ->
                        val (statusLabel, statusColor) = when (entry.attendance) {
                            1.0 -> "Present" to AttendancePresent
                            0.5 -> "Half Day" to AttendanceHalfDay
                            else -> "Absent" to AttendanceAbsent
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextGray, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(sdf.format(Date(entry.date)), fontSize = 13.sp, color = TextDark)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(statusColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                            }
                        }
                    }
                } else {
                    Text("No attendance records yet.", fontSize = 12.sp, color = TextGray)
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = BackgroundCream, thickness = 1.dp)
                Spacer(Modifier.height(12.dp))

                // Payment History
                Text("Payment History", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                Spacer(Modifier.height(8.dp))
                if (paymentEntries.isNotEmpty()) {
                    paymentEntries.forEach { entry ->
                        val modeIcon = when (entry.paymentMode) {
                            "UPI" -> Icons.Default.PhoneAndroid
                            "Scanner" -> Icons.Default.QrCodeScanner
                            else -> Icons.Default.AttachMoney
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(modeIcon, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Text(sdf.format(Date(entry.date)), fontSize = 13.sp, color = TextDark)
                                    Text(entry.paymentMode ?: "", fontSize = 11.sp, color = TextGray)
                                }
                            }
                            Text("₹${entry.advance.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AttendanceAbsent)
                        }
                    }
                } else {
                    Text("No payments recorded yet.", fontSize = 12.sp, color = TextGray)
                }
            }
        }
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
