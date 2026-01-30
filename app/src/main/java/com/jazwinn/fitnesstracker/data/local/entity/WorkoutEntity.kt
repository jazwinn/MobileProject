package com.jazwinn.fitnesstracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val type: String, // PUSH_UP, SIT_UP
    val reps: Int,
    val durationSeconds: Long = 0 // Optional
)
