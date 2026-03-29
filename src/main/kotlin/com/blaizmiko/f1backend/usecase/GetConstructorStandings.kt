package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.adapter.port.CacheProvider
import com.blaizmiko.f1backend.domain.model.ConstructorStanding
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

data class ConstructorStandingsResult(
    val season: String,
    val round: Int,
    val standings: List<ConstructorStanding>,
    val isStale: Boolean,
)

class GetConstructorStandings(
    private val cacheProvider: CacheProvider,
    private val dataSource: StandingsDataSource,
) {
    companion object {
        private const val RETRY_THROTTLE_SECONDS = 60L
    }

    private val fetchMutexes = ConcurrentHashMap<String, Mutex>()
    private val lastFailedAttempt = ConcurrentHashMap<String, Instant>()

    suspend fun execute(season: String = "current"): ConstructorStandingsResult {
        val spec = specForSeason(season)
        val cache = cacheProvider.getCache<String, StandingsData<ConstructorStanding>>(spec)
        val quickCached = cache.getIfPresent(season)
        if (quickCached != null) {
            return freshResult(quickCached)
        }

        val mutex = fetchMutexes.getOrPut(season) { Mutex() }
        return mutex.withLock { fetchOrServeCached(season) }
    }

    private suspend fun fetchOrServeCached(season: String): ConstructorStandingsResult {
        val spec = specForSeason(season)
        val cache = cacheProvider.getCache<String, StandingsData<ConstructorStanding>>(spec)
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
    private suspend fun tryFetch(season: String): ConstructorStandingsResult =
        try {
            val data = dataSource.fetchConstructorStandings(season)
            val spec = specForResolvedSeason(data.season)
            val cache = cacheProvider.getCache<String, StandingsData<ConstructorStanding>>(spec)
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

    private fun freshResult(data: StandingsData<ConstructorStanding>): ConstructorStandingsResult =
        ConstructorStandingsResult(
            season = data.season,
            round = data.round,
            standings = data.standings,
            isStale = false,
        )

    @Suppress("UNCHECKED_CAST")
    private fun staleOrThrow(season: String): ConstructorStandingsResult {
        for (spec in listOf(CacheSpec.CONSTRUCTOR_STANDINGS, CacheSpec.CONSTRUCTOR_STANDINGS_HISTORICAL)) {
            val fallback = cacheProvider.getFallback<String>(spec, season) as? StandingsData<ConstructorStanding>
            if (fallback != null) {
                return ConstructorStandingsResult(
                    season = fallback.season,
                    round = fallback.round,
                    standings = fallback.standings,
                    isStale = true,
                )
            }
        }
        throw ExternalServiceException("Unable to fetch constructor standings. Please try again later.")
    }

    private fun specForSeason(season: String): CacheSpec =
        if (season == "current" || season == Year.now().value.toString()) {
            CacheSpec.CONSTRUCTOR_STANDINGS
        } else {
            CacheSpec.CONSTRUCTOR_STANDINGS_HISTORICAL
        }

    private fun specForResolvedSeason(resolvedSeason: String): CacheSpec =
        if (resolvedSeason == Year.now().value.toString()) {
            CacheSpec.CONSTRUCTOR_STANDINGS
        } else {
            CacheSpec.CONSTRUCTOR_STANDINGS_HISTORICAL
        }
}
