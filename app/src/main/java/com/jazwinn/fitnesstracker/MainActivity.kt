package com.jazwinn.fitnesstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.isSystemInDarkTheme
import javax.inject.Inject
import com.jazwinn.fitnesstracker.data.repository.SettingsRepository
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jazwinn.fitnesstracker.ui.navigation.Screen
import com.jazwinn.fitnesstracker.ui.screens.ExerciseScreen
import com.jazwinn.fitnesstracker.ui.screens.HistoryScreen as HistoryScreenComposable
import com.jazwinn.fitnesstracker.ui.screens.HomeScreen
import com.jazwinn.fitnesstracker.ui.screens.ProfileScreen
import com.jazwinn.fitnesstracker.ui.screens.SettingsScreen
import com.jazwinn.fitnesstracker.ui.theme.FitnessTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by settingsRepository.isDarkMode.collectAsState(initial = isSystemInDarkTheme())
            FitnessTrackerTheme(darkTheme = isDarkMode) {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Home,
        Screen.ExerciseMenu,
        Screen.Stats,
        Screen.History,
        Screen.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.ExerciseMenu.route) { com.jazwinn.fitnesstracker.ui.screens.ExerciseMenuScreen(navController) }
            composable(Screen.Stats.route) { com.jazwinn.fitnesstracker.ui.screens.StatsScreen() }
            composable(Screen.History.route) { HistoryScreenComposable() }
            composable(Screen.Profile.route) { com.jazwinn.fitnesstracker.ui.screens.ProfileScreen() }
            
            composable(Screen.ExerciseSession.route) { backStackEntry -> 
                val type = backStackEntry.arguments?.getString("type")
                ExerciseScreen(
                    exerciseType = type,
                    onClose = { navController.popBackStack() },
                    onNavigateHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                ) 
            }
            
            composable(Screen.MachineRecognition.route) {
                com.jazwinn.fitnesstracker.ui.screens.machine.MachineRecognitionScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.Running.route) { 
                com.jazwinn.fitnesstracker.ui.screens.RunningScreen(
                    onNavigateBack = { navController.popBackStack() }
                ) 
            }
            composable(Screen.Settings.route) { SettingsScreen(navController) }
        }
    }
}

