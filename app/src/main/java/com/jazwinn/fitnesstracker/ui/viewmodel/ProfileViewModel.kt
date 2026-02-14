package com.jazwinn.fitnesstracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jazwinn.fitnesstracker.data.local.dao.UserProfileDao
import com.jazwinn.fitnesstracker.data.local.entity.UserProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileDao: UserProfileDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            userProfileDao.getUserProfile().collect { profile ->
                if (profile != null) {
                    _uiState.update { 
                        it.copy(
                            name = profile.name,
                            height = profile.heightCm.toString(),
                            weight = profile.weightKg.toString(),
                            age = profile.age.toString(),
                            stepGoal = profile.dailyStepGoal.toString()
                        )
                    }
                } else {
                    // Create default profile if none exists
                    val defaultProfile = UserProfileEntity()
                    userProfileDao.insertOrUpdateProfile(defaultProfile)
                }
            }
        }
    }

    fun updateName(newName: String) {
        _uiState.update { it.copy(name = newName) }
    }

    fun updateHeight(newHeight: String) {
        if (newHeight.all { it.isDigit() || it == '.' }) {
            _uiState.update { it.copy(height = newHeight) }
        }
    }

    fun updateWeight(newWeight: String) {
         if (newWeight.all { it.isDigit() || it == '.' }) {
            _uiState.update { it.copy(weight = newWeight) }
        }
    }

    fun updateAge(newAge: String) {
         if (newAge.all { it.isDigit() }) {
            _uiState.update { it.copy(age = newAge) }
        }
    }
    
    fun updateStepGoal(newGoal: String) {
         if (newGoal.all { it.isDigit() }) {
            _uiState.update { it.copy(stepGoal = newGoal) }
        }
    }

    fun saveProfile() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val profile = UserProfileEntity(
                id = 0,
                name = currentState.name,
                heightCm = currentState.height.toFloatOrNull() ?: 170f,
                weightKg = currentState.weight.toFloatOrNull() ?: 70f,
                age = currentState.age.toIntOrNull() ?: 25,
                dailyStepGoal = currentState.stepGoal.toIntOrNull() ?: 10000
            )
            userProfileDao.insertOrUpdateProfile(profile)
            _uiState.update { it.copy(isSaved = true) }
            // Reset saved flag after a moment? Or handle in UI
        }
    }
    
    fun resetSavedFlag() {
        _uiState.update { it.copy(isSaved = false) }
    }
}

data class ProfileUiState(
    val name: String = "",
    val height: String = "",
    val weight: String = "",
    val age: String = "",
    val stepGoal: String = "",
    val isSaved: Boolean = false
)
