package com.jazwinn.fitnesstracker.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jazwinn.fitnesstracker.data.local.dao.WorkoutDao
import com.jazwinn.fitnesstracker.data.local.entity.WorkoutEntity
import com.jazwinn.fitnesstracker.service.RunningService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RunningViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sensorManager: com.jazwinn.fitnesstracker.service.RunningSensorManager,
    private val workoutDao: WorkoutDao
) : ViewModel() {

    private val _cadence = MutableStateFlow(0)
    val cadence = _cadence.asStateFlow()

    private val _showSaveDialog = MutableStateFlow(false)
    val showSaveDialog = _showSaveDialog.asStateFlow()

    // Snapshot values captured when user stops run
    private var snapshotDistanceKm = 0f
    private var snapshotPaceMinPerKm = 0f
    private var snapshotDurationSeconds = 0L

    init {
        viewModelScope.launch {
            var bucketStart = System.currentTimeMillis()
            var stepCount = 0
            
            sensorManager.getStepUpdates().collect {
                val now = System.currentTimeMillis()
                if (now - bucketStart > 10000) { // Update every 10s
                   val spm = (stepCount * 6).toInt() // steps in 10s * 6 = steps per min
                   _cadence.value = spm
                   stepCount = 0
                   bucketStart = now
                }
                stepCount++
            }
        }
    }

    val isRunning = RunningService.isRunning
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val locationUpdates = RunningService.locationUpdates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentPace = RunningService.currentPace
        .map { pace -> 
            if (pace > 0) String.format("%.2f min/km", pace) else "--:--"
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "--:--")
    
    val paceHistory = RunningService.paceHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val elapsedTime = RunningService.elapsedTime
        .map { millis ->
            val seconds = (millis / 1000) % 60
            val minutes = (millis / (1000 * 60)) % 60
            val hours = (millis / (1000 * 60 * 60))
            if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, seconds)
            else String.format("%02d:%02d", minutes, seconds)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "00:00")

    val distanceKm = RunningService.locationUpdates
        .map { locations ->
            var dist = 0f
            for (i in 0 until locations.size - 1) {
                dist += locations[i].distanceTo(locations[i+1])
            }
            dist / 1000f
        }
        .map { String.format("%.2f km", it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0.00 km")

    // For Momentum Indicator
    val currentSpeed = RunningService.locationUpdates
        .map { it.lastOrNull()?.speed ?: 0f } // m/s
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val avgSpeed = RunningService.locationUpdates
        .map { locations ->
            if (locations.isEmpty()) 0f
            else {
                val distMeters = locations.zipWithNext { a, b -> a.distanceTo(b) }.sum()
                val durationSeconds = RunningService.elapsedTime.value / 1000f
                if (durationSeconds > 0) distMeters / durationSeconds else 0f
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // For Progress Ring (Goal: 5km hardcoded for demo)
    val runProgress = distanceKm.map { 
        val dist = it.replace(" km", "").toFloatOrNull() ?: 0f
        (dist / 5f).coerceIn(0f, 1f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    fun toggleRun() {
        if (isRunning.value) {
            // Snapshot current values before stopping
            val locations = RunningService.locationUpdates.value
            var dist = 0f
            for (i in 0 until locations.size - 1) {
                dist += locations[i].distanceTo(locations[i + 1])
            }
            snapshotDistanceKm = dist / 1000f
            snapshotPaceMinPerKm = RunningService.currentPace.value
            snapshotDurationSeconds = RunningService.elapsedTime.value / 1000

            // Stop the service
            stopRunService()

            // Show save dialog
            _showSaveDialog.value = true
        } else {
            val intent = Intent(context, RunningService::class.java)
            intent.action = RunningService.ACTION_START
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    fun saveRun() {
        viewModelScope.launch {
            workoutDao.insertWorkout(
                WorkoutEntity(
                    timestamp = System.currentTimeMillis(),
                    type = "OUTDOOR_RUN",
                    durationSeconds = snapshotDurationSeconds,
                    distanceKm = snapshotDistanceKm,
                    paceMinPerKm = if (snapshotPaceMinPerKm > 0) snapshotPaceMinPerKm else null
                )
            )
            _showSaveDialog.value = false
            RunningService.reset()
        }
    }

    fun discardRun() {
        _showSaveDialog.value = false
        RunningService.reset()
    }

    private fun stopRunService() {
        val intent = Intent(context, RunningService::class.java)
        intent.action = RunningService.ACTION_STOP
        context.startService(intent)
    }
}
