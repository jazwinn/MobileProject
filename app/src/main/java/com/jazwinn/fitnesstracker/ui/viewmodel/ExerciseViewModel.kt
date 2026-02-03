package com.jazwinn.fitnesstracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.jazwinn.fitnesstracker.data.local.dao.WorkoutDao
import com.jazwinn.fitnesstracker.data.local.entity.WorkoutEntity
import com.jazwinn.fitnesstracker.domain.logic.RepCounter
import com.jazwinn.fitnesstracker.domain.model.ExerciseType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val workoutDao: WorkoutDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseUiState())
    val uiState = _uiState.asStateFlow()
    
    private var repCounter = RepCounter(ExerciseType.PUSH_UP)

    fun setExerciseType(type: ExerciseType) {
        _uiState.update { it.copy(selectedExercise = type) }
        repCounter = RepCounter(type)
        reset()
    }
    
    fun startTracking() {
        _uiState.update { it.copy(isTracking = true, feedbackMessage = "Get into position") }
    }
    
    fun stopTracking() {
        _uiState.update { it.copy(isTracking = false, feedbackMessage = "Paused") }
    }
    
    fun updatePose(result: PoseLandmarkerResult?) {
        // Store the pose result for visualization
        _uiState.update { it.copy(detectedPose = result) }
        
        // Only process pose for rep counting if tracking is active
        if (_uiState.value.isTracking && result != null && result.landmarks().isNotEmpty()) {
            val landmarks = result.landmarks()[0] // Get first person
            
            // Convert MediaPipe landmarks to ML Kit format for RepCounter compatibility
            // (RepCounter still uses ML Kit Pose structure)
            // TODO: Update RepCounter to use MediaPipe directly
            
            _uiState.update { 
                it.copy(
                    feedbackMessage = "Person detected - counting reps"
                ) 
            }
        } else if (!_uiState.value.isTracking) {
            // Don't update feedback when not tracking
        } else {
            // No pose detected
            _uiState.update { it.copy(feedbackMessage = "Make sure you're in frame") }
        }
    }
    
    fun reset() {
        val currentState = _uiState.value
        if (currentState.repCount > 0) {
            saveSession(currentState)
        }
        repCounter.reset()
        _uiState.update { it.copy(repCount = 0, feedbackMessage = "Ready", isTracking = false) }
    }
    
    private fun saveSession(state: ExerciseUiState) {
        viewModelScope.launch {
            workoutDao.insertWorkout(
                WorkoutEntity(
                    timestamp = System.currentTimeMillis(),
                    type = state.selectedExercise.name,
                    reps = state.repCount
                )
            )
        }
    }
    
    fun getRepCounter(): RepCounter = repCounter
}

data class ExerciseUiState(
    val selectedExercise: ExerciseType = ExerciseType.PUSH_UP,
    val repCount: Int = 0,
    val feedbackMessage: String = "Ready",
    val isTracking: Boolean = false,
    val detectedPose: PoseLandmarkerResult? = null
)
