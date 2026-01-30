package com.jazwinn.fitnesstracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jazwinn.fitnesstracker.data.local.FitnessDatabase
import com.jazwinn.fitnesstracker.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val areNotificationsEnabled: Boolean = true,
    val isUnitMetric: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val database: FitnessDatabase
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.isDarkMode,
        settingsRepository.areNotificationsEnabled,
        settingsRepository.isUnitMetric
    ) { darkMode, notifications, metric ->
        SettingsUiState(
            isDarkMode = darkMode,
            areNotificationsEnabled = notifications,
            isUnitMetric = metric
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkMode(enabled)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }

    fun toggleUnitMetric(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUnitMetric(enabled)
        }
    }

    fun resetAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearAllTables()
            // Re-populate default data if necessary, or just leave empty
        }
    }
}
