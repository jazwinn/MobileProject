package com.jazwinn.fitnesstracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Filled.Home)
    data object ExerciseMenu : Screen("exercise_menu", "Exercise", Icons.Filled.FitnessCenter)
    data object Stats : Screen("stats", "Stats", Icons.Filled.BarChart)
    data object History : Screen("history", "History", Icons.Filled.DateRange)
    data object Profile : Screen("profile", "Profile", Icons.Filled.Person)
    
    // Non-tab screens
    data object ExerciseSession : Screen("exercise_session/{type}", "Session", Icons.Filled.FitnessCenter) {
        fun createRoute(type: String) = "exercise_session/$type"
    }

    data object Running : Screen("running", "Running", Icons.Filled.DirectionsRun)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}
