package com.jazwinn.fitnesstracker.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jazwinn.fitnesstracker.data.local.entity.WorkoutEntity
import com.jazwinn.fitnesstracker.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val currentMonth by viewModel.currentMonth.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dailyStats by viewModel.statsForSelectedDate.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("History") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Calendar View
            CalendarView(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                onMonthChange = viewModel::changeMonth,
                onDateSelected = viewModel::selectDate
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Daily Stats View
            DailyStatsDetail(
                date = selectedDate,
                steps = dailyStats.steps,
                calories = dailyStats.calories,
                distance = dailyStats.distance,
                workouts = dailyStats.workouts
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarView(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Month Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
            }
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Days Header
        Row(modifier = Modifier.fillMaxWidth()) {
            val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days Grid
        // Calculate days in month and start padding
        val daysInMonth = currentMonth.lengthOfMonth()
        val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value % 7 // adjust so Sunday = 0 or similar based on locale, Java DayOfWeek starts Mon=1. 
        // Java Time: Mon=1, Sun=7.
        // We want Sun, Mon, Tue...
        // If Sun=7, we want index 0 -> 7%7=0. Mon=1. Correct if first col is Sunday.
        
        val startOffset = if (firstDayOfMonth == 7) 0 else firstDayOfMonth 
        // Wait, DayOfWeek.MONDAY.value is 1. Calendar usually starts Sunday.
        // If logic is Sun, Mon, Tue...
        // Sun(0), Mon(1)
        // ISO Mon=1. So we want Sunday to be 0 or 7?
        // Let's assume standard grid S M T W T F S
        // If 1st is Monday(1), offset is 1.
        // If 1st is Sunday(7), offset is 0.
        // If 1st is Tuesday(2), offset is 2.
        // So offset = dayOfWeek.value % 7.
        // Mon(1)%7 = 1.
        // Sun(7)%7 = 0.
        // Correct.

        val totalSlots = daysInMonth + startOffset
        val rows = (totalSlots + 6) / 7

        Column {
            for (week in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (dayOfWeek in 0..6) {
                        val dayIndex = (week * 7) + dayOfWeek
                        val dayOfMonth = dayIndex - startOffset + 1

                        if (dayOfMonth in 1..daysInMonth) {
                            val date = currentMonth.atDay(dayOfMonth)
                            val isSelected = date == selectedDate
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onDateSelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayOfMonth.toString(),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                            else MaterialTheme.colorScheme.onSurface 
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DailyStatsDetail(
    date: LocalDate,
    steps: Long,
    calories: Float,
    distance: Float,
    workouts: List<WorkoutEntity>
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Stats for ${date.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem(value = steps.toString(), label = "Steps")
                StatItem(value = calories.toString(), label = "Kcal")
                StatItem(value = String.format(Locale.US, "%.2f", distance), label = "Km")
            }
        }

        item {
            Text("Workouts", style = MaterialTheme.typography.titleMedium)
        }

        if (workouts.isEmpty()) {
            item {
                Text(
                    text = "No workouts recorded for this day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(workouts.size) { index ->
                val workout = workouts[index]
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(workout.type, style = MaterialTheme.typography.titleMedium)
                            Text(
                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(workout.timestamp)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text("${workout.reps} Reps", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
