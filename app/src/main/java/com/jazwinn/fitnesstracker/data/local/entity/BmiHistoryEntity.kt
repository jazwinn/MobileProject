package com.jazwinn.fitnesstracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bmi_history")
data class BmiHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String, // Format: YYYY-MM-DD
    val bmiValue: Float,
    val weightKg: Float,
    val heightCm: Float
)
