package com.jazwinn.fitnesstracker.data.repository

import com.jazwinn.fitnesstracker.data.local.dao.StepsDao
import com.jazwinn.fitnesstracker.data.local.entity.DailyStepsEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StepsRepository @Inject constructor(
    private val stepsDao: StepsDao,
    private val userProfileDao: com.jazwinn.fitnesstracker.data.local.dao.UserProfileDao
) {
    private val todayDate: String
        get() = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

    fun getTodaySteps(): Flow<DailyStepsEntity?> {
        return stepsDao.getStepsForDate(todayDate)
    }

    suspend fun updateSteps(stepCount: Long) {
        val currentEntity = stepsDao.getStepsForDateOneShot(todayDate) ?: DailyStepsEntity(date = todayDate)
        
        // Fetch user profile logic
        // Since we are in suspend function, we can do OneShot query if we had it, or collect Flow.
        // For MVP simplicity, we might default or try to get it.
        // Let's assume we default if data missing.
        // Ideally we cache profile in Repository or use Flow.
        // But since Flow is hard to get synchronously here without blocking, we'll implement a one-shot helper in DAO later or rely on defaults.
        // Actually, we can add a one-shot in DAO. I will assume it exists or use defaults for now.
        
        val heightCm = 170f // Default
        val weightKg = 70f // Default
        
        // Improve: Add getProfileOneShot to Dao or just use these defaults until then.
        
        val strideLengthMeters = (heightCm * 0.415f) / 100 // Approximation
        val caloriesPerStep = (weightKg * 2.2f) * 0.57f / 1000 // Very rough approx
        
        val distance = stepCount * strideLengthMeters
        val calories = stepCount * caloriesPerStep

        val updatedEntity = currentEntity.copy(
            stepCount = stepCount,
            distance = distance,
            calories = calories
        )
        stepsDao.insertOrUpdateSteps(updatedEntity)
    }
}
