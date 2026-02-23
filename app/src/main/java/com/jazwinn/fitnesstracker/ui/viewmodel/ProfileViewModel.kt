package com.jazwinn.fitnesstracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jazwinn.fitnesstracker.data.local.dao.BmiHistoryDao
import com.jazwinn.fitnesstracker.data.local.dao.UserProfileDao
import com.jazwinn.fitnesstracker.data.local.entity.BmiHistoryEntity
import com.jazwinn.fitnesstracker.data.local.entity.UserProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val bmiHistoryDao: BmiHistoryDao
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
                            stepGoal = profile.dailyStepGoal.toString(),
                            bmi = calculateBmi(profile.heightCm.toString(), profile.weightKg.toString())
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
            _uiState.update { 
                val h = newHeight.toFloatOrNull() ?: 0f
                val error = if (newHeight.isNotEmpty() && h <= 0f) "Must be greater than 0" else null
                val newState = it.copy(height = newHeight, heightError = error)
                newState.copy(bmi = calculateBmi(newState.height, newState.weight))
            }
        }
    }

    fun updateWeight(newWeight: String) {
         if (newWeight.all { it.isDigit() || it == '.' }) {
            _uiState.update { 
                val w = newWeight.toFloatOrNull() ?: 0f
                val error = if (newWeight.isNotEmpty() && w <= 0f) "Must be greater than 0" else null
                val newState = it.copy(weight = newWeight, weightError = error)
                newState.copy(bmi = calculateBmi(newState.height, newState.weight))
            }
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
            val h = currentState.height.toFloatOrNull() ?: 170f
            val w = currentState.weight.toFloatOrNull() ?: 70f
            
            val profile = UserProfileEntity(
                id = 0,
                name = currentState.name,
                heightCm = h,
                weightKg = w,
                age = currentState.age.toIntOrNull() ?: 25,
                dailyStepGoal = currentState.stepGoal.toIntOrNull() ?: 10000
            )
            userProfileDao.insertOrUpdateProfile(profile)
            
            // Save to BMI History
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val bmiValue = currentState.bmi.value.toFloatOrNull() ?: 0f
            if (bmiValue > 0) {
                val bmiRecord = BmiHistoryEntity(
                    date = dateStr,
                    bmiValue = bmiValue,
                    weightKg = w,
                    heightCm = h
                )
                bmiHistoryDao.insertBmiRecord(bmiRecord)
            }
            
            _uiState.update { it.copy(isSaved = true) }
        }
    }
    
    fun resetSavedFlag() {
        _uiState.update { it.copy(isSaved = false) }
    }
    
    private fun calculateBmi(heightCm: String, weightKg: String): BmiInfo {
        val h = heightCm.toFloatOrNull() ?: 0f
        val w = weightKg.toFloatOrNull() ?: 0f
        
        if (h <= 0f || w <= 0f) return BmiInfo()
        
        val heightMeters = h / 100f
        val bmiValue = w / (heightMeters * heightMeters)
        
        val category = when {
            bmiValue < 18.5 -> "Underweight"
            bmiValue < 25.0 -> "Normal"
            bmiValue < 30.0 -> "Overweight"
            else -> "Obese"
        }
        
        val colorHex = when {
            bmiValue < 18.5 -> 0xFF3498DB // Blue
            bmiValue < 25.0 -> 0xFF2ECC71 // Green
            bmiValue < 30.0 -> 0xFFF1C40F // Yellow
            else -> 0xFFE74C3C // Red
        }
        
        return BmiInfo(
            value = "%.1f".format(Locale.getDefault(), bmiValue),
            category = category,
            color = colorHex.toLong()
        )
    }
}

data class ProfileUiState(
    val name: String = "",
    val height: String = "",
    val weight: String = "",
    val age: String = "",
    val stepGoal: String = "",
    val isSaved: Boolean = false,
    val bmi: BmiInfo = BmiInfo(),
    val heightError: String? = null,
    val weightError: String? = null
)

data class BmiInfo(
    val value: String = "--",
    val category: String = "Unknown",
    val color: Long = 0xFF95A5A6 // Gray
)
