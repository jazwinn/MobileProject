package com.jazwinn.fitnesstracker.ui.viewmodel

import com.jazwinn.fitnesstracker.data.local.dao.BmiHistoryDao
import com.jazwinn.fitnesstracker.data.local.dao.UserProfileDao
import com.jazwinn.fitnesstracker.data.local.entity.UserProfileEntity
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
class ProfileViewModelTest {

    private lateinit var userProfileDao: UserProfileDao
    private lateinit var bmiHistoryDao: BmiHistoryDao
    private lateinit var viewModel: ProfileViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userProfileDao = mock()
        bmiHistoryDao = mock()
        // Provide initial flow for loadProfile()
        whenever(userProfileDao.getUserProfile()).thenReturn(flowOf(null))
        viewModel = ProfileViewModel(userProfileDao, bmiHistoryDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateHeight and updateWeight should calculate correct BMI`() = runTest {
        // Normal weight: 70kg, 175cm -> 22.9
        viewModel.updateHeight("175")
        viewModel.updateWeight("70")
        
        testDispatcher.scheduler.runCurrent()
        
        val state = viewModel.uiState.value
        assertEquals("22.9", state.bmi.value)
        assertEquals("Normal", state.bmi.category)
        assertEquals(0xFF2ECC71L, state.bmi.color)
    }

    @Test
    fun `underweight BMI calculation`() = runTest {
        // Underweight: 50kg, 180cm -> 15.4
        viewModel.updateHeight("180")
        viewModel.updateWeight("50")
        
        testDispatcher.scheduler.runCurrent()
        
        val state = viewModel.uiState.value
        assertEquals("15.4", state.bmi.value)
        assertEquals("Underweight", state.bmi.category)
        assertEquals(0xFF3498DBL, state.bmi.color)
    }

    @Test
    fun `obese BMI calculation`() = runTest {
        // Obese: 100kg, 170cm -> 34.6
        viewModel.updateHeight("170")
        viewModel.updateWeight("100")
        
        testDispatcher.scheduler.runCurrent()
        
        val state = viewModel.uiState.value
        assertEquals("34.6", state.bmi.value)
        assertEquals("Obese", state.bmi.category)
        assertEquals(0xFFE74C3CL, state.bmi.color)
    }

    @Test
    fun `invalid inputs should return error state and default BMI`() = runTest {
        viewModel.updateHeight("0")
        viewModel.updateWeight("0.0")
        
        testDispatcher.scheduler.runCurrent()
        
        val state = viewModel.uiState.value
        assertEquals("Must be greater than 0", state.heightError)
        assertEquals("Must be greater than 0", state.weightError)
        assertEquals("--", state.bmi.value)
        assertEquals("Unknown", state.bmi.category)
    }

    @Test
    fun `empty inputs should not show error but should have default BMI`() = runTest {
        viewModel.updateHeight("")
        viewModel.updateWeight("")
        
        testDispatcher.scheduler.runCurrent()
        
        val state = viewModel.uiState.value
        assertEquals(null, state.heightError)
        assertEquals(null, state.weightError)
        assertEquals("--", state.bmi.value)
    }
}
