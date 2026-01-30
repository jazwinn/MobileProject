package com.jazwinn.fitnesstracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Filled.Home)
    data object Exercise : Screen("exercise", "Exercise", Icons.Filled.FitnessCenter)
    data object History : Screen("history", "History", Icons.Filled.Analytics)
    data object Profile : Screen("profile", "Profile", Icons.Filled.Person)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}
