package com.jazwinn.fitnesstracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jazwinn.fitnesstracker.data.local.entity.BmiHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BmiHistoryDao {
    @Query("SELECT * FROM bmi_history ORDER BY date ASC")
    fun getAllBmiHistory(): Flow<List<BmiHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBmiRecord(record: BmiHistoryEntity)

    @Delete
    suspend fun deleteBmiRecord(record: BmiHistoryEntity)
    
    @Query("DELETE FROM bmi_history WHERE date = :date")
    suspend fun deleteBmiRecordByDate(date: String)
}
