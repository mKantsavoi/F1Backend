package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.domain.model.NotFoundException
import com.blaizmiko.f1backend.domain.repository.DriverRepository
import com.blaizmiko.f1backend.domain.repository.FavoriteRepository
import java.time.Instant
import java.util.UUID

class AddFavoriteDriver(
    private val favoriteRepository: FavoriteRepository,
    private val driverRepository: DriverRepository,
) {
    suspend fun execute(
        userId: UUID,
        driverId: String,
    ): Pair<Boolean, Instant> {
        driverRepository.findByDriverId(driverId)
            ?: throw NotFoundException("Driver '$driverId' not found")
        return favoriteRepository.addFavoriteDriver(userId, driverId)
    }
}
