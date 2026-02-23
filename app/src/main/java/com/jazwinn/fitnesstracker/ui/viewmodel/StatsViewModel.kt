package com.jazwinn.fitnesstracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jazwinn.fitnesstracker.data.local.dao.BmiHistoryDao
import com.jazwinn.fitnesstracker.data.local.dao.StepsDao
import com.jazwinn.fitnesstracker.data.local.dao.WorkoutDao
import com.jazwinn.fitnesstracker.data.local.entity.BmiHistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val stepsDao: StepsDao,
    private val workoutDao: WorkoutDao,
    private val bmiHistoryDao: BmiHistoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState = _uiState.asStateFlow()

    private val calendar = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.SUNDAY
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    }

    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

    init {
        loadData()
    }

    private fun loadData() {
        updateDateRange()
        
        viewModelScope.launch {
            combine(
                stepsDao.getAllStepsHistory(),
                workoutDao.getAllWorkouts(),
                bmiHistoryDao.getAllBmiHistory()
            ) { steps, workouts, bmiHistory ->
                val statsMap = mutableMapOf<String, DailyStat>()
                val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                
                steps.forEach { step ->
                    statsMap[step.date] = DailyStat(steps = step.stepCount)
                }
                
                workouts.forEach { workout ->
                    val date = Date(workout.timestamp)
                    val dateStr = isoFormat.format(date)
                    val current = statsMap.getOrDefault(dateStr, DailyStat())
                    statsMap[dateStr] = current.copy(workoutReps = current.workoutReps + workout.reps)
                }
                
                Triple(statsMap, bmiHistory, steps)
            }.collect { (fullStatsMap, bmiHistory, allSteps) ->
                val weekStats = getWeekStats(fullStatsMap)
                
                val avgSteps = if (weekStats.isNotEmpty()) weekStats.map { it.steps }.average().toInt() else 0
                val activeDays = weekStats.count { it.steps > 0 || it.workoutReps > 0 }
                
                val maxSteps = weekStats.maxOfOrNull { it.steps }?.takeIf { it > 0 } ?: 10000
                val chartPoints = weekStats.map { it.steps.toFloat() / maxSteps.toFloat() }

                // BMI Chart Points (Recent 7 entries)
                val recentBmi = bmiHistory.takeLast(7)
                val minBmi = recentBmi.minOfOrNull { it.bmiValue } ?: 0f
                val maxBmi = recentBmi.maxOfOrNull { it.bmiValue } ?: 1f
                val bmiChartPoints = if (maxBmi > minBmi) {
                    recentBmi.map { (it.bmiValue - minBmi) / (maxBmi - minBmi) }
                } else {
                    recentBmi.map { 0.5f }
                }

                _uiState.update { 
                    it.copy(
                        weekStats = weekStats,
                        avgSteps = avgSteps,
                        activeStreak = activeDays,
                        chartPoints = chartPoints,
                        bmiChartPoints = bmiChartPoints,
                        bmiHistory = recentBmi,
                        fullStatsMap = fullStatsMap
                    )
                }
            }
        }
    }
    
    private fun getWeekStats(fullData: Map<String, DailyStat>): List<DailyStat> {
        val stats = mutableListOf<DailyStat>()
        val tempCal = calendar.clone() as Calendar
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        for (i in 0..6) {
            val dateStr = isoFormat.format(tempCal.time)
            stats.add(fullData.getOrDefault(dateStr, DailyStat()))
            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return stats
    }
    
    private fun updateDateRange() {
        val tempCal = calendar.clone() as Calendar
        val start = dateFormat.format(tempCal.time)
        tempCal.add(Calendar.DAY_OF_YEAR, 6)
        val end = dateFormat.format(tempCal.time)
        
        _uiState.update { 
            it.copy(dateRange = "$start - $end")
        }
    }
    
    fun previousWeek() {
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        updateDateRange()
        refreshWeekView()
    }
    
    fun nextWeek() {
        val currentWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)
        if (calendar.get(Calendar.WEEK_OF_YEAR) == currentWeek && 
            calendar.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)) {
            return
        }
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        updateDateRange()
        refreshWeekView()
    }
    
    private fun refreshWeekView() {
        val fullData = _uiState.value.fullStatsMap
        val weekStats = getWeekStats(fullData)
        
        val avgSteps = if (weekStats.isNotEmpty()) weekStats.map { it.steps }.average().toInt() else 0
        val activeDays = weekStats.count { it.steps > 0 || it.workoutReps > 0 }
        
        val maxSteps = weekStats.maxOfOrNull { it.steps }?.takeIf { it > 0 } ?: 10000
        val chartPoints = weekStats.map { it.steps.toFloat() / maxSteps.toFloat() }

        _uiState.update {
            it.copy(
                weekStats = weekStats,
                chartPoints = chartPoints,
                avgSteps = avgSteps,
                activeStreak = activeDays
            )
        }
    }
}

data class DailyStat(
    val steps: Long = 0,
    val workoutReps: Int = 0
)

data class StatsUiState(
    val dateRange: String = "",
    val weekStats: List<DailyStat> = emptyList(),
    val chartPoints: List<Float> = emptyList(),
    val bmiChartPoints: List<Float> = emptyList(),
    val bmiHistory: List<BmiHistoryEntity> = emptyList(),
    val avgSteps: Int = 0,
    val activeStreak: Int = 0,
    val fullStatsMap: Map<String, DailyStat> = emptyMap()
)
