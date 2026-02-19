package com.jazwinn.fitnesstracker.ui.screens

import android.Manifest
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.jazwinn.fitnesstracker.domain.model.ExerciseType
import com.jazwinn.fitnesstracker.ui.camera.YoloPoseAnalyzer
import com.jazwinn.fitnesstracker.ui.camera.YoloPoseOverlay
import com.jazwinn.fitnesstracker.ui.viewmodel.ExerciseViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ExerciseScreen(
    exerciseType: String? = null,
    onClose: () -> Unit = {},
    onNavigateHome: () -> Unit = {},
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    LaunchedEffect(exerciseType) {
        if (exerciseType != null) {
            try {
                val type = ExerciseType.valueOf(exerciseType)
                viewModel.setExerciseType(type)
            } catch (e: IllegalArgumentException) {
                Log.e("ExerciseScreen", "Invalid exercise type: $exerciseType")
            }
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

    // Check for camera availability to avoid startup crash on devices without back camera (e.g. some emulators)
    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        if (!cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) && cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            Log.i("ExerciseScreen", "Defaulting to FRONT camera (Back camera not found)")
        }
    }
    
    // Create YOLOv8 analyzer once
    val analyzer = remember {
        Log.d("ExerciseScreen", "Creating YoloPoseAnalyzer...")
        YoloPoseAnalyzer(context) { results ->
            if (results.isNotEmpty()) {
                Log.v("ExerciseScreen", "Received ${results.size} pose results from analyzer")
            }
            viewModel.updatePose(results)
        }
    }

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    DisposableEffect(Unit) {
        onDispose {
            analyzer.close()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        SideEffect { Log.d("ExerciseScreen", "Camera permission GRANTED, rendering camera preview") }
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Use key() to force AndroidView recreation when camera changes
            key(cameraSelector) {
                // Camera Preview
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                    },
                    update = { previewView ->
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            
                            val preview = Preview.Builder().build().apply {
                                setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(android.util.Size(640, 480))
                                .setTargetRotation(previewView.display.rotation)
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .apply {
                                    setAnalyzer(
                                        Executors.newSingleThreadExecutor(),
                                        analyzer
                                    )
                                }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Log.e("ExerciseScreen", "Use case binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                )
            }

            // YOLOv8 Pose Skeleton Overlay
            val detectedPoses = uiState.detectedPoses
            if (uiState.showOverlay && detectedPoses.isNotEmpty()) {
                YoloPoseOverlay(results = detectedPoses)
            }
            
            // Detection indicator - Green = detecting, Red = not detecting
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(20.dp)
                    .background(
                        color = if (detectedPoses.isNotEmpty()) Color.Green else Color.Red,
                        shape = CircleShape
                    )
            )

            // Overlay UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { expanded = true }) {
                            Text(uiState.selectedExercise.getDisplayName())
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            ExerciseType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.getDisplayName()) },
                                    onClick = {
                                        viewModel.setExerciseType(type)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Toggle Overlay Switch
                        Text("Skeleton", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = uiState.showOverlay,
                            onCheckedChange = { viewModel.toggleOverlay() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(onClick = {
                            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Home Button
                        IconButton(onClick = onNavigateHome) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                tint = Color.White
                            )
                        }

                        // Close Button
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }
                
                // Bottom Feedback
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${uiState.repCount}",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White
                    )
                    Text(
                        text = "REPS",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.feedbackMessage,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (uiState.feedbackMessage.contains("Good") || uiState.feedbackMessage.contains("detected"))
                            Color.Green else Color.Yellow
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Start/Stop Button
                        Button(
                            onClick = {
                                if (uiState.isTracking) {
                                    viewModel.stopTracking()
                                } else {
                                    viewModel.startTracking()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isTracking)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(if (uiState.isTracking) "Stop" else "Start")
                        }
                        
                        // Reset Button (only when not tracking)
                        if (!uiState.isTracking && uiState.repCount > 0) {
                            Button(
                                onClick = { viewModel.reset() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("Save & Reset")
                            }
                            
                            // Restart without saving
                            Button(
                                onClick = { viewModel.restartWithoutSaving() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Restart")
                            }
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Camera permission required for exercise detection.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Grant Permission")
            }
        }
    }
}
