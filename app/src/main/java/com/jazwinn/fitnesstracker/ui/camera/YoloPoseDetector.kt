package com.jazwinn.fitnesstracker.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * A single detected keypoint with position and confidence.
 * Coordinates are normalized [0, 1] relative to the original image.
 */
data class Keypoint(
    val x: Float,
    val y: Float,
    val confidence: Float
)

/**
 * Result of a single pose detection containing a bounding box and 17 COCO keypoints.
 *
 * COCO Keypoint indices:
 * 0=Nose, 1=L_Eye, 2=R_Eye, 3=L_Ear, 4=R_Ear,
 * 5=L_Shoulder, 6=R_Shoulder, 7=L_Elbow, 8=R_Elbow,
 * 9=L_Wrist, 10=R_Wrist, 11=L_Hip, 12=R_Hip,
 * 13=L_Knee, 14=R_Knee, 15=L_Ankle, 16=R_Ankle
 */
data class PoseDetectionResult(
    val boundingBox: FloatArray, // [x1, y1, x2, y2] normalized
    val score: Float,
    val keypoints: List<Keypoint>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PoseDetectionResult
        return boundingBox.contentEquals(other.boundingBox) && score == other.score && keypoints == other.keypoints
    }
    override fun hashCode(): Int = 31 * (31 * boundingBox.contentHashCode() + score.hashCode()) + keypoints.hashCode()
}

/**
 * YOLOv8 Pose Detector using TensorFlow Lite.
 * 
 * Loads a yolo26n-pose_float32.tflite model from assets and runs inference on bitmaps.
 * Input: 640x640 RGB image (NHWC), Output: bounding boxes + 17 COCO keypoints per person.
 */
class YoloPoseDetector(context: Context) {

    private var interpreter: Interpreter? = null
    
    private val inputSize = 640 // YOLOv8 expects 640x640

    // Confidence thresholds
    private val confThreshold = 0.15f
    private val iouThreshold = 0.45f

    companion object {
        private const val TAG = "YoloPoseDetector"
        private const val MODEL_NAME = "yolo26n-pose.tflite"
        private const val NUM_KEYPOINTS = 17
    }

    private var outputShape: IntArray = intArrayOf(1, 56, 8400)
    private lateinit var imageProcessor: org.tensorflow.lite.support.image.ImageProcessor
    
    init {
        Log.d(TAG, "🔄 Initializing YOLOv8 Pose Detector (TFLite)...")
        try {
            // 1. Load model file
            val mappedByteBuffer = FileUtil.loadMappedFile(context, MODEL_NAME)
            Log.d(TAG, "📂 Read model '$MODEL_NAME'")

            // 2. Init Interpreter with safe fallback
        try {
            // Attempt 1: Try with GPU Delegate
            val optionsGpu = Interpreter.Options()
            optionsGpu.setNumThreads(4)
            optionsGpu.addDelegate(org.tensorflow.lite.gpu.GpuDelegate())
            
            Log.d(TAG, "⚙️ Creating TFLite interpreter with GPU Delegate...")
            interpreter = Interpreter(mappedByteBuffer, optionsGpu)
            Log.d(TAG, "🚀 GPU Delegate successfully applied.")
        } catch (e: Throwable) {
            Log.e(TAG, "⚠️ GPU Delegate failed to apply (unsupported ops). Falling back to CPU.", e)
            
            // Attempt 2: Fallback to CPU
            try {
                val optionsCpu = Interpreter.Options()
                optionsCpu.setNumThreads(4)
                Log.d(TAG, "⚙️ Creating TFLite interpreter (CPU Fallback)...")
                interpreter = Interpreter(mappedByteBuffer, optionsCpu)
                Log.d(TAG, "✅ CPU Interpreter created successfully.")
            } catch (eCpu: Exception) {
                 Log.e(TAG, "❌ CRITICAL: Failed to initialize TFLite Interpreter even on CPU", eCpu)
                 throw eCpu
            }
        }
            
            // 4. Inspect output shape
            val outputTensor = interpreter?.getOutputTensor(0)
            if (outputTensor != null) {
                outputShape = outputTensor.shape() 
            }
            
            // 5. Initialize Image Processor (Resize + Normalize)
            imageProcessor = org.tensorflow.lite.support.image.ImageProcessor.Builder()
                .add(org.tensorflow.lite.support.image.ops.ResizeOp(inputSize, inputSize, org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod.BILINEAR))
                .add(org.tensorflow.lite.support.common.ops.NormalizeOp(0f, 255f))
                .build()
            
            Log.d(TAG, "✅ YOLOv8 Pose model loaded.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ CRITICAL: Failed to initialize YoloPoseDetector", e)
            throw e 
        }
    }

    /**
     * Run pose detection on a bitmap.
     * @param bitmap The input image.
     * @return List of detected poses.
     */
    fun detect(bitmap: Bitmap): List<PoseDetectionResult> {
        try {
            val originalWidth = bitmap.width
            val originalHeight = bitmap.height

            // 1. Preprocess using Support Library (Fast!)
            var tensorImage = org.tensorflow.lite.support.image.TensorImage(org.tensorflow.lite.DataType.FLOAT32)
            
            // Ensure bitmap is ARGB_8888 (Hardware bitmaps cause crashes with TensorImage)
            val argbBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
                bitmap.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                bitmap
            }
            
            tensorImage.load(argbBitmap)
            tensorImage = imageProcessor.process(tensorImage)
            
            // 2. Run inference
            val d1 = outputShape[1]
            val d2 = outputShape[2]
            
            val outputArray = Array(1) { Array(d1) { FloatArray(d2) } }
            
            interpreter?.run(tensorImage.buffer, outputArray)
            
            // 3. Post-process (Handle Stretched Input)
            val poses = postprocess(outputArray[0], originalWidth, originalHeight)
            
            if (argbBitmap != bitmap) {
                argbBitmap.recycle()
            }

            return poses
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during detection: ${e.message}", e)
            return emptyList()
        }
    }
    
    // Manual preprocess removed in favor of ImageProcessor

    /**
     * Post-process YOLOv8-pose output tensor.
     * Handles both [56][8400] and [300][57].
     */
    private fun postprocess(
        outputData: Array<FloatArray>, 
        originalWidth: Int,
        originalHeight: Int
    ): List<PoseDetectionResult> {
        val dim1 = outputData.size       // 56 or 300
        val dim2 = outputData[0].size    // 8400 or 57
        
        val candidates = mutableListOf<PoseDetectionResult>()

        if (dim1 == 300 && dim2 == 57) {
            // [300][57] TFLite Fused
            for (d in 0 until dim1) {
                val detection = outputData[d]
                val score = detection[4] 
                
                if (score < confThreshold) continue 
                
                // Heuristic Check for normalization (sometimes export creates pixels, sometimes normalized)
                // If coordinates are clearly > 1.0, they are pixels.
                val isNormalized = (detection[0] <= 1.5f && detection[2] <= 1.5f) 
                
                val divisor = if (isNormalized) 1.0f else inputSize.toFloat()
                
                // Coordinates
                val cx = detection[0] / divisor
                val cy = detection[1] / divisor
                val w = detection[2] / divisor
                val h = detection[3] / divisor
                
                val x1 = cx - w / 2
                val y1 = cy - h / 2
                val x2 = cx + w / 2
                val y2 = cy + h / 2
                
                // Parse keypoints
                val keypoints = mutableListOf<Keypoint>()
                for (k in 0 until NUM_KEYPOINTS) {
                    val offset = 6 + k * 3
                    val kx = detection[offset] / divisor
                    val ky = detection[offset + 1] / divisor
                    val kConf = detection[offset + 2]
                    
                    keypoints.add(Keypoint(kx.coerceIn(0f, 1f), ky.coerceIn(0f, 1f), kConf))
                }
                
                candidates.add(PoseDetectionResult(
                    boundingBox = floatArrayOf(
                        x1.coerceIn(0f, 1f), y1.coerceIn(0f, 1f),
                        x2.coerceIn(0f, 1f), y2.coerceIn(0f, 1f)
                    ),
                    score = score,
                    keypoints = keypoints
                ))
            }
            return candidates
            
        } else if (dim1 == 56 && dim2 == 8400) {
            // [56][8400]
            val numDetections = dim2
            
            for (d in 0 until numDetections) {
                val conf = outputData[4][d]
                if (conf < confThreshold) continue

                val cx = outputData[0][d] / inputSize
                val cy = outputData[1][d] / inputSize
                val w = outputData[2][d] / inputSize
                val h = outputData[3][d] / inputSize
                
                val x1 = cx - w / 2
                val y1 = cy - h / 2
                val x2 = cx + w / 2
                val y2 = cy + h / 2

                val keypoints = mutableListOf<Keypoint>()
                for (k in 0 until NUM_KEYPOINTS) {
                    val kx = outputData[5 + k * 3][d] / inputSize
                    val ky = outputData[5 + k * 3 + 1][d] / inputSize
                    val kConf = outputData[5 + k * 3 + 2][d]

                    keypoints.add(Keypoint(kx.coerceIn(0f, 1f), ky.coerceIn(0f, 1f), kConf))
                }

                candidates.add(PoseDetectionResult(
                    boundingBox = floatArrayOf(
                        x1.coerceIn(0f, 1f), y1.coerceIn(0f, 1f),
                        x2.coerceIn(0f, 1f), y2.coerceIn(0f, 1f)
                    ),
                    score = conf,
                    keypoints = keypoints
                ))
            }
            return applyNMS(candidates)
        }
        
        return emptyList()
    }

    /**
     * Non-Maximum Suppression to remove overlapping detections.
     */
    private fun applyNMS(detections: List<PoseDetectionResult>): List<PoseDetectionResult> {
        if (detections.isEmpty()) return emptyList()

        val sorted = detections.sortedByDescending { it.score }.toMutableList()
        val result = mutableListOf<PoseDetectionResult>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            result.add(best)
            sorted.removeAll { calculateIoU(best.boundingBox, it.boundingBox) > iouThreshold }
        }

        return result
    }

    private fun calculateIoU(boxA: FloatArray, boxB: FloatArray): Float {
        val x1 = max(boxA[0], boxB[0]); val y1 = max(boxA[1], boxB[1])
        val x2 = min(boxA[2], boxB[2]); val y2 = min(boxA[3], boxB[3])
        val intersection = max(0f, x2 - x1) * max(0f, y2 - y1)
        val areaA = (boxA[2] - boxA[0]) * (boxA[3] - boxA[1])
        val areaB = (boxB[2] - boxB[0]) * (boxB[3] - boxB[1])
        val union = areaA + areaB - intersection
        return if (union > 0) intersection / union else 0f
    }

    fun close() {
        Log.d(TAG, "🔄 Closing YOLO26 Pose Detector")
        interpreter?.close()
        interpreter = null
    }

    private data class PadInfo(val scale: Float, val padX: Int, val padY: Int)
}
