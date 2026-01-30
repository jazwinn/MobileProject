package com.jazwinn.fitnesstracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jazwinn.fitnesstracker.data.local.entity.DailyStepsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StepsDao {
    @Query("SELECT * FROM daily_steps WHERE date = :date")
    fun getStepsForDate(date: String): Flow<DailyStepsEntity?>

    @Query("SELECT * FROM daily_steps WHERE date = :date")
    suspend fun getStepsForDateOneShot(date: String): DailyStepsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSteps(steps: DailyStepsEntity)

    @Query("SELECT * FROM daily_steps ORDER BY date DESC")
    fun getAllStepsHistory(): Flow<List<DailyStepsEntity>>
}
