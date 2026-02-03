package com.jazwinn.fitnesstracker.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

private const val MIN_CONFIDENCE = 0.3f
private const val MIN_LANDMARKS_REQUIRED = 5

@Composable
fun MediaPipePoseOverlay(
    result: PoseLandmarkerResult?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (result == null || result.landmarks().isEmpty()) return@Canvas
        
        val landmarks = result.landmarks()[0]
        if (landmarks.isEmpty()) return@Canvas

        val confidentLandmarks = landmarks.filter {
            it.visibility().orElse(0f) >= MIN_CONFIDENCE
        }
        
        if (confidentLandmarks.size < MIN_LANDMARKS_REQUIRED) {
            return@Canvas
        }

        val connections = listOf(
            0 to 1, 1 to 2, 2 to 3, 3 to 7,
            0 to 4, 4 to 5, 5 to 6, 6 to 8,
            11 to 12,
            11 to 13, 13 to 15, 15 to 17, 15 to 19, 15 to 21, 17 to 19,
            12 to 14, 14 to 16, 16 to 18, 16 to 20, 16 to 22, 18 to 20,
            11 to 23, 12 to 24, 23 to 24,
            23 to 25, 25 to 27, 27 to 29, 27 to 31, 29 to 31,
            24 to 26, 26 to 28, 28 to 30, 28 to 32, 30 to 32
        )

        // Rotate 90 degrees CLOCKWISE: new_x = (1 - y), new_y = x
        fun transformCoords(x: Float, y: Float): Offset {
            val rotatedX = (1f - y) * size.width
            val rotatedY = x * size.height
            return Offset(rotatedX, rotatedY)
        }

        // Draw connections
        connections.forEach { (start, end) ->
            if (start < landmarks.size && end < landmarks.size) {
                val startLandmark = landmarks[start]
                val endLandmark = landmarks[end]
                
                val startVis = startLandmark.visibility().orElse(0f)
                val endVis = endLandmark.visibility().orElse(0f)
                
                if (startVis >= MIN_CONFIDENCE && endVis >= MIN_CONFIDENCE) {
                    val startPos = transformCoords(startLandmark.x(), startLandmark.y())
                    val endPos = transformCoords(endLandmark.x(), endLandmark.y())
                    
                    drawLine(
                        color = Color.Cyan,
                        start = startPos,
                        end = endPos,
                        strokeWidth = 12f
                    )
                }
            }
        }

        // Draw landmarks
        confidentLandmarks.forEach { landmark ->
            val pos = transformCoords(landmark.x(), landmark.y())
            
            drawCircle(
                color = Color.Green,
                radius = 18f,
                center = pos
            )
        }
    }
}
