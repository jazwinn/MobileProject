package com.jazwinn.fitnesstracker.data.repository

import android.graphics.Bitmap
import com.jazwinn.fitnesstracker.data.ai.GeminiService
import com.jazwinn.fitnesstracker.domain.ml.ImageClassifier
import com.jazwinn.fitnesstracker.domain.repository.MachineGuideRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MachineGuideRepositoryImpl(
    private val classifier: ImageClassifier,
    private val geminiService: GeminiService
) : MachineGuideRepository {

    override suspend fun identifyMachine(bitmap: Bitmap): Result<String> = withContext(Dispatchers.Default) {
        try {
            val results = classifier.classify(bitmap)
            if (results.isNotEmpty()) {
                // Return the top result's label
                Result.success(results.first().label)
            } else {
                Result.failure(Exception("No machine identified"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGuideForMachine(machineName: String): Result<String> {
        return geminiService.generateGuide(machineName)
    }
}
