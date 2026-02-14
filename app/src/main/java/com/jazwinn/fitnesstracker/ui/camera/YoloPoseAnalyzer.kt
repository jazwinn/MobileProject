package com.jazwinn.fitnesstracker.ui.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * CameraX ImageAnalysis.Analyzer that feeds frames through YOLOv8 pose detection.
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
        private const val PROCESS_EVERY_N_FRAMES = 4 // Process approx 7.5fps to reduce heat
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
            if (frameCount <= 3) {
                Log.e(TAG, "❌ Frame $frameCount: detector is NULL!")
            }
            imageProxy.close()
            return
        }

        // Skip frames for performance
        if (frameCount % PROCESS_EVERY_N_FRAMES != 0) {
            imageProxy.close()
            return
        }

        try {
            val nchwFloats = processImageToNchw(imageProxy)
            val startTime = System.currentTimeMillis()
            val results = detector!!.detect(nchwFloats, imageProxy.width, imageProxy.height)
            val inferenceTime = System.currentTimeMillis() - startTime

            if (results.isNotEmpty()) {
                detectionCount++
                val kpCount = results[0].keypoints.count { it.confidence > 0.3f }
                Log.d(TAG, "🎯 Detection #$detectionCount: ${results.size} person(s), $kpCount confident keypoints. Inference: ${inferenceTime}ms")
            } else {
                 if (frameCount % 30 == 0) {
                     Log.d(TAG, "⚠️ Frame $frameCount: No pose detected. Inference: ${inferenceTime}ms")
                 }
            }

            onPoseDetected(results)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Frame $frameCount error: ${e.message}", e)
            onPoseDetected(emptyList())
        } finally {
            imageProxy.close()
        }
    }

    private fun processImageToNchw(image: ImageProxy): FloatArray {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        // Native conversion & resize
        // Target size is 640x640 for YOLOv8
        val nchwFloats = ImageUtils.yuvToNchwFloats(
            nv21,
            image.width,
            image.height,
            640,
            640
        )

        return nchwFloats ?: throw RuntimeException("Native image processing failed")
    }

    fun close() {
        Log.d(TAG, "🔄 Closing YOLOv8 analyzer (detected $detectionCount poses total)")
        detector?.close()
        detector = null
    }
}
