package com.jazwinn.fitnesstracker.domain.model

enum class ExerciseType {
    PUSH_UP,
    SIT_UP;

    fun getDisplayName(): String {
        return when (this) {
            PUSH_UP -> "Push-Ups"
            SIT_UP -> "Sit-Ups"
        }
    }
}
