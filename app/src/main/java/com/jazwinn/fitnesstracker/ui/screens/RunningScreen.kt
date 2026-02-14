package com.jazwinn.fitnesstracker.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.jazwinn.fitnesstracker.ui.viewmodel.RunningViewModel
import com.jazwinn.fitnesstracker.ui.components.PaceGraph
import com.jazwinn.fitnesstracker.ui.components.MomentumIndicator
import com.jazwinn.fitnesstracker.ui.components.ProgressRing
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RunningScreen(
    onNavigateBack: () -> Unit,
    viewModel: RunningViewModel = hiltViewModel()
) {
    val isRunning by viewModel.isRunning.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    val distance by viewModel.distanceKm.collectAsState()
    val pace by viewModel.currentPace.collectAsState()
    val locationUpdates by viewModel.locationUpdates.collectAsState()
    val cadence by viewModel.cadence.collectAsState()
    val paceHistory by viewModel.paceHistory.collectAsState()
    val showSaveDialog by viewModel.showSaveDialog.collectAsState()

    // New metrics for visualization
    val currentSpeed by viewModel.currentSpeed.collectAsState()
    val avgSpeed by viewModel.avgSpeed.collectAsState()
    val runProgress by viewModel.runProgress.collectAsState()

    val context = LocalContext.current
    
    // Permission State
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        locationPermissions.launchMultiplePermissionRequest()
    }

    // Save/Discard Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { /* Don't dismiss on outside tap */ },
            title = { Text("Run Complete") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Great job! Here's your run summary:")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Distance: $distance", style = MaterialTheme.typography.bodyLarge)
                    Text("Duration: $elapsedTime", style = MaterialTheme.typography.bodyLarge)
                    Text("Pace: $pace", style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveRun()
                    onNavigateBack()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    viewModel.discardRun()
                    onNavigateBack()
                }) {
                    Text("Discard")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.toggleRun() },
                containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Stop Run" else "Start Run"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Map
            val cameraPositionState = rememberCameraPositionState()
            
            // Fetch initial location to center the map immediately
            LaunchedEffect(locationPermissions.allPermissionsGranted) {
                if (locationPermissions.allPermissionsGranted) {
                    try {
                        val fusedClient = com.google.android.gms.location.LocationServices
                            .getFusedLocationProviderClient(context)
                        fusedClient.lastLocation.addOnSuccessListener { location ->
                            if (location != null) {
                                cameraPositionState.move(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(location.latitude, location.longitude),
                                        17f
                                    )
                                )
                            }
                        }
                    } catch (e: SecurityException) {
                        // Permission not granted yet
                    }
                }
            }
            
            // Update camera to follow user during run
            LaunchedEffect(locationUpdates) {
                locationUpdates.lastOrNull()?.let { location ->
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(location.latitude, location.longitude),
                            17f
                        )
                    )
                }
            }

            if (locationPermissions.allPermissionsGranted) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = true)
                ) {
                    // Draw Polyline
                    if (locationUpdates.isNotEmpty()) {
                        val points = locationUpdates.map { LatLng(it.latitude, it.longitude) }
                        Polyline(
                            points = points,
                            color = Color(0xFF4B39EF),
                            width = 10f
                        )
                    }
                }
            } else {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                     Text("Location permissions required for running tracking.")
                 }
            }

            // Overlay Stats (Cards)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Main Stats Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Progress Ring (Left)
                            ProgressRing(
                                progress = runProgress,
                                label = "GOAL",
                                value = "5km",
                                modifier = Modifier.size(60.dp)
                            )
                            
                            // Timer (Center)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = elapsedTime, style = MaterialTheme.typography.displayMedium)
                                Text(text = "DURATION", style = MaterialTheme.typography.labelSmall)
                            }
                            
                            // Momentum (Right)
                            MomentumIndicator(
                                currentSpeed = currentSpeed,
                                avgSpeed = avgSpeed,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = distance, style = MaterialTheme.typography.headlineSmall)
                                Text(text = "DISTANCE", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = pace, style = MaterialTheme.typography.headlineSmall)
                                Text(text = "PACE", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "$cadence", style = MaterialTheme.typography.headlineSmall)
                                Text(text = "SPM", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Pace Graph Visualization
                if (paceHistory.isNotEmpty()) {
                    Card(
                         modifier = Modifier.fillMaxWidth().height(150.dp),
                         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Pace Trend", style = MaterialTheme.typography.labelSmall)
                            PaceGraph(
                                paceHistory = paceHistory,
                                modifier = Modifier.fillMaxSize().padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
