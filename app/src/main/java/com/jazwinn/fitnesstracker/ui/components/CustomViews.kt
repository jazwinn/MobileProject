package com.jazwinn.fitnesstracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PaceGraph(
    paceHistory: List<Float>, // List of pace values (min/km)
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier) {
        if (paceHistory.isEmpty()) return@Canvas
        
        val width = size.width
        val height = size.height
        
        // Calculate min/max for scaling
        val maxPace = paceHistory.maxOrNull() ?: 10f
        val minPace = paceHistory.minOrNull() ?: 0f
        val range = (maxPace - minPace).coerceAtLeast(1f)
        
        val path = Path()
        
        paceHistory.forEachIndexed { index, pace ->
            val x = (index.toFloat() / (paceHistory.size - 1).coerceAtLeast(1)) * width
            // Invert Y because higher pace value (slower) should be lower on graph? 
            // Usually running graphs show faster (lower min/km) higher up.
            // Let's assume lower value = higher on graph.
            val normalizedPace = (pace - minPace) / range
            val y = height - (1f - normalizedPace) * height // Flip: lower pace = higher y
            
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // Draw gradient below line
        val gradientPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        
        drawPath(
            path = gradientPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )
    }
}

@Composable
fun MomentumIndicator(
    currentSpeed: Float, // m/s
    avgSpeed: Float, // m/s
    modifier: Modifier = Modifier
) {
    // Visual representation of speed changes (Momentum)
    // If current > avg, show forward momentum (green arrow/gauge)
    // If current < avg, show slowing (red)
    
    val diff = (currentSpeed - avgSpeed).coerceIn(-2f, 2f) // Cap at +/- 2 m/s diff
    val normalizedDiff = diff / 2f // -1.0 to 1.0
    
    val color = when {
        normalizedDiff > 0.1f -> Color.Green
        normalizedDiff < -0.1f -> Color.Red
        else -> Color.Gray
    }
    
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2
        
        // Draw background circle
        drawCircle(color = Color.LightGray.copy(alpha = 0.3f), style = Stroke(width = 8.dp.toPx()))
        
        // Draw momentum arc
        val sweepAngle = normalizedDiff * 180f // +/- 180 degrees
        
        drawArc(
            color = color,
            startAngle = 270f, // Top
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )
    }
}

@Composable
fun ProgressRing(
    progress: Float, // 0.0 to 1.0
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val diameter = size.minDimension
            val radius = diameter / 2
            
            // Background
            drawCircle(
                color = color.copy(alpha = 0.2f),
                style = Stroke(width = strokeWidth)
            )
            
            // Progress
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360 * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
