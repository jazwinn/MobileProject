package com.jazwinn.fitnesstracker.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

private const val MIN_CONFIDENCE = 0.3f
private const val MIN_KEYPOINTS_REQUIRED = 5

/**
 * Composable overlay that draws detected COCO pose skeletons on the camera preview.
 * 
 * Renders keypoint dots (green) and skeletal connections (cyan) for each detected person.
 * Applies a 90° clockwise rotation to align with camera coordinate space.
 */
@Composable
fun YoloPoseOverlay(
    results: List<PoseDetectionResult>,
    modifier: Modifier = Modifier
) {
    // Debug log to confirm recomposition
    SideEffect {
        android.util.Log.d("YoloPoseOverlay", "Recomposing with ${results.size} results")
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (results.isEmpty()) return@Canvas


        // COCO skeleton connections (pairs of keypoint indices)
        val connections = listOf(
            // Head
            0 to 1, 0 to 2, 1 to 3, 2 to 4,
            // Torso
            5 to 6, 5 to 11, 6 to 12, 11 to 12,
            // Left arm
            5 to 7, 7 to 9,
            // Right arm
            6 to 8, 8 to 10,
            // Left leg
            11 to 13, 13 to 15,
            // Right leg
            12 to 14, 14 to 16
        )

        // Rotate 90 degrees CLOCKWISE to match camera orientation: new_x = (1 - y), new_y = x
        fun transformCoords(x: Float, y: Float): Offset {
            val rotatedX = (1f - y) * size.width
            val rotatedY = x * size.height
            return Offset(rotatedX, rotatedY)
        }

        for (pose in results) {
            val keypoints = pose.keypoints

            // Only draw if we have enough confident keypoints
            val confidentCount = keypoints.count { it.confidence >= MIN_CONFIDENCE }
            if (confidentCount < MIN_KEYPOINTS_REQUIRED) continue

            // Draw connections
            for ((startIdx, endIdx) in connections) {
                if (startIdx >= keypoints.size || endIdx >= keypoints.size) continue
                val startKp = keypoints[startIdx]
                val endKp = keypoints[endIdx]

                if (startKp.confidence >= MIN_CONFIDENCE && endKp.confidence >= MIN_CONFIDENCE) {
                    val startPos = transformCoords(startKp.x, startKp.y)
                    val endPos = transformCoords(endKp.x, endKp.y)
                    drawLine(
                        color = Color.Cyan,
                        start = startPos,
                        end = endPos,
                        strokeWidth = 8f // Slightly thinner for better visibility of dots
                    )
                }
            }

            // Draw keypoints
            for (kp in keypoints) {
                if (kp.confidence >= MIN_CONFIDENCE) {
                    val pos = transformCoords(kp.x, kp.y)
                    drawCircle(
                        color = Color.Red,
                        radius = 12f,
                        center = pos
                    )
                    drawCircle(
                        color = Color.Yellow,
                        radius = 8f,
                        center = pos
                    )
                }
            }
        }
    }
}
