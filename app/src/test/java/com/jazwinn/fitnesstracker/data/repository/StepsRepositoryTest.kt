package com.jazwinn.fitnesstracker.data.repository

import com.jazwinn.fitnesstracker.data.local.dao.StepsDao
import com.jazwinn.fitnesstracker.data.local.dao.UserProfileDao
import com.jazwinn.fitnesstracker.data.local.entity.DailyStepsEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class StepsRepositoryTest {

    private lateinit var stepsDao: StepsDao
    private lateinit var userProfileDao: UserProfileDao
    private lateinit var repository: StepsRepository

    @Before
    fun setup() {
        stepsDao = mock()
        userProfileDao = mock()
        repository = StepsRepository(stepsDao, userProfileDao)
    }

    // -------------------------------------------------------------------------
    // updateSteps — new entity (no prior record)
    // -------------------------------------------------------------------------

    @Test
    fun testUpdateSteps_newEntity_calculatesDistanceCorrectly() = runBlocking {
        // No existing record for today
        whenever(stepsDao.getStepsForDateOneShot(any())).thenReturn(null)

        repository.updateSteps(10_000L)

        val captor = argumentCaptor<DailyStepsEntity>()
        verify(stepsDao).insertOrUpdateSteps(captor.capture())
        val saved = captor.firstValue

        assertEquals(10_000L, saved.stepCount)

        val expectedDistance = 10_000L * (170f * 0.415f / 100f)
        assertEquals(expectedDistance, saved.distance, 0.01f)
    }

    @Test
    fun testUpdateSteps_newEntity_calculatesCaloriesCorrectly() = runBlocking {
        whenever(stepsDao.getStepsForDateOneShot(any())).thenReturn(null)

        repository.updateSteps(5_000L)

        val captor = argumentCaptor<DailyStepsEntity>()
        verify(stepsDao).insertOrUpdateSteps(captor.capture())
        val saved = captor.firstValue

        val expectedCalories = 5_000L * (70f * 2.2f * 0.57f / 1000f)
        assertEquals(expectedCalories, saved.calories, 0.01f)
    }

    // -------------------------------------------------------------------------
    // updateSteps — existing entity is preserved and updated
    // -------------------------------------------------------------------------

    @Test
    fun testUpdateSteps_existingEntity_updatesStepCount() = runBlocking {
        val existing = DailyStepsEntity(
            date = "2026-02-21",
            stepCount = 3_000L,
            distance = 2.1f,
            calories = 50f
        )
        whenever(stepsDao.getStepsForDateOneShot(any())).thenReturn(existing)

        repository.updateSteps(8_000L)

        val captor = argumentCaptor<DailyStepsEntity>()
        verify(stepsDao).insertOrUpdateSteps(captor.capture())
        val saved = captor.firstValue

        // Step count should be fully replaced by the new value
        assertEquals(8_000L, saved.stepCount)
    }

    @Test
    fun testUpdateSteps_existingEntity_preservesDate() = runBlocking {
        val existing = DailyStepsEntity(date = "2026-02-21", stepCount = 1_000L)
        whenever(stepsDao.getStepsForDateOneShot(any())).thenReturn(existing)

        repository.updateSteps(2_000L)

        val captor = argumentCaptor<DailyStepsEntity>()
        verify(stepsDao).insertOrUpdateSteps(captor.capture())

        // The date field should stay the same as the existing entity
        assertEquals("2026-02-21", captor.firstValue.date)
    }

    // -------------------------------------------------------------------------
    // getTodaySteps — delegates to DAO
    // -------------------------------------------------------------------------

    @Test
    fun testGetTodaySteps_delegatesToDao() {
        val fakeEntity = DailyStepsEntity(date = "2026-02-21", stepCount = 500L)
        whenever(stepsDao.getStepsForDate(any())).thenReturn(flowOf(fakeEntity))

        val flow = repository.getTodaySteps()

        // Verify DAO was called — the Flow returned should be the one from the DAO
        verify(stepsDao).getStepsForDate(any())
    }
}
