package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.domain.repository.FavoriteRepository
import java.util.UUID

class CheckTeamFavorite(
    private val favoriteRepository: FavoriteRepository,
) {
    suspend fun execute(
        userId: UUID,
        teamId: String,
    ): Boolean = favoriteRepository.isTeamFavorite(userId, teamId)
}
