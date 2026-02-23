package com.jazwinn.fitnesstracker.ui.viewmodel

import com.jazwinn.fitnesstracker.data.local.dao.BmiHistoryDao
import com.jazwinn.fitnesstracker.data.local.dao.StepsDao
import com.jazwinn.fitnesstracker.data.local.dao.WorkoutDao
import com.jazwinn.fitnesstracker.data.local.entity.BmiHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private lateinit var stepsDao: StepsDao
    private lateinit var workoutDao: WorkoutDao
    private lateinit var bmiHistoryDao: BmiHistoryDao
    private lateinit var viewModel: StatsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        stepsDao = mock()
        workoutDao = mock()
        bmiHistoryDao = mock()
        
        whenever(stepsDao.getAllStepsHistory()).thenReturn(flowOf(emptyList()))
        whenever(workoutDao.getAllWorkouts()).thenReturn(flowOf(emptyList()))
        whenever(bmiHistoryDao.getAllBmiHistory()).thenReturn(flowOf(emptyList()))
        
        viewModel = StatsViewModel(stepsDao, workoutDao, bmiHistoryDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `bmiChartPoints should be normalized correctly`() = runTest {
        val history = listOf(
            BmiHistoryEntity(1, "2023-10-01", 20f, 60f, 173f),
            BmiHistoryEntity(2, "2023-10-02", 30f, 90f, 173f)
        )
        whenever(bmiHistoryDao.getAllBmiHistory()).thenReturn(flowOf(history))
        
        val vm = StatsViewModel(stepsDao, workoutDao, bmiHistoryDao)
        testDispatcher.scheduler.runCurrent()
        
        val state = vm.uiState.value
        assertEquals(2, state.bmiChartPoints.size)
        assertEquals(0f, state.bmiChartPoints[0], 0.01f)
        assertEquals(1f, state.bmiChartPoints[1], 0.01f)
    }

    @Test
    fun `bmiChartPoints should be 0_5 for single value`() = runTest {
        val history = listOf(
            BmiHistoryEntity(1, "2023-10-01", 25f, 75f, 173f)
        )
        whenever(bmiHistoryDao.getAllBmiHistory()).thenReturn(flowOf(history))
        
        val vm = StatsViewModel(stepsDao, workoutDao, bmiHistoryDao)
        testDispatcher.scheduler.runCurrent()
        
        val state = vm.uiState.value
        assertEquals(1, state.bmiChartPoints.size)
        assertEquals(0.5f, state.bmiChartPoints[0], 0.01f)
    }
}
