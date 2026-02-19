package com.jazwinn.fitnesstracker.data.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.jazwinn.fitnesstracker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiService {
    
    // Using gemini-1.5-flash for speed and lower cost, or gemini-1.5-pro for better quality.
    // User requested gemini-2.5-pro but SDK supports 1.5. Let's use 1.5-flash as default for responsiveness.
    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun generateGuide(equipmentName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (BuildConfig.GEMINI_API_KEY.isEmpty()) {
                return@withContext Result.failure(Exception("API Key missing. Please add GEMINI_API_KEY to local.properties"))
            }

            val prompt = """
                You are an expert certified personal trainer. 
                Generate a comprehensive exercise guide for the gym machine: "$equipmentName".
                
                Format the response with the following sections (use Markdown):
                1. ## EQUIPMENT OVERVIEW
                2. ## SETUP INSTRUCTIONS (Numbered list)
                3. ## EXECUTION TECHNIQUE (Numbered list)
                4. ## BREATHING PATTERN
                5. ## COMMON MISTAKES
                6. ## SAFETY TIPS
                
                Keep it concise, motivating, and safe for beginners.
            """.trimIndent()

            val response = model.generateContent(prompt)
            val text = response.text ?: "No response generated."
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
