package com.jazwinn.fitnesstracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val workoutDao: com.jazwinn.fitnesstracker.data.local.dao.WorkoutDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseUiState())
    val uiState = _uiState.asStateFlow()
    
    private var repCounter = RepCounter(ExerciseType.PUSH_UP) // Default

    fun setExerciseType(type: ExerciseType) {
        _uiState.update { it.copy(selectedExercise = type) }
        repCounter = RepCounter(type)
        reset()
    }
    
    fun updateReps(count: Int, feedback: String) {
        _uiState.update { 
            it.copy(
                repCount = count,
                feedbackMessage = feedback
            ) 
        }
    }
    
    fun reset() {
        val currentState = _uiState.value
        if (currentState.repCount > 0) {
            saveSession(currentState)
        }
        repCounter.reset()
        _uiState.update { it.copy(repCount = 0, feedbackMessage = "Ready") }
    }
    
    private fun saveSession(state: ExerciseUiState) {
        viewModelScope.launch {
            workoutDao.insertWorkout(
                com.jazwinn.fitnesstracker.data.local.entity.WorkoutEntity(
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
    val isExercising: Boolean = false
)
