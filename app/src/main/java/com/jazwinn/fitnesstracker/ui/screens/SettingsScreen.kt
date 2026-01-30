package com.jazwinn.fitnesstracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.jazwinn.fitnesstracker.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Data?") },
            text = { Text("This will delete all your workout history, step data, and profile settings. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection("Preferences") {
                SettingsItem(
                    title = "Dark Mode", 
                    subtitle = if (uiState.isDarkMode) "On" else "Off", 
                    hasSwitch = true, 
                    initialSwitchState = uiState.isDarkMode,
                    onSwitchChange = { viewModel.toggleDarkMode(it) }
                )
                SettingsItem(
                    title = "Notifications", 
                    subtitle = if (uiState.areNotificationsEnabled) "Enabled" else "Disabled", 
                    hasSwitch = true, 
                    initialSwitchState = uiState.areNotificationsEnabled,
                     onSwitchChange = { viewModel.toggleNotifications(it) }
                )
                SettingsItem(
                    title = "Measurement Units", 
                    subtitle = if (uiState.isUnitMetric) "Metric (kg, cm)" else "Imperial (lbs, ft)", // Simple toggle for now
                    hasSwitch = true,
                    initialSwitchState = uiState.isUnitMetric,
                    onSwitchChange = { viewModel.toggleUnitMetric(it) }
                )
            }

            SettingsSection("Data Management") {
                SettingsItem(title = "Export Data", subtitle = "Save as CSV")
                SettingsItem(
                    title = "Reset All Data", 
                    subtitle = "Clear history and profile", 
                    isDestructive = true,
                    onClick = { showResetDialog = true }
                )
            }
            
            SettingsSection("About") {
                SettingsItem(title = "Version", subtitle = "1.0.0")
                SettingsItem(title = "Terms of Service")
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    hasSwitch: Boolean = false,
    initialSwitchState: Boolean = false,
    onSwitchChange: (Boolean) -> Unit = {},
    isDestructive: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (hasSwitch) {
            Switch(checked = initialSwitchState, onCheckedChange = onSwitchChange)
        } else {
            if (!isDestructive && !title.contains("Export")) { // Don't show chevron for reset or export if handled differently
                 Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
