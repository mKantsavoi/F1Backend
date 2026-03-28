package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.domain.repository.FavoriteRepository
import java.util.UUID

class CheckDriverFavorite(
    private val favoriteRepository: FavoriteRepository,
) {
    suspend fun execute(
        userId: UUID,
        driverId: String,
    ): Boolean = favoriteRepository.isDriverFavorite(userId, driverId)
}
