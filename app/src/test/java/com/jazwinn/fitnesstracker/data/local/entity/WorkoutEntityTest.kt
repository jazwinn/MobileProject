package com.jazwinn.fitnesstracker.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutEntityTest {

    @Test
    fun testDefaultValues() {
        val entity = WorkoutEntity(
            timestamp = 1_000_000L,
            type = "PUSH_UP"
        )
        assertEquals(0L, entity.id)
        assertEquals(0, entity.reps)
        assertEquals(0L, entity.durationSeconds)
        assertNull(entity.distanceKm)
        assertNull(entity.paceMinPerKm)
    }

    @Test
    fun testCopy_updatesReps() {
        val original = WorkoutEntity(timestamp = 1_000_000L, type = "SIT_UP")
        val updated = original.copy(reps = 15)
        assertEquals(15, updated.reps)
        assertEquals("SIT_UP", updated.type)
    }

    @Test
    fun testEquality_sameFieldsAreEqual() {
        val a = WorkoutEntity(id = 1L, timestamp = 500L, type = "PUSH_UP", reps = 10)
        val b = WorkoutEntity(id = 1L, timestamp = 500L, type = "PUSH_UP", reps = 10)
        assertEquals(a, b)
    }

    @Test
    fun testEquality_differentRepsAreNotEqual() {
        val a = WorkoutEntity(id = 1L, timestamp = 500L, type = "PUSH_UP", reps = 5)
        val b = WorkoutEntity(id = 1L, timestamp = 500L, type = "PUSH_UP", reps = 10)
        assertNotEquals(a, b)
    }

    @Test
    fun testOutdoorRunFields() {
        val run = WorkoutEntity(
            timestamp = 2_000_000L,
            type = "OUTDOOR_RUN",
            durationSeconds = 1800L,
            distanceKm = 5.0f,
            paceMinPerKm = 6.0f
        )
        assertEquals(5.0f, run.distanceKm)
        assertEquals(6.0f, run.paceMinPerKm)
        assertEquals(1800L, run.durationSeconds)
    }
}
