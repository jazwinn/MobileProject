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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.common.util.concurrent.ListenableFuture
import com.jazwinn.fitnesstracker.domain.model.ExerciseType
import com.jazwinn.fitnesstracker.ui.camera.MediaPipePoseAnalyzer
import com.jazwinn.fitnesstracker.ui.camera.MediaPipePoseOverlay
import com.jazwinn.fitnesstracker.ui.viewmodel.ExerciseViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ExerciseScreen(
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    
    // Create analyzer once, not on every update
    val analyzer = remember {
        MediaPipePoseAnalyzer(context) { result ->
            viewModel.updatePose(result)
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
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Use key() to force AndroidView recreation when camera changes
            key(cameraSelector) {
                // Camera Preview with key to trigger recreation on camera change
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

                            // Reuse the same analyzer instance
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(android.util.Size(1280, 720)) // 720p for better quality
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

            // MediaPipe Pose Skeleton Overlay
            val detectedPoseResult = uiState.detectedPose
            if (detectedPoseResult != null) {
                MediaPipePoseOverlay(result = detectedPoseResult)
            }
            
            // Simple detection indicator - Green circle = detecting, red = not detecting
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(20.dp)
                    .background(
                        color = if (detectedPoseResult != null && detectedPoseResult.landmarks().isNotEmpty()) {
                            Color.Green
                        } else {
                            Color.Red
                        },
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
