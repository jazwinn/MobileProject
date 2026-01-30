package com.jazwinn.fitnesstracker.ui.screens

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.jazwinn.fitnesstracker.service.StepTrackerService
import com.jazwinn.fitnesstracker.ui.viewmodel.HomeViewModel
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val dailySteps by viewModel.dailySteps.collectAsState()
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
            
            // Circular Progress (Placeholder logic)
            val steps = dailySteps?.stepCount ?: 0
            val goal = 10000
            val progress = (steps.toFloat() / goal).coerceIn(0f, 1f)
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$steps", style = MaterialTheme.typography.displayMedium)
                    Text(text = "/ $goal steps", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Stats Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatCard(title = "Distance", value = "%.2f km".format((dailySteps?.distance ?: 0f) / 1000))
                StatCard(title = "Calories", value = "%.0f kcal".format(dailySteps?.calories ?: 0f))
            }
            
            if (!permissionState.allPermissionsGranted) {
                Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                    Text("Grant Permissions")
                }
            } else {
                 Button(onClick = {
                     val steps = dailySteps?.stepCount ?: 0
                     val dist = (dailySteps?.distance ?: 0f) / 1000
                     val cal = dailySteps?.calories ?: 0f
                     com.jazwinn.fitnesstracker.ui.utils.ShareUtils.shareStats(context, steps, dist, cal)
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
