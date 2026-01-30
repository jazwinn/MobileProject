package com.jazwinn.fitnesstracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_steps")
data class DailyStepsEntity(
    @PrimaryKey
    val date: String, // Format: YYYY-MM-DD
    val stepCount: Long = 0,
    val distance: Float = 0f, // in meters
    val calories: Float = 0f, // in kcal
    val activeTime: Long = 0 // in milliseconds
)
