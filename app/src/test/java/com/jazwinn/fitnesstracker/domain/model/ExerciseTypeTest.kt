package com.jazwinn.fitnesstracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseTypeTest {

    @Test
    fun testPushUpDisplayName() {
        assertEquals("Push-Ups", ExerciseType.PUSH_UP.getDisplayName())
    }

    @Test
    fun testSitUpDisplayName() {
        assertEquals("Sit-Ups", ExerciseType.SIT_UP.getDisplayName())
    }

    @Test
    fun testEnumContainsExactlyTwoValues() {
        assertEquals(2, ExerciseType.values().size)
    }

    @Test
    fun testEnumValueOf() {
        assertEquals(ExerciseType.PUSH_UP, ExerciseType.valueOf("PUSH_UP"))
        assertEquals(ExerciseType.SIT_UP, ExerciseType.valueOf("SIT_UP"))
    }
}
