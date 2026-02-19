package com.jazwinn.fitnesstracker.domain.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.TensorLabel
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class ClassificationResult(
    val label: String,
    val score: Float
)

class ImageClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private val inputSize = 640 // YOLOv8 standard: 640x640
    private lateinit var imageProcessor: ImageProcessor

    companion object {
        private const val MODEL_NAME = "gym_machines.tflite"
        private const val LABELS_FILE = "labels.txt"
        private const val TAG = "ImageClassifier"
    }

    init {
        setupClassifier()
    }

    private fun setupClassifier() {
        try {
            // Load labels
            labels = FileUtil.loadLabels(context, LABELS_FILE)
            Log.d(TAG, "Loaded ${labels.size} labels")

            // Load model
            val model = loadModelFile(context, MODEL_NAME)
            val options = Interpreter.Options()
            interpreter = Interpreter(model, options)
            Log.d(TAG, "Model loaded successfully")

            // Init image processor
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
                .add(org.tensorflow.lite.support.common.ops.NormalizeOp(0f, 255f)) // Normalize [0, 255] -> [0, 1]
                .build()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize classifier", e)
        }
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun classify(bitmap: Bitmap): List<ClassificationResult> {
        val interpreter = interpreter ?: return emptyList()

        // Preprocess
        var tensorImage = TensorImage(org.tensorflow.lite.DataType.FLOAT32)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // Output buffer: [1, 300, 6]
        // Likely: [ymin, xmin, ymax, xmax, score, class_id] or similar 6 values per detection
        val outputBuffer = Array(1) { Array(300) { FloatArray(6) } }
        
        // Run
        interpreter.run(tensorImage.buffer, outputBuffer)
        
        val detections = ArrayList<ClassificationResult>()
        
        // Iterate through 300 detections
        for (i in 0 until 300) {
            val detection = outputBuffer[0][i]
            val score = detection[4] // Assuming index 4 is score
            val classId = detection[5].toInt() // Assuming index 5 is class_id
            
            // Filter low confidence
            if (score > 0.5f && classId >= 0 && classId < labels.size) {
                 val label = labels[classId]
                 detections.add(ClassificationResult(label, score))
            }
        }
        
        // Sort by score and take top result
        return detections.sortedByDescending { it.score }.take(1)
    }
    
    fun close() {
        interpreter?.close()
    }
}
