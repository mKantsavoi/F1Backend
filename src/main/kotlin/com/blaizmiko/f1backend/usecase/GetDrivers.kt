package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.domain.model.Driver
import com.blaizmiko.f1backend.domain.repository.DriverRepository

class GetDrivers(
    private val repository: DriverRepository,
) {
    suspend fun execute(): List<Driver> = repository.findAll()
}
