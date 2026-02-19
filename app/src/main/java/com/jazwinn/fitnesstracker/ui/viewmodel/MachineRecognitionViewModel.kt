package com.jazwinn.fitnesstracker.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jazwinn.fitnesstracker.domain.repository.MachineGuideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MachineRecognitionUiState {
    object Idle : MachineRecognitionUiState
    object Analyzing : MachineRecognitionUiState
    data class Recognized(val machineName: String, val bitmap: Bitmap) : MachineRecognitionUiState
    data class LoadingGuide(val machineName: String) : MachineRecognitionUiState
    data class GuideLoaded(val machineName: String, val guideText: String) : MachineRecognitionUiState
    data class Error(val message: String) : MachineRecognitionUiState
}

@HiltViewModel
class MachineRecognitionViewModel @Inject constructor(
    private val repository: MachineGuideRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MachineRecognitionUiState>(MachineRecognitionUiState.Idle)
    val uiState: StateFlow<MachineRecognitionUiState> = _uiState.asStateFlow()

    fun analyzeImage(bitmap: Bitmap) {
        _uiState.value = MachineRecognitionUiState.Analyzing
        viewModelScope.launch {
            repository.identifyMachine(bitmap)
                .onSuccess { machineName ->
                    _uiState.value = MachineRecognitionUiState.Recognized(machineName, bitmap)
                }
                .onFailure { e ->
                    _uiState.value = MachineRecognitionUiState.Error("Failed to identify machine: ${e.message}")
                }
        }
    }

    fun fetchGuide(machineName: String) {
        _uiState.value = MachineRecognitionUiState.LoadingGuide(machineName)
        viewModelScope.launch {
            repository.getGuideForMachine(machineName)
                .onSuccess { guideText ->
                    _uiState.value = MachineRecognitionUiState.GuideLoaded(machineName, guideText)
                }
                .onFailure { e ->
                    // Keep the recognised state if guide fails? Or Error?
                    // Error is safer so user knows.
                    _uiState.value = MachineRecognitionUiState.Error("Failed to load guide: ${e.message}")
                }
        }
    }

    fun reset() {
        _uiState.value = MachineRecognitionUiState.Idle
    }
}
