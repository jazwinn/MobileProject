package com.jazwinn.fitnesstracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 0, // Single user
    val name: String = "User",
    val heightCm: Float = 170f,
    val weightKg: Float = 70f,
    val age: Int = 25,
    val dailyStepGoal: Int = 10000,
    val profilePictureUri: String? = null
)
