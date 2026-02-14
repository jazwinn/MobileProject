package com.jazwinn.fitnesstracker.ui.screens

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.jazwinn.fitnesstracker.service.StepTrackerService
import com.jazwinn.fitnesstracker.ui.viewmodel.HomeViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import com.jazwinn.fitnesstracker.ui.utils.ShareUtils
import com.jazwinn.fitnesstracker.ui.navigation.Screen
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    navController: androidx.navigation.NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val dailySteps by viewModel.dailySteps.collectAsState()
    val stepGoal by viewModel.stepGoal.collectAsState()
    val context = LocalContext.current

    // Permission handling
    val permissionsList = mutableListOf(
        Manifest.permission.ACTIVITY_RECOGNITION
    )
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissionsList.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    val permissionState = rememberMultiplePermissionsState(permissions = permissionsList)

    LaunchedEffect(Unit) {
        permissionState.launchMultiplePermissionRequest()
    }
    
    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            StepTrackerService.start(context)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Dashboard") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Circular Progress
            val steps = dailySteps?.stepCount ?: 0
            val progress = (steps.toFloat() / stepGoal).coerceIn(0f, 1f)
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$steps", style = MaterialTheme.typography.displayMedium)
                    Text(text = "/ $stepGoal steps", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Stats Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatCard(title = "Distance", value = "%.2f km".format((dailySteps?.distance ?: 0f) / 1000))
                StatCard(title = "Calories", value = "%.0f kcal".format(dailySteps?.calories ?: 0f))
            }
            
            // Quick Actions
            Text(
                text = "Quick Start",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    text = "Outdoor Run",
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.Running.route) }
                )
                QuickActionCard(
                    text = "Push-up",
                    icon = Icons.Default.FitnessCenter,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.ExerciseSession.createRoute("PUSH_UP")) }
                )
                QuickActionCard(
                    text = "Sit-up",
                    icon = Icons.Default.FitnessCenter,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.ExerciseSession.createRoute("SIT_UP")) }
                )
            }
            
            if (!permissionState.allPermissionsGranted) {
                Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                    Text("Grant Permissions")
                }
            } else {
                 Button(onClick = {
                     val currentSteps = dailySteps?.stepCount ?: 0
                     val dist = (dailySteps?.distance ?: 0f) / 1000
                     val cal = dailySteps?.calories ?: 0f
                     ShareUtils.shareStats(context, currentSteps, dist, cal)
                 }) {
                     Text("Share Today's Stats")
                 }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String) {
    Card(modifier = Modifier.width(150.dp)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun QuickActionCard(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

