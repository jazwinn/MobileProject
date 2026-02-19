package com.jazwinn.fitnesstracker.domain.repository

import android.graphics.Bitmap

interface MachineGuideRepository {
    suspend fun identifyMachine(bitmap: Bitmap): Result<String>
    suspend fun getGuideForMachine(machineName: String): Result<String>
}
