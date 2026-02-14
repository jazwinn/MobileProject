package com.jazwinn.fitnesstracker.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
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
 * YOLOv8 Pose Detector using ONNX Runtime.
 * 
 * Loads a yolov8n-pose.onnx model from assets and runs inference on bitmaps.
 * Input: 640x640 RGB image, Output: bounding boxes + 17 COCO keypoints per person.
 */
class YoloPoseDetector(context: Context) {

    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val ortSession: OrtSession
    
    private val inputSize = 640 // YOLOv8 expects 640x640

    // Confidence thresholds
    private val confThreshold = 0.15f
    private val iouThreshold = 0.45f

    companion object {
        private const val TAG = "YoloPoseDetector"
        private const val MODEL_NAME = "yolov8n-pose.onnx"
        private const val NUM_KEYPOINTS = 17
    }

    init {
        Log.d(TAG, "🔄 Initializing YOLOv8 Pose Detector...")
        try {
            // 1. Read model file
            val inputStream = context.assets.open(MODEL_NAME)
            val modelBytes = inputStream.readBytes()
            inputStream.close()
            Log.d(TAG, "📂 Read model '$MODEL_NAME': ${modelBytes.size} bytes")

            // 2. Create session options
            val sessionOptions = OrtSession.SessionOptions()
            // remove "ORT" format constraint to support standard ONNX files
            // sessionOptions.addConfigEntry("session.load_model_format", "ORT")

            // 3. Create session
            Log.d(TAG, "⚙️ Creating ORT session...")
            ortSession = ortEnv.createSession(modelBytes, sessionOptions)
            
            Log.d(TAG, "✅ YOLOv8 Pose model loaded. Input: ${ortSession.inputNames}, Output: ${ortSession.outputNames}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ CRITICAL: Failed to initialize YoloPoseDetector", e)
            throw e // Re-throw to be caught by Analyzer
        }
    }

    /**
     * Run pose detection on a bitmap.
     * @param bitmap The input image (any size, will be resized to 640x640)
     * @return List of detected poses, empty if none found
     */
    fun detect(bitmap: Bitmap): List<PoseDetectionResult> {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        // 1. Preprocess: resize with letterboxing, normalize to [0,1], convert to NCHW float tensor
        val (inputTensor, padInfo) = preprocess(bitmap)

        // 2. Run inference
        val inputName = ortSession.inputNames.first()
        val onnxTensor = OnnxTensor.createTensor(ortEnv, inputTensor, longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong()))
        
        val results = ortSession.run(mapOf(inputName to onnxTensor))
        
        // 3. Post-process: parse output tensor into pose results
        // YOLOv8-pose output shape: [1, 56, 8400]
        // 56 = 4 (bbox: cx, cy, w, h) + 1 (conf) + 51 (17 keypoints × 3: x, y, visibility)
        val outputName = ortSession.outputNames.first()
        val outputTensor = results[outputName].get() as OnnxTensor
        val outputData = outputTensor.floatBuffer

        val poses = postprocess(outputData, padInfo, originalWidth, originalHeight)

        onnxTensor.close()
        results.close()

        return poses
    }

    /**
     * Run pose detection on a pre-processed NCHW float array.
     * @param nchwFloats The input float array (3 * 640 * 640), normalized [0, 1]
     * @param originalWidth Width of the original camera frame (for post-processing coordinates)
     * @param originalHeight Height of the original camera frame
     * @return List of detected poses, empty if none found
     */
    fun detect(nchwFloats: FloatArray, originalWidth: Int, originalHeight: Int): List<PoseDetectionResult> {
        // Create ONNX Tensor from float array
        val inputTensor = FloatBuffer.wrap(nchwFloats)
        
        // Calculate scale/pad info for post-processing
        // Since the C++ code resizes with aspect ratio preservation (or simple resize? let's check C++ logic)
        // Wait, the C++ logic I wrote does SIMPLE resize (sx = x * srcWidth / dstWidth).
        // It DOES NOT do letterboxing. It stretches the image!
        // This is fine for YOLO usually, but coordinates mapping back might be slightly different.
        // For now, let's assume we map back using simple scaling.
        val padInfo = PadInfo(
            scale = max(inputSize.toFloat() / originalWidth, inputSize.toFloat() / originalHeight), // Approximation
            padX = 0,
            padY = 0
        )
        // Actually, since C++ does direct resize without padding:
        // x_original = x_640 * (originalWidth / 640)
        // y_original = y_640 * (originalHeight / 640)
        // My postprocess function uses PadInfo which assumes letterboxing.
        // I should probably update postprocess to handle "stretched" resize if that's what C++ does.
        // OR, I should update C++ to do letterboxing.
        // Given "simple nearest-neighbor resizing", it STRETCHES.
        // Let's stick to STRETCHING for speed, and update postprocess logic indirectly by faking PadInfo 
        // effectively: scaleX = 640/w, scaleY = 640/h.
        // But postprocess uses a single 'scale'.
        
        // Let's use a specialized post-process for this, or modify postprocess.
        // For now, let's execute inference.
        
        val inputName = ortSession.inputNames.first()
        val onnxTensor = OnnxTensor.createTensor(ortEnv, inputTensor, longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong()))
        
        val results = ortSession.run(mapOf(inputName to onnxTensor))
        
        val outputName = ortSession.outputNames.first()
        val outputTensor = results[outputName].get() as OnnxTensor
        val outputData = outputTensor.floatBuffer

        // We need a post-process that handles independent X/Y scaling because we stretched the image.
        val poses = postprocessStretched(outputData, originalWidth, originalHeight)

        onnxTensor.close()
        results.close()

        return poses
    }

    /**
     * Post-process logic for image that was STRETCHED to 640x640 (not letterboxed).
     */
    private fun postprocessStretched(
        outputData: FloatBuffer,
        originalWidth: Int,
        originalHeight: Int
    ): List<PoseDetectionResult> {
        val numDetections = 8400
        val numChannels = 56 

        val detections = Array(numDetections) { FloatArray(numChannels) }
        for (c in 0 until numChannels) {
            for (d in 0 until numDetections) {
                detections[d][c] = outputData.get(c * numDetections + d)
            }
        }

        val candidates = mutableListOf<PoseDetectionResult>()
        for (det in detections) {
            val conf = det[4]
            if (conf < confThreshold) continue

            // Box in 640x640
            val cx = det[0]; val cy = det[1]; val w = det[2]; val h = det[3]
            
            // Map back to original size (Stretched)
            // x_orig = x_640 / 640 * origW
            val normX = 1f / inputSize * originalWidth
            val normY = 1f / inputSize * originalHeight
            
            val cxOrig = cx * normX
            val cyOrig = cy * normY
            val wOrig = w * normX
            val hOrig = h * normY
            
            val x1 = (cxOrig - wOrig / 2) / originalWidth
            val y1 = (cyOrig - hOrig / 2) / originalHeight
            val x2 = (cxOrig + wOrig / 2) / originalWidth
            val y2 = (cyOrig + hOrig / 2) / originalHeight
            
            val boundingBox = floatArrayOf(
                 x1.coerceIn(0f, 1f), y1.coerceIn(0f, 1f),
                 x2.coerceIn(0f, 1f), y2.coerceIn(0f, 1f)
            )

            val keypoints = mutableListOf<Keypoint>()
            for (k in 0 until NUM_KEYPOINTS) {
                val kx = det[5 + k * 3]
                val ky = det[5 + k * 3 + 1]
                val kConf = det[5 + k * 3 + 2]
                
                val finalX = (kx * normX / originalWidth).coerceIn(0f, 1f)
                val finalY = (ky * normY / originalHeight).coerceIn(0f, 1f)
                
                keypoints.add(Keypoint(finalX, finalY, kConf))
            }

            candidates.add(PoseDetectionResult(boundingBox, conf, keypoints))
        }

        return applyNMS(candidates)
    }

    /**
     * Preprocess bitmap: letterbox resize to 640x640, normalize to [0,1], return NCHW float buffer.
     */
    private fun preprocess(bitmap: Bitmap): Pair<FloatBuffer, PadInfo> {
        // Calculate letterbox dimensions
        val scale = min(inputSize.toFloat() / bitmap.width, inputSize.toFloat() / bitmap.height)
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        val padX = (inputSize - newWidth) / 2
        val padY = (inputSize - newHeight) / 2

        // Resize bitmap
        // createScaledBitmap might return the same object if dimensions match
        val resized = if (bitmap.width != newWidth || bitmap.height != newHeight) {
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
        
        // Create padded 640x640 bitmap with gray fill (114, 114, 114)
        // Optimization: If resized matches inputSize, use it directly (don't create new bitmap)
        val padded = if (resized.width == inputSize && resized.height == inputSize) {
            resized
        } else {
            val p = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(p)
            canvas.drawColor(android.graphics.Color.rgb(114, 114, 114))
            canvas.drawBitmap(resized, padX.toFloat(), padY.toFloat(), null)
            p
        }
        
        if (resized != bitmap && resized != padded) {
            resized.recycle()
        }

        // Convert to NCHW float buffer, normalized to [0, 1]
        val pixels = IntArray(inputSize * inputSize)
        padded.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        
        // Only recycle padded if we created it (i.e. it's not the input bitmap)
        if (padded != bitmap) {
            padded.recycle()
        }

        val buffer = FloatBuffer.allocate(3 * inputSize * inputSize)
        val channelSize = inputSize * inputSize

        // Write R, G, B channels sequentially (NCHW format)
        for (i in pixels.indices) {
            buffer.put(i, ((pixels[i] shr 16) and 0xFF) / 255f)               // R
            buffer.put(i + channelSize, ((pixels[i] shr 8) and 0xFF) / 255f)  // G
            buffer.put(i + 2 * channelSize, (pixels[i] and 0xFF) / 255f)      // B
        }

        buffer.rewind()
        return Pair(buffer, PadInfo(scale, padX, padY))
    }

    /**
     * Post-process YOLOv8-pose output tensor.
     * Output shape: [1, 56, 8400] transposed to [8400, 56] for easier parsing.
     */
    private fun postprocess(
        outputData: FloatBuffer,
        padInfo: PadInfo,
        originalWidth: Int,
        originalHeight: Int
    ): List<PoseDetectionResult> {
        val numDetections = 8400
        val numChannels = 56 // 4 bbox + 1 conf + 51 keypoints (17 * 3)

        // Read transposed: output is [1, 56, 8400], we need [8400, 56]
        val detections = Array(numDetections) { FloatArray(numChannels) }
        for (c in 0 until numChannels) {
            for (d in 0 until numDetections) {
                detections[d][c] = outputData.get(c * numDetections + d)
            }
        }

        // Filter by confidence
        val candidates = mutableListOf<PoseDetectionResult>()
        for (det in detections) {
            val conf = det[4]
            if (conf < confThreshold) continue

            // Convert cx, cy, w, h → x1, y1, x2, y2 (in 640×640 space)
            val cx = det[0]; val cy = det[1]; val w = det[2]; val h = det[3]
            val x1 = cx - w / 2; val y1 = cy - h / 2
            val x2 = cx + w / 2; val y2 = cy + h / 2

            // Remove letterbox padding and normalize to [0, 1]
            val bx1 = ((x1 - padInfo.padX) / padInfo.scale / originalWidth).coerceIn(0f, 1f)
            val by1 = ((y1 - padInfo.padY) / padInfo.scale / originalHeight).coerceIn(0f, 1f)
            val bx2 = ((x2 - padInfo.padX) / padInfo.scale / originalWidth).coerceIn(0f, 1f)
            val by2 = ((y2 - padInfo.padY) / padInfo.scale / originalHeight).coerceIn(0f, 1f)

            // Parse 17 keypoints (each has x, y, visibility starting at index 5)
            val keypoints = mutableListOf<Keypoint>()
            for (k in 0 until NUM_KEYPOINTS) {
                val kx = det[5 + k * 3]
                val ky = det[5 + k * 3 + 1]
                val kConf = det[5 + k * 3 + 2]

                // Remove padding and normalize
                val normalizedX = ((kx - padInfo.padX) / padInfo.scale / originalWidth).coerceIn(0f, 1f)
                val normalizedY = ((ky - padInfo.padY) / padInfo.scale / originalHeight).coerceIn(0f, 1f)

                keypoints.add(Keypoint(normalizedX, normalizedY, kConf))
            }

            candidates.add(PoseDetectionResult(
                boundingBox = floatArrayOf(bx1, by1, bx2, by2),
                score = conf,
                keypoints = keypoints
            ))
        }

        // Apply NMS
        return applyNMS(candidates)
    }

    /**
     * Non-Maximum Suppression to remove overlapping detections.
     */
    private fun applyNMS(detections: List<PoseDetectionResult>): List<PoseDetectionResult> {
        if (detections.isEmpty()) return emptyList()

        val sorted = detections.sortedByDescending { it.score }.toMutableList()
        val result = mutableListOf<PoseDetectionResult>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeFirst()
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
        Log.d(TAG, "🔄 Closing YOLOv8 Pose Detector")
        ortSession.close()
        ortEnv.close()
    }

    private data class PadInfo(val scale: Float, val padX: Int, val padY: Int)
}
