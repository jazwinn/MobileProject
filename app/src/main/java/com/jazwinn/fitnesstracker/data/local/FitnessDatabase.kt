package com.jazwinn.fitnesstracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jazwinn.fitnesstracker.data.local.dao.BmiHistoryDao
import com.jazwinn.fitnesstracker.data.local.dao.StepsDao
import com.jazwinn.fitnesstracker.data.local.dao.UserProfileDao
import com.jazwinn.fitnesstracker.data.local.dao.WorkoutDao
import com.jazwinn.fitnesstracker.data.local.entity.BmiHistoryEntity
import com.jazwinn.fitnesstracker.data.local.entity.DailyStepsEntity
import com.jazwinn.fitnesstracker.data.local.entity.UserProfileEntity
import com.jazwinn.fitnesstracker.data.local.entity.WorkoutEntity

@Database(
    entities = [
        DailyStepsEntity::class, 
        WorkoutEntity::class, 
        UserProfileEntity::class,
        BmiHistoryEntity::class
    ], 
    version = 6, 
    exportSchema = false
)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun stepsDao(): StepsDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun bmiHistoryDao(): BmiHistoryDao
}
