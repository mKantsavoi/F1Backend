package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.Team
import com.blaizmiko.f1backend.domain.port.TeamCache
import com.blaizmiko.f1backend.domain.port.TeamDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

data class TeamsResult(
    val season: String,
    val teams: List<Team>,
    val isStale: Boolean,
)

class GetTeams(
    private val cache: TeamCache,
    private val dataSource: TeamDataSource,
    private val cacheTtlHours: Long,
) {
    companion object {
        private const val RETRY_THROTTLE_SECONDS = 60L
        private const val SECONDS_PER_HOUR = 3600L
    }

    private val fetchMutexes = ConcurrentHashMap<String, Mutex>()
    private val resolvedSeasons = ConcurrentHashMap<String, String>()
    private val lastFailedAttempt = ConcurrentHashMap<String, Instant>()

    suspend fun execute(season: String = "current"): TeamsResult {
        val quickCached = cache.get(season)
        if (quickCached != null && quickCached.isFresh()) {
            return freshResult(season, quickCached.data)
        }

        val mutex = fetchMutexes.getOrPut(season) { Mutex() }
        return mutex.withLock { fetchOrServeCached(season) }
    }

    private suspend fun fetchOrServeCached(season: String): TeamsResult {
        val cached = cache.get(season)

        return when {
            cached != null && cached.isFresh() -> freshResult(season, cached.data)
            isThrottled(season) -> staleOrThrow(season, cached)
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
        cached: CacheEntry<List<Team>>?,
    ): TeamsResult =
        try {
            val (resolvedSeason, teams) = dataSource.fetchTeams(season)
            val now = Instant.now()
            val entry =
                CacheEntry(
                    data = teams,
                    fetchedAt = now,
                    expiresAt = now.plusSeconds(cacheTtlHours * SECONDS_PER_HOUR),
                )
            cache.put(season, entry)
            resolvedSeasons[season] = resolvedSeason
            lastFailedAttempt.remove(season)

            TeamsResult(season = resolvedSeason, teams = teams, isStale = false)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            lastFailedAttempt[season] = Instant.now()
            staleOrThrow(season, cached)
        }

    private fun freshResult(
        season: String,
        teams: List<Team>,
    ): TeamsResult =
        TeamsResult(
            season = resolvedSeasons[season] ?: season,
            teams = teams,
            isStale = false,
        )

    private fun staleOrThrow(
        season: String,
        cached: CacheEntry<List<Team>>?,
    ): TeamsResult {
        if (cached != null) {
            return TeamsResult(
                season = resolvedSeasons[season] ?: season,
                teams = cached.data,
                isStale = true,
            )
        }
        throw ExternalServiceException("Unable to fetch team data. Please try again later.")
    }
}
