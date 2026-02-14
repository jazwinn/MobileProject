package com.jazwinn.fitnesstracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jazwinn.fitnesstracker.ui.viewmodel.HistoryViewModel
import java.util.Calendar
import java.util.Date

// Updated Data Class
data class HistoryEntry(
    val title: String,
    val date: String,
    val value: String,
    val icon: ImageVector,
    val dayOfMonth: Int,
    val timestamp: Long,
    val dateStr: String // Helper for matching
)

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Activity History",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Calendar View
        CalendarView(
            currentMonthYear = uiState.currentMonthYear,
            selectedDate = uiState.selectedDate,
            activeDates = uiState.activeDates,
            onDaySelected = { viewModel.selectDate(it) },
            onPrevMonth = { viewModel.previousMonth() },
            onNextMonth = { viewModel.nextMonth() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.filteredHistory) { item ->
                HistoryItem(item.title, item.date, item.value, item.icon)
            }
            
            if (uiState.filteredHistory.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (uiState.selectedDate != null) "No activity on this day." else "Select a day to view history.", 
                            style = MaterialTheme.typography.bodyLarge, 
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarView(
    currentMonthYear: String,
    selectedDate: Date?,
    activeDates: Set<String>,
    onDaySelected: (Int) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month Header with Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous Month")
                }
                
                Text(
                    text = currentMonthYear.ifEmpty { "Month Year" }, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next Month")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Days Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { 
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            // Calendar Grid Logic
            val calendar = Calendar.getInstance()
            // Parse currentMonthYear back to set calendar month OR pass Date from ViewModel
            // For robust UI, better to pass a Date object representing the month start
            // Here we do a quick parse or rely on the fact that ViewModel tracks state
            try {
                val format = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
                val date = format.parse(currentMonthYear)
                if (date != null) {
                    calendar.time = date
                }
            } catch (e: Exception) {
               // Fallback to now
            }
            
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1 = Sunday
            val startOffset = dayOfWeek - 1
            
            // Helper to format date for checking activeDates
            fun getDateStr(day: Int): String {
                val cal = calendar.clone() as Calendar
                cal.set(Calendar.DAY_OF_MONTH, day)
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                return format.format(cal.time)
            }

            var currentDay = 1
            // 6 rows to cover all possible month layouts
            for (week in 0..5) {
                if (currentDay > daysInMonth) break;

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (day in 0..6) {
                         if (week == 0 && day < startOffset) {
                             Box(modifier = Modifier.size(32.dp))
                         } else if (currentDay <= daysInMonth) {
                             val dayNum = currentDay
                             val dateStr = getDateStr(dayNum)
                             
                             // Check selection
                             val isSelected = selectedDate?.let { 
                                 val calSel = Calendar.getInstance()
                                 calSel.time = it
                                 val calCurrent = calendar.clone() as Calendar
                                 calCurrent.set(Calendar.DAY_OF_MONTH, dayNum)
                                 
                                 // Compare Year, Month, Day
                                 calSel.get(Calendar.YEAR) == calCurrent.get(Calendar.YEAR) &&
                                 calSel.get(Calendar.MONTH) == calCurrent.get(Calendar.MONTH) &&
                                 calSel.get(Calendar.DAY_OF_MONTH) == dayNum
                             } ?: false

                             val hasActivity = activeDates.contains(dateStr)
                             
                             // Check if it is today
                             val today = Calendar.getInstance()
                             val isToday = today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                                           today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                                           today.get(Calendar.DAY_OF_MONTH) == dayNum

                             Box(
                                 modifier = Modifier
                                     .size(32.dp)
                                     .clip(CircleShape)
                                     .clickable { onDaySelected(dayNum) }
                                     .background(
                                         color = when {
                                             isSelected -> MaterialTheme.colorScheme.primary
                                             hasActivity -> MaterialTheme.colorScheme.primaryContainer
                                             else -> Color.Transparent
                                         },
                                         shape = CircleShape
                                     ),
                                 contentAlignment = Alignment.Center
                             ) {
                                 Text(
                                     text = "$dayNum", 
                                     color = when {
                                         isSelected -> Color.White
                                         else -> MaterialTheme.colorScheme.onSurface
                                     },
                                     fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                 )
                             }
                             currentDay++
                         } else {
                             Box(modifier = Modifier.size(32.dp))
                         }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun HistoryItem(title: String, date: String, value: String, icon: ImageVector) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp), 
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) 
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
