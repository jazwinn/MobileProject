package com.jazwinn.fitnesstracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jazwinn.fitnesstracker.data.local.dao.WorkoutDao
import com.jazwinn.fitnesstracker.data.local.entity.WorkoutEntity
import com.jazwinn.fitnesstracker.domain.logic.RepCounter
import com.jazwinn.fitnesstracker.domain.model.ExerciseType
import com.jazwinn.fitnesstracker.ui.camera.PoseDetectionResult
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
    
    // Tone generator for audio feedback
    private val toneGenerator = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)

    fun setExerciseType(type: ExerciseType) {
        _uiState.update { it.copy(selectedExercise = type) }
        repCounter = RepCounter(type)
        reset()
    }
    
    fun toggleOverlay() {
        _uiState.update { it.copy(showOverlay = !it.showOverlay) }
    }
    
    fun startTracking() {
        _uiState.update { it.copy(isTracking = true, feedbackMessage = "Get into position") }
    }
    
    fun stopTracking() {
        _uiState.update { it.copy(isTracking = false, feedbackMessage = "Paused") }
    }
    
    /**
     * Process YOLOv8 pose detection results.
     * Updates the overlay visualization and, when tracking, feeds keypoints to the RepCounter.
     */
    fun updatePose(results: List<PoseDetectionResult>) {
        // Store results for skeleton overlay visualization
        _uiState.update { it.copy(detectedPoses = results) }
        
        // Only process for rep counting if tracking is active
        if (_uiState.value.isTracking && results.isNotEmpty()) {
            // Use the first (highest confidence) detected person
            val bestPose = results[0]
            
            // Feed keypoints into RepCounter for angle-based rep counting
            val previousReps = repCounter.repCount
            repCounter.processKeypoints(bestPose.keypoints)
            val currentReps = repCounter.repCount
            
            // Play beep if rep count increased
            if (currentReps > previousReps) {
                try {
                    toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP)
                } catch (e: Exception) {
                    // Ignore tone errors
                }
            }
            
            _uiState.update { 
                it.copy(
                    repCount = repCounter.repCount,
                    feedbackMessage = repCounter.feedback
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

    fun restartWithoutSaving() {
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
    
    override fun onCleared() {
        super.onCleared()
        toneGenerator.release()
    }
    
    fun getRepCounter(): RepCounter = repCounter
}

data class ExerciseUiState(
    val selectedExercise: ExerciseType = ExerciseType.PUSH_UP,
    val repCount: Int = 0,
    val feedbackMessage: String = "Ready",
    val isTracking: Boolean = false,
    val showOverlay: Boolean = true, // Default to true
    val detectedPoses: List<PoseDetectionResult> = emptyList()
)
