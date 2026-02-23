package com.jazwinn.fitnesstracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jazwinn.fitnesstracker.ui.viewmodel.StatsViewModel
import java.util.Locale

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Header with Navigation
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.previousWeek() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Previous Week")
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Weekly Stats",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = uiState.dateRange,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            IconButton(onClick = { viewModel.nextWeek() }) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next Week")
            }
        }

        // Weekly Steps Chart
        TrendChart(
            title = "Steps History",
            points = uiState.chartPoints,
            lineColor = Color(0xFF4B39EF),
            emptyMessage = "No data for this week"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
             StatSummaryCard("Avg Steps", java.text.NumberFormat.getIntegerInstance().format(uiState.avgSteps), Modifier.weight(1f))
             Spacer(modifier = Modifier.width(16.dp))
             StatSummaryCard("Active Days", "${uiState.activeStreak} Days", Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // BMI Progress Chart
        TrendChart(
            title = "BMI Progress",
            points = uiState.bmiChartPoints,
            lineColor = Color(0xFFE74C3C),
            emptyMessage = "No BMI history yet"
        )

        if (uiState.bmiHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recent Records", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.bmiHistory.reversed().forEach { record ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(record.date, style = MaterialTheme.typography.bodySmall)
                            Text(
                                String.format(Locale.getDefault(), "BMI: %.1f", record.bmiValue),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE74C3C)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun TrendChart(
    title: String,
    points: List<Float>,
    lineColor: Color,
    emptyMessage: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (points.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        
                        val stepX = if (points.size > 1) width / (points.size - 1) else width
                        
                        val path = Path()
                        points.forEachIndexed { index, value ->
                            val x = index * stepX
                            val y = height - (value * height) // Invert Y
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            
                            drawCircle(
                                color = lineColor,
                                radius = 8f,
                                center = Offset(x, y)
                            )
                        }
                        
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 5f, cap = StrokeCap.Round)
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                     Text(emptyMessage, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun StatSummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
             Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
             Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}
