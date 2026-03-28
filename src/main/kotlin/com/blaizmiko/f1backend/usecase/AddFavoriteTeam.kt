package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.domain.model.NotFoundException
import com.blaizmiko.f1backend.domain.repository.FavoriteRepository
import com.blaizmiko.f1backend.domain.repository.TeamRepository
import java.time.Instant
import java.util.UUID

class AddFavoriteTeam(
    private val favoriteRepository: FavoriteRepository,
    private val teamRepository: TeamRepository,
) {
    suspend fun execute(
        userId: UUID,
        teamId: String,
    ): Pair<Boolean, Instant> {
        teamRepository.findByTeamId(teamId)
            ?: throw NotFoundException("Team '$teamId' not found")
        return favoriteRepository.addFavoriteTeam(userId, teamId)
    }
}
