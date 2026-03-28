package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.domain.model.Driver
import com.blaizmiko.f1backend.domain.model.Team
import com.blaizmiko.f1backend.domain.repository.DriverRepository
import com.blaizmiko.f1backend.domain.repository.FavoriteRepository
import com.blaizmiko.f1backend.domain.repository.TeamRepository
import java.time.Instant
import java.util.UUID

data class FavoriteDriverDetail(
    val driver: Driver,
    val team: Team?,
    val addedAt: Instant,
)

class GetFavoriteDrivers(
    private val favoriteRepository: FavoriteRepository,
    private val driverRepository: DriverRepository,
    private val teamRepository: TeamRepository,
) {
    suspend fun execute(userId: UUID): List<FavoriteDriverDetail> {
        val favorites = favoriteRepository.getFavoriteDriverIds(userId)
        if (favorites.isEmpty()) return emptyList()
        val allDrivers = driverRepository.findAll().associateBy { it.id }
        val allTeams = teamRepository.findAll().associateBy { it.id }
        return favorites.mapNotNull { (driverId, addedAt) ->
            val driver = allDrivers[driverId] ?: return@mapNotNull null
            FavoriteDriverDetail(driver, driver.teamId?.let { allTeams[it] }, addedAt)
        }
    }
}
