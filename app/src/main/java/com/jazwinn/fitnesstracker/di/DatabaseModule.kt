package com.jazwinn.fitnesstracker.di

import android.content.Context
import androidx.room.Room
import com.jazwinn.fitnesstracker.data.local.FitnessDatabase
import com.jazwinn.fitnesstracker.data.local.dao.BmiHistoryDao
import com.jazwinn.fitnesstracker.data.local.dao.StepsDao
import com.jazwinn.fitnesstracker.data.local.dao.UserProfileDao
import com.jazwinn.fitnesstracker.data.local.dao.WorkoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FitnessDatabase {
        return Room.databaseBuilder(
            context,
            FitnessDatabase::class.java,
            "fitness_database"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideStepsDao(database: FitnessDatabase): StepsDao {
        return database.stepsDao()
    }

    @Provides
    fun provideWorkoutDao(database: FitnessDatabase): WorkoutDao {
        return database.workoutDao()
    }

    @Provides
    fun provideUserProfileDao(database: FitnessDatabase): UserProfileDao {
        return database.userProfileDao()
    }

    @Provides
    @Singleton
    fun provideBmiHistoryDao(database: FitnessDatabase): BmiHistoryDao {
        return database.bmiHistoryDao()
    }
}
