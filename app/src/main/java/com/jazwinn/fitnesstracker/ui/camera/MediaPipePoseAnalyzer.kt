package com.jazwinn.fitnesstracker.ui.camera

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.io.ByteArrayOutputStream

class MediaPipePoseAnalyzer(
    private val context: android.content.Context,
    private val onPoseDetected: (PoseLandmarkerResult?) -> Unit
) : ImageAnalysis.Analyzer {

    private var poseLandmarker: PoseLandmarker? = null
    private var frameCount = 0
    private var detectionCount = 0
    
    init {
        Log.d(TAG, "🔄 Initializing MediaPipe Pose Analyzer...")
        setupPoseLandmarker()
    }
    
    private fun setupPoseLandmarker() {
        try {
            poseLandmarker = createPoseLandmarker(Delegate.CPU)
            Log.d(TAG, "✅ MediaPipe initialized successfully with CPU")
        } catch (e: Exception) {
            Log.e(TAG, "❌ FAILED to initialize MediaPipe!", e)
        }
    }
    
    private fun createPoseLandmarker(delegate: Delegate): PoseLandmarker {
        val baseOptions = BaseOptions.builder()
            .setDelegate(delegate)
            .setModelAssetPath("pose_landmarker_lite.task")
            .build()
        
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setMinPoseDetectionConfidence(0.1f) // Ultra low - detect almost anything
            .setMinPosePresenceConfidence(0.1f) 
            .setMinTrackingConfidence(0.1f)
            .setNumPoses(1)
            .setResultListener { result, _ ->
                if (result.landmarks().isNotEmpty()) {
                    val landmarks = result.landmarks()[0]
                    detectionCount++
                    // Log average visibility as confidence indicator
                    val avgVisibility = landmarks.take(5).map { it.visibility().orElse(0f) }.average()
                    Log.d(TAG, "🎯 ✅ POSE #${detectionCount}: ${landmarks.size} landmarks, confidence: ${String.format("%.2f", avgVisibility)}")
                } else {
                    Log.w(TAG, "⚠️ NO landmarks detected - check lighting/positioning/distance")
                }
                onPoseDetected(result)
            }
            .setErrorListener { error ->
                Log.e(TAG, "❌ MediaPipe ERROR: ${error.message}", error)
                onPoseDetected(null)
            }
            .build()
        
        return PoseLandmarker.createFromOptions(context, options)
    }
    
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        frameCount++
        
        if (poseLandmarker == null) {
            if (frameCount <= 3) {
                Log.e(TAG, "❌ Frame ${frameCount}: poseLandmarker is NULL!")
            }
            imageProxy.close()
            return
        }
        
        // Process EVERY frame for maximum detection
        if (frameCount <= 10) {
            Log.d(TAG, "📸 Frame ${frameCount}: ${imageProxy.width}x${imageProxy.height}")
        }
        
        try {
            val bitmap = yuv420ToBitmap(imageProxy)
            val mpImage = BitmapImageBuilder(bitmap).build()
            val timestamp = System.currentTimeMillis()
            
            poseLandmarker?.detectAsync(mpImage, timestamp)
            
            if (frameCount == 5) {
                Log.d(TAG, "✓ Frames being sent to MediaPipe successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Frame ${frameCount} error: ${e.message}", e)
            onPoseDetected(null)
        } finally {
            imageProxy.close()
        }
    }
    
    private fun yuv420ToBitmap(image: ImageProxy): Bitmap {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 95, out) // High quality
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
    
    fun close() {
        Log.d(TAG, "🔄 Closing MediaPipe analyzer (detected ${detectionCount} poses total)")
        poseLandmarker?.close()
        poseLandmarker = null
    }
    
    companion object {
        private const val TAG = "MediaPipePoseAnalyzer"
    }
}
