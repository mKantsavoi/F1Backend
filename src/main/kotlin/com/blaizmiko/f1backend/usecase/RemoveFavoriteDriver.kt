package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.domain.repository.FavoriteRepository
import java.util.UUID

class RemoveFavoriteDriver(
    private val favoriteRepository: FavoriteRepository,
) {
    suspend fun execute(
        userId: UUID,
        driverId: String,
    ) {
        favoriteRepository.removeFavoriteDriver(userId, driverId)
    }
}
