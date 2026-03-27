package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.adapter.port.DriverStandingsCache
import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.DriverStanding
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.StandingsData
import com.blaizmiko.f1backend.domain.port.StandingsDataSource
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
    private val cache: DriverStandingsCache,
    private val dataSource: StandingsDataSource,
) {
    companion object {
        private const val RETRY_THROTTLE_SECONDS = 60L
        private const val CURRENT_SEASON_TTL_SECONDS = 3600L
    }

    private val fetchMutexes = ConcurrentHashMap<String, Mutex>()
    private val lastFailedAttempt = ConcurrentHashMap<String, Instant>()

    suspend fun execute(season: String = "current"): DriverStandingsResult {
        val quickCached = cache.get(season)
        if (quickCached != null && quickCached.isFresh()) {
            return freshResult(quickCached.data)
        }

        val mutex = fetchMutexes.getOrPut(season) { Mutex() }
        return mutex.withLock { fetchOrServeCached(season) }
    }

    private suspend fun fetchOrServeCached(season: String): DriverStandingsResult {
        val cached = cache.get(season)

        return when {
            cached != null && cached.isFresh() -> freshResult(cached.data)
            isThrottled(season) -> staleOrThrow(cached)
            else -> tryFetch(season, cached)
        }
    }

    private fun isThrottled(season: String): Boolean {
        val lastFailed = lastFailedAttempt[season] ?: return false
        return Instant.now().isBefore(lastFailed.plusSeconds(RETRY_THROTTLE_SECONDS))
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun tryFetch(
        season: String,
        cached: CacheEntry<StandingsData<DriverStanding>>?,
    ): DriverStandingsResult =
        try {
            val data = dataSource.fetchDriverStandings(season)
            val now = Instant.now()
            val ttl =
                if (data.season == Year.now().value.toString()) {
                    CURRENT_SEASON_TTL_SECONDS
                } else {
                    Long.MAX_VALUE / 2
                }
            val entry =
                CacheEntry(
                    data = data,
                    fetchedAt = now,
                    expiresAt = if (ttl == Long.MAX_VALUE / 2) Instant.MAX else now.plusSeconds(ttl),
                )
            cache.put(season, entry)
            lastFailedAttempt.remove(season)

            freshResult(data)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            lastFailedAttempt[season] = Instant.now()
            staleOrThrow(cached)
        }

    private fun freshResult(data: StandingsData<DriverStanding>): DriverStandingsResult =
        DriverStandingsResult(
            season = data.season,
            round = data.round,
            standings = data.standings,
            isStale = false,
        )

    private fun staleOrThrow(cached: CacheEntry<StandingsData<DriverStanding>>?): DriverStandingsResult {
        if (cached != null) {
            return DriverStandingsResult(
                season = cached.data.season,
                round = cached.data.round,
                standings = cached.data.standings,
                isStale = true,
            )
        }
        throw ExternalServiceException("Unable to fetch driver standings. Please try again later.")
    }
}
