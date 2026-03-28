package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.domain.model.DriverWithTeam
import com.blaizmiko.f1backend.domain.model.NotFoundException
import com.blaizmiko.f1backend.domain.repository.DriverRepository

class GetDriverDetail(
    private val repository: DriverRepository,
) {
    suspend fun execute(driverId: String): DriverWithTeam =
        repository.findByDriverId(driverId)
            ?: throw NotFoundException("Driver not found")
}
