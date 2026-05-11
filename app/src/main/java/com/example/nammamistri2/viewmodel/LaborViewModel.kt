package com.example.nammamistri2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nammamistri2.data.LaborEntry
import com.example.nammamistri2.data.Worker
import com.example.nammamistri2.repository.NammaMistriRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class TeamSummary(
    val totalWorkers: Int = 0,
    val totalEarnings: Double = 0.0,
    val totalPaid: Double = 0.0,
    val totalBalance: Double = 0.0
)

data class WorkerState(
    val worker: Worker,
    val todayEntry: LaborEntry? = null,
    val totalEarned: Double = 0.0,
    val totalPaid: Double = 0.0,
    val balance: Double = 0.0,
    val allEntries: List<LaborEntry> = emptyList()
)

class LaborViewModel(private val repository: NammaMistriRepository) : ViewModel() {

    private val currentSiteId: Long = 1 // Default site
    
    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    val workers = repository.getWorkersBySite(currentSiteId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val workerStates: Flow<List<WorkerState>> = combine(workers, _selectedDate) { workerList, date ->
        workerList to date
    }.flatMapLatest { (workerList, date) ->
        if (workerList.isEmpty()) flowOf(emptyList())
        else {
            val flows = workerList.map { worker ->
                combine(
                    repository.getEntriesByWorker(worker.id),
                    repository.getTotalDaysWorkedFlow(worker.id),
                    repository.getTotalAdvanceFlow(worker.id)
                ) { entries, days, paid ->
                    val todayEntry = entries.find { isSameDay(it.date, date) }
                    val earnings = days * worker.dailyWage
                    WorkerState(
                        worker = worker,
                        todayEntry = todayEntry,
                        totalEarned = earnings,
                        totalPaid = paid,
                        balance = earnings - paid,
                        allEntries = entries
                    )
                }
            }
            combine(flows) { it.toList() }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val teamSummary: Flow<TeamSummary> = workers.flatMapLatest { workerList ->
        if (workerList.isEmpty()) {
            flowOf(TeamSummary())
        } else {
            val individualFlows = workerList.map { worker ->
                combine(
                    repository.getTotalDaysWorkedFlow(worker.id),
                    repository.getTotalAdvanceFlow(worker.id)
                ) { days, advance ->
                    val earnings = days * worker.dailyWage
                    earnings to advance
                }
            }
            combine(individualFlows) { pairs ->
                var earningsSum = 0.0
                var paidSum = 0.0
                pairs.forEach { (earnings, paid) ->
                    earningsSum += earnings
                    paidSum += paid
                }
                TeamSummary(
                    totalWorkers = workerList.size,
                    totalEarnings = earningsSum,
                    totalPaid = paidSum,
                    totalBalance = earningsSum - paidSum
                )
            }
        }
    }

    fun selectDate(date: Long) {
        _selectedDate.value = date
    }

    fun addWorker(name: String, dailyWage: Double, role: String) {
        viewModelScope.launch {
            repository.insertWorker(Worker(name = name, role = role, dailyWage = dailyWage, siteId = currentSiteId))
        }
    }

    fun markAttendance(workerId: Long, attendance: Double) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val entries = repository.getEntriesByWorker(workerId).first()
            val existing = entries.find { isSameDay(it.date, date) }
            
            if (existing != null) {
                repository.insertLaborEntry(existing.copy(attendance = attendance))
            } else {
                repository.insertLaborEntry(LaborEntry(workerId = workerId, date = date, attendance = attendance, advance = 0.0))
            }
        }
    }

    fun addPayment(workerId: Long, amount: Double, mode: String, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.insertLaborEntry(
                LaborEntry(
                    workerId = workerId,
                    date = date,
                    attendance = 0.0, // payment-only entry
                    advance = amount,
                    paymentMode = mode
                )
            )
        }
    }

    private fun isSameDay(date1: Long, date2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun deleteWorker(workerId: Long) {
        viewModelScope.launch {
            repository.deleteWorker(workerId)
        }
    }
}
