package com.jazwinn.fitnesstracker.ui.camera

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * CameraX ImageAnalysis.Analyzer that feeds frames through YOLO26 pose detection.
 * 
 * Converts YUV420 camera frames to Bitmap, runs inference via [YoloPoseDetector],
 * and delivers results through the callback. Processes every 2nd frame for performance.
 */
class YoloPoseAnalyzer(
    context: android.content.Context,
    private val onPoseDetected: (List<PoseDetectionResult>) -> Unit
) : ImageAnalysis.Analyzer {

    private var detector: YoloPoseDetector? = null
    private var frameCount = 0
    private var detectionCount = 0

    companion object {
        private const val TAG = "YoloPoseAnalyzer"
        private const val PROCESS_EVERY_N_FRAMES = 2 // Increased sampling rate (was 4)
    }

    init {
        Log.d(TAG, "🔄 Initializing YOLOv8 Pose Analyzer...")
        try {
            detector = YoloPoseDetector(context)
            Log.d(TAG, "✅ YOLOv8 detector initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ FAILED to initialize YOLOv8 detector!", e)
        }
    }

    override fun analyze(imageProxy: ImageProxy) {
        frameCount++

        if (detector == null) {
            imageProxy.close()
            return
        }

        // Skip frames for performance
        if (frameCount % PROCESS_EVERY_N_FRAMES != 0) {
            imageProxy.close()
            return
        }

        try {
            // Convert ImageProxy to Bitmap (handles rotation automatically)
            val bitmap = imageProxy.toBitmap()
            
            val startTime = System.currentTimeMillis()
            val results = detector!!.detect(bitmap)
            val inferenceTime = System.currentTimeMillis() - startTime
            
            // Handle rotation mismatch between ImageAnalysis bitmap and PreviewView
            val rotation = imageProxy.imageInfo.rotationDegrees
            val finalResults = if (rotation != 0) {
                 rotateResults(results, rotation)
            } else {
                 results
            }

            // Only log if detection is found to avoid spam
            if (finalResults.isNotEmpty()) {
                detectionCount++
                // Log.v(TAG, "Inference: ${inferenceTime}ms") 
            }

            onPoseDetected(finalResults)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Frame $frameCount error: ${e.message}", e)
            onPoseDetected(emptyList())
        } finally {
            imageProxy.close()
        }
    }

    private fun rotateResults(results: List<PoseDetectionResult>, rotation: Int): List<PoseDetectionResult> {
        return results.map { pose ->
            val rotatedKeypoints = pose.keypoints.map { kp ->
                val (nx, ny) = rotateCoord(kp.x, kp.y, rotation)
                kp.copy(x = nx, y = ny)
            }
            // Box rotation is complex, approximating by min/max of keypoints or just rotating corners
            // Ideally we rotate box center and w/h.
            // For now, let's just make sure keypoints are right. Box is less critical for overlay (we draw skeleton).
            // But let's rotate box too just in case.
            val b = pose.boundingBox
            val (x1, y1) = rotateCoord(b[0], b[1], rotation)
            val (x2, y2) = rotateCoord(b[2], b[3], rotation)
            // Re-normalize min/max because rotation might flip order
            val minX = kotlin.math.min(x1, x2); val maxX = kotlin.math.max(x1, x2)
            val minY = kotlin.math.min(y1, y2); val maxY = kotlin.math.max(y1, y2)
            
            pose.copy(
                keypoints = rotatedKeypoints,
                boundingBox = floatArrayOf(minX, minY, maxX, maxY)
            )
        }
    }

    private fun rotateCoord(x: Float, y: Float, rotation: Int): Pair<Float, Float> {
        return when (rotation) {
            90 -> Pair(1f - y, x)
            180 -> Pair(1f - x, 1f - y)
            270 -> Pair(y, 1f - x)
            else -> Pair(x, y)
        }
    }

    fun close() {
        Log.d(TAG, "🔄 Closing YOLOv8 analyzer (detected $detectionCount poses total)")
        detector?.close()
        detector = null
    }
}
