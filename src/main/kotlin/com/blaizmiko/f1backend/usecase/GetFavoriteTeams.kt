package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.domain.model.Driver
import com.blaizmiko.f1backend.domain.model.Team
import com.blaizmiko.f1backend.domain.repository.DriverRepository
import com.blaizmiko.f1backend.domain.repository.FavoriteRepository
import com.blaizmiko.f1backend.domain.repository.TeamRepository
import java.time.Instant
import java.util.UUID

data class FavoriteTeamDetail(
    val team: Team,
    val drivers: List<Driver>,
    val addedAt: Instant,
)

class GetFavoriteTeams(
    private val favoriteRepository: FavoriteRepository,
    private val teamRepository: TeamRepository,
    private val driverRepository: DriverRepository,
) {
    suspend fun execute(userId: UUID): List<FavoriteTeamDetail> {
        val favorites = favoriteRepository.getFavoriteTeamIds(userId)
        if (favorites.isEmpty()) return emptyList()
        val allTeams = teamRepository.findAll().associateBy { it.id }
        val allDrivers = driverRepository.findAll()
        return favorites.mapNotNull { (teamId, addedAt) ->
            val team = allTeams[teamId] ?: return@mapNotNull null
            val teamDrivers = allDrivers.filter { it.teamId == teamId }
            FavoriteTeamDetail(team, teamDrivers, addedAt)
        }
    }
}
