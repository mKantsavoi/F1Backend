package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.adapter.port.CacheProvider
import com.blaizmiko.f1backend.domain.model.DriverStanding
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.StandingsData
import com.blaizmiko.f1backend.domain.port.StandingsDataSource
import com.blaizmiko.f1backend.infrastructure.cache.CacheSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.Year
import java.util.concurrent.ConcurrentHashMap

data class DriverStandingsResult(
    val season: String,
    val round: Int,
    val standings: List<DriverStanding>,
    val isStale: Boolean,
)

class GetDriverStandings(
    private val cacheProvider: CacheProvider,
    private val dataSource: StandingsDataSource,
) {
    companion object {
        private const val RETRY_THROTTLE_SECONDS = 60L
    }

    private val fetchMutexes = ConcurrentHashMap<String, Mutex>()
    private val lastFailedAttempt = ConcurrentHashMap<String, Instant>()

    suspend fun execute(season: String = "current"): DriverStandingsResult {
        val spec = specForSeason(season)
        val cache = cacheProvider.getCache<String, StandingsData<DriverStanding>>(spec)
        val quickCached = cache.getIfPresent(season)
        if (quickCached != null) {
            return freshResult(quickCached)
        }

        val mutex = fetchMutexes.getOrPut(season) { Mutex() }
        return mutex.withLock { fetchOrServeCached(season) }
    }

    private suspend fun fetchOrServeCached(season: String): DriverStandingsResult {
        val spec = specForSeason(season)
        val cache = cacheProvider.getCache<String, StandingsData<DriverStanding>>(spec)
        val cached = cache.getIfPresent(season)

        return when {
            cached != null -> freshResult(cached)
            isThrottled(season) -> staleOrThrow(season)
            else -> tryFetch(season)
        }
    }

    private fun isThrottled(season: String): Boolean {
        val lastFailed = lastFailedAttempt[season] ?: return false
        return Instant.now().isBefore(lastFailed.plusSeconds(RETRY_THROTTLE_SECONDS))
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun tryFetch(season: String): DriverStandingsResult =
        try {
            val data = dataSource.fetchDriverStandings(season)
            val spec = specForResolvedSeason(data.season)
            val cache = cacheProvider.getCache<String, StandingsData<DriverStanding>>(spec)
            cache.put(season, data)
            cacheProvider.putFallback(spec, season, data)
            lastFailedAttempt.remove(season)

            freshResult(data)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            lastFailedAttempt[season] = Instant.now()
            staleOrThrow(season)
        }

    private fun freshResult(data: StandingsData<DriverStanding>): DriverStandingsResult =
        DriverStandingsResult(
            season = data.season,
            round = data.round,
            standings = data.standings,
            isStale = false,
        )

    @Suppress("UNCHECKED_CAST")
    private fun staleOrThrow(season: String): DriverStandingsResult {
        for (spec in listOf(CacheSpec.DRIVER_STANDINGS, CacheSpec.DRIVER_STANDINGS_HISTORICAL)) {
            val fallback = cacheProvider.getFallback<String>(spec, season) as? StandingsData<DriverStanding>
            if (fallback != null) {
                return DriverStandingsResult(
                    season = fallback.season,
                    round = fallback.round,
                    standings = fallback.standings,
                    isStale = true,
                )
            }
        }
        throw ExternalServiceException("Unable to fetch driver standings. Please try again later.")
    }

    private fun specForSeason(season: String): CacheSpec =
        if (season == "current" || season == Year.now().value.toString()) {
            CacheSpec.DRIVER_STANDINGS
        } else {
            CacheSpec.DRIVER_STANDINGS_HISTORICAL
        }

    private fun specForResolvedSeason(resolvedSeason: String): CacheSpec =
        if (resolvedSeason == Year.now().value.toString()) {
            CacheSpec.DRIVER_STANDINGS
        } else {
            CacheSpec.DRIVER_STANDINGS_HISTORICAL
        }
}
