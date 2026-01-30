package com.jazwinn.fitnesstracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jazwinn.fitnesstracker.data.local.dao.StepsDao
import com.jazwinn.fitnesstracker.data.local.dao.WorkoutDao
import com.jazwinn.fitnesstracker.data.local.entity.DailyStepsEntity
import com.jazwinn.fitnesstracker.data.local.entity.WorkoutEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

import android.os.Build
import androidx.annotation.RequiresApi
import com.jazwinn.fitnesstracker.data.local.dao.UserProfileDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.Instant

@HiltViewModel
@RequiresApi(Build.VERSION_CODES.O)
class HistoryViewModel @Inject constructor(
    stepsDao: StepsDao,
    workoutDao: WorkoutDao,
    userProfileDao: UserProfileDao // In case we need profile stats later
) : ViewModel() {

    // Calendar State
    private val _selectedDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _currentMonth = MutableStateFlow<YearMonth>(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    // Raw Data
    private val allSteps = stepsDao.getAllStepsHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allWorkouts = workoutDao.getAllWorkouts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Aggregated Stats for Selected Date
    val statsForSelectedDate = combine(_selectedDate, allSteps, allWorkouts) { date, stepsList, workoutsList ->
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE) // yyyy-MM-dd
        
        val stepsEntity = stepsList.find { it.date == dateString }
        val steps = stepsEntity?.stepCount ?: 0L
        val calories = stepsEntity?.calories ?: 0f
        val distance = stepsEntity?.distance ?: 0f

        val daysWorkouts = workoutsList.filter { 
            val workoutDate = Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            workoutDate == date
        }

        DailyStats(
            date = date,
            steps = steps,
            calories = calories,
            distance = distance,
            workouts = daysWorkouts
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyStats(LocalDate.now(), 0L, 0f, 0f, emptyList()))

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun changeMonth(yearMonth: YearMonth) {
        _currentMonth.value = yearMonth
    }
}

data class DailyStats(
    val date: LocalDate,
    val steps: Long,
    val calories: Float,
    val distance: Float,
    val workouts: List<WorkoutEntity>
)
