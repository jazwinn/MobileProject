package com.jazwinn.fitnesstracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jazwinn.fitnesstracker.data.local.dao.UserProfileDao
import com.jazwinn.fitnesstracker.data.local.entity.UserProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileDao: UserProfileDao
) : ViewModel() {

    val userProfile: StateFlow<UserProfileEntity?> = userProfileDao.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateProfile(name: String, height: Float, weight: Float, age: Int, stepGoal: Int) {
        viewModelScope.launch {
            userProfileDao.insertOrUpdateProfile(
                UserProfileEntity(
                    id = 0, // Always 0 for single user
                    name = name,
                    heightCm = height,
                    weightKg = weight,
                    age = age,
                    dailyStepGoal = stepGoal
                )
            )
        }
    }
}
