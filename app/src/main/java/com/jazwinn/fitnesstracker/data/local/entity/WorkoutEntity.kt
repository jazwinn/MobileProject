package com.jazwinn.fitnesstracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val type: String, // PUSH_UP, SIT_UP, OUTDOOR_RUN
    val reps: Int = 0,
    val durationSeconds: Long = 0,
    val distanceKm: Float? = null,
    val paceMinPerKm: Float? = null
)
