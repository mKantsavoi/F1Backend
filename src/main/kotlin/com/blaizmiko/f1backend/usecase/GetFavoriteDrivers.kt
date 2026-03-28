package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.domain.model.DriverWithTeam
import com.blaizmiko.f1backend.domain.repository.DriverRepository
import com.blaizmiko.f1backend.domain.repository.FavoriteRepository
import java.time.Instant
import java.util.UUID

data class FavoriteDriverDetail(
    val driver: DriverWithTeam,
    val addedAt: Instant,
)

class GetFavoriteDrivers(
    private val favoriteRepository: FavoriteRepository,
    private val driverRepository: DriverRepository,
) {
    suspend fun execute(userId: UUID): List<FavoriteDriverDetail> {
        val favorites = favoriteRepository.getFavoriteDriverIds(userId)
        return favorites.mapNotNull { (driverId, addedAt) ->
            val driver = driverRepository.findByDriverId(driverId) ?: return@mapNotNull null
            FavoriteDriverDetail(driver, addedAt)
        }
    }
}
