package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.domain.repository.DriverRepository
import com.blaizmiko.f1backend.domain.repository.FavoriteRepository
import com.blaizmiko.f1backend.domain.repository.TeamRepository
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class PersonalizedFeedResult(
    val favoriteDrivers: List<FeedDriverResult>,
    val favoriteTeams: List<FeedTeamResult>,
    val relevantNews: List<String>,
)

data class FeedDriverResult(
    val driverId: String,
    val code: String,
    val photoUrl: String?,
    val championshipPosition: Int?,
    val championshipPoints: Double?,
    val lastRaceName: String?,
    val lastRacePosition: Int?,
    val lastRacePoints: Double?,
)

data class FeedTeamResult(
    val teamId: String,
    val name: String,
    val championshipPosition: Int?,
    val championshipPoints: Double?,
)

class GetPersonalizedFeed(
    private val favoriteRepository: FavoriteRepository,
    private val driverRepository: DriverRepository,
    private val teamRepository: TeamRepository,
    private val getDriverStandings: GetDriverStandings,
    private val getConstructorStandings: GetConstructorStandings,
    private val getRaceResults: GetRaceResults,
) {
    companion object {
        private const val CACHE_TTL_SECONDS = 30L
    }

    private data class CachedFeed(
        val result: PersonalizedFeedResult,
        val expiresAt: Instant,
    )

    private val cache = ConcurrentHashMap<UUID, CachedFeed>()

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    suspend fun execute(userId: UUID): PersonalizedFeedResult {
        val cached = cache[userId]
        if (cached != null && Instant.now().isBefore(cached.expiresAt)) {
            return cached.result
        }

        val favoriteDriverIds = favoriteRepository.getFavoriteDriverIds(userId).map { it.first }.toSet()
        val favoriteTeamIds = favoriteRepository.getFavoriteTeamIds(userId).map { it.first }.toSet()

        if (favoriteDriverIds.isEmpty() && favoriteTeamIds.isEmpty()) {
            val empty = PersonalizedFeedResult(emptyList(), emptyList(), emptyList())
            cacheResult(userId, empty)
            return empty
        }

        val driverStandings =
            try {
                getDriverStandings.execute("current")
            } catch (_: Exception) {
                null
            }

        val constructorStandings =
            try {
                getConstructorStandings.execute("current")
            } catch (_: Exception) {
                null
            }

        val lastRaceResults =
            if (driverStandings != null && driverStandings.round > 0) {
                try {
                    getRaceResults.execute("current", driverStandings.round)
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }

        val feedDrivers =
            favoriteDriverIds.mapNotNull { driverId ->
                val driver = driverRepository.findByDriverId(driverId) ?: return@mapNotNull null
                val standing = driverStandings?.standings?.find { it.driverId == driverId }
                val raceResult = lastRaceResults?.results?.find { it.driverId == driverId }
                FeedDriverResult(
                    driverId = driverId,
                    code = driver.code,
                    photoUrl = driver.photoUrl,
                    championshipPosition = standing?.position,
                    championshipPoints = standing?.points,
                    lastRaceName = lastRaceResults?.raceName,
                    lastRacePosition = raceResult?.position,
                    lastRacePoints = raceResult?.points,
                )
            }

        val feedTeams =
            favoriteTeamIds.mapNotNull { teamId ->
                val team = teamRepository.findByTeamId(teamId) ?: return@mapNotNull null
                val standing = constructorStandings?.standings?.find { it.teamId == teamId }
                FeedTeamResult(
                    teamId = teamId,
                    name = team.name,
                    championshipPosition = standing?.position,
                    championshipPoints = standing?.points,
                )
            }

        val result = PersonalizedFeedResult(feedDrivers, feedTeams, emptyList())
        cacheResult(userId, result)
        return result
    }

    private fun cacheResult(
        userId: UUID,
        result: PersonalizedFeedResult,
    ) {
        cache[userId] = CachedFeed(result, Instant.now().plusSeconds(CACHE_TTL_SECONDS))
    }
}
