package com.jazwinn.fitnesstracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jazwinn.fitnesstracker.data.local.dao.StepsDao
import com.jazwinn.fitnesstracker.data.local.dao.UserProfileDao
import com.jazwinn.fitnesstracker.data.local.dao.WorkoutDao
import com.jazwinn.fitnesstracker.data.local.entity.DailyStepsEntity
import com.jazwinn.fitnesstracker.data.local.entity.UserProfileEntity
import com.jazwinn.fitnesstracker.data.local.entity.WorkoutEntity

@Database(entities = [DailyStepsEntity::class, WorkoutEntity::class, UserProfileEntity::class], version = 4, exportSchema = false)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun stepsDao(): StepsDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun userProfileDao(): UserProfileDao
}


