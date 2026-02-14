package com.jazwinn.fitnesstracker.ui.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jazwinn.fitnesstracker.data.local.dao.StepsDao
import com.jazwinn.fitnesstracker.data.local.dao.WorkoutDao
import com.jazwinn.fitnesstracker.data.local.entity.WorkoutEntity
import com.jazwinn.fitnesstracker.ui.screens.HistoryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val stepsDao: StepsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState = _uiState.asStateFlow()

    // Formatters
    private val entryDateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    // State for navigation
    private var currentCalendar = Calendar.getInstance()

    init {
        updateDateState()
        loadData()
    }

    private fun updateDateState() {
        _uiState.update { 
            it.copy(
                selectedDate = currentCalendar.time,
                currentMonthYear = monthYearFormat.format(currentCalendar.time)
            )
        }
    }

    fun previousMonth() {
        currentCalendar.add(Calendar.MONTH, -1)
        // Reset to first day of month to avoid issues
        currentCalendar.set(Calendar.DAY_OF_MONTH, 1) 
        updateDateState()
        // We might want to clear selected day specific filter when changing months
        // or select the first day? Let's keep it simple and just show the month view first
        _uiState.update { it.copy(filteredHistory = emptyList(), selectedDate = null) }
    }

    fun nextMonth() {
        val today = Calendar.getInstance()
        if (currentCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) && 
            currentCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
            return // Don't go into future
        }
        currentCalendar.add(Calendar.MONTH, 1)
        currentCalendar.set(Calendar.DAY_OF_MONTH, 1)
        updateDateState()
        _uiState.update { it.copy(filteredHistory = emptyList(), selectedDate = null) }
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                workoutDao.getAllWorkouts(),
                stepsDao.getAllStepsHistory()
            ) { workouts, stepsEntityList ->
                // 1. Identify all dates with activity
                val activeDatesSet = mutableSetOf<String>()
                
                // 2. Map Workouts to History Entries
                val workoutEntries = workouts.map { workout ->
                    val date = Date(workout.timestamp)
                    val dateStr = dayFormat.format(date)
                    activeDatesSet.add(dateStr)
                    
                    val isRun = workout.type == "OUTDOOR_RUN"
                    val displayValue = if (isRun) {
                        val dist = workout.distanceKm?.let { String.format("%.2f km", it) } ?: "0 km"
                        val paceStr = workout.paceMinPerKm?.let { String.format("%.1f min/km", it) } ?: ""
                        if (paceStr.isNotEmpty()) "$dist • $paceStr" else dist
                    } else {
                        "${workout.reps} reps"
                    }
                    
                    HistoryEntry(
                        title = formatWorkoutType(workout.type),
                        date = entryDateFormat.format(date),
                        value = displayValue,
                        icon = if (isRun) Icons.Default.DirectionsRun else Icons.Default.FitnessCenter,
                        dayOfMonth = getDayOfMonth(date),
                        timestamp = workout.timestamp,
                        dateStr = dateStr
                    )
                }

                // 3. Map Steps to History Entries
                val stepEntries = stepsEntityList.mapNotNull { stepEntity ->
                    try {
                        val date = dayFormat.parse(stepEntity.date)
                        if (date != null) {
                            activeDatesSet.add(stepEntity.date)
                            HistoryEntry(
                                title = "Daily Steps",
                                date = stepEntity.date, 
                                value = "${stepEntity.stepCount} steps",
                                icon = Icons.Default.DirectionsRun,
                                dayOfMonth = getDayOfMonth(date),
                                timestamp = date.time,
                                dateStr = stepEntity.date
                            )
                        } else null
                    } catch (e: Exception) { null }
                }

                // Combine and store
                val allEntries = (workoutEntries + stepEntries).sortedByDescending { it.timestamp }
                
                Triple(allEntries, activeDatesSet, workouts)
            }.collect { (allEntries, activeDates, _) ->
                val selectedDateStr = _uiState.value.selectedDate?.let { dayFormat.format(it) }
                    ?: dayFormat.format(Calendar.getInstance().time)
                val filtered = allEntries.filter { it.dateStr == selectedDateStr }
                _uiState.update { 
                    it.copy(
                        fullHistory = allEntries,
                        activeDates = activeDates,
                        filteredHistory = filtered
                    )
                }
            }
        }
    }

    fun selectDate(dayOfMonth: Int) {
        val tempCal = currentCalendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        val newDate = tempCal.time
        val newDateStr = dayFormat.format(newDate)
        
        _uiState.update { state ->
            val filtered = state.fullHistory.filter { it.dateStr == newDateStr }
            state.copy(
                selectedDate = newDate,
                filteredHistory = filtered
            )
        }
    }

    private fun formatWorkoutType(type: String): String {
        return type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }
    
    private fun getDayOfMonth(date: Date): Int {
        val cal = Calendar.getInstance()
        cal.time = date
        return cal.get(Calendar.DAY_OF_MONTH)
    }
}

data class HistoryUiState(
    val fullHistory: List<HistoryEntry> = emptyList(),
    val filteredHistory: List<HistoryEntry> = emptyList(),
    val activeDates: Set<String> = emptySet(), // Set of "yyyy-MM-dd"
    val selectedDate: Date? = null,
    val currentMonthYear: String = ""
)
