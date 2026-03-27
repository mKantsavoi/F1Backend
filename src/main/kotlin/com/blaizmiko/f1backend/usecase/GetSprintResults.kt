package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.adapter.port.RaceResultCache
import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.NotFoundException
import com.blaizmiko.f1backend.domain.model.RaceResult
import com.blaizmiko.f1backend.domain.port.RaceDataSource
import com.blaizmiko.f1backend.domain.port.SprintResultsData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.Year
import java.util.concurrent.ConcurrentHashMap

data class SprintResultsResult(
    val season: String,
    val round: Int,
    val raceName: String,
    val results: List<RaceResult>,
    val isStale: Boolean,
)

class GetSprintResults(
    private val cache: RaceResultCache,
    private val dataSource: RaceDataSource,
) {
    companion object {
        private const val RETRY_THROTTLE_SECONDS = 60L
        private const val CURRENT_SEASON_TTL_SECONDS = 300L
        private val NO_SPRINT = SprintResultsData("", 0, "", emptyList())
    }

    private val fetchMutexes = ConcurrentHashMap<String, Mutex>()
    private val lastFailedAttempt = ConcurrentHashMap<String, Instant>()

    suspend fun execute(
        season: String,
        round: Int,
    ): SprintResultsResult {
        val key = "sprint:$season:$round"
        val quickCached = cache.get(key)
        if (quickCached != null && quickCached.isFresh()) {
            throwIfNoSprint(quickCached, season, round)
            return toResult(quickCached, false)
        }

        val mutex = fetchMutexes.getOrPut(key) { Mutex() }
        return mutex.withLock { fetchOrServeCached(season, round, key) }
    }

    private suspend fun fetchOrServeCached(
        season: String,
        round: Int,
        key: String,
    ): SprintResultsResult {
        val cached = cache.get(key)

        if (cached != null && cached.isFresh()) {
            throwIfNoSprint(cached, season, round)
            return toResult(cached, false)
        }

        return when {
            isThrottled(key) -> staleOrThrow(cached)
            else -> tryFetch(season, round, key, cached)
        }
    }

    private fun isThrottled(key: String): Boolean {
        val lastFailed = lastFailedAttempt[key] ?: return false
        return Instant.now().isBefore(lastFailed.plusSeconds(RETRY_THROTTLE_SECONDS))
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun tryFetch(
        season: String,
        round: Int,
        key: String,
        cached: CacheEntry<Any>?,
    ): SprintResultsResult =
        try {
            val data = dataSource.fetchSprintResults(season, round)
            val now = Instant.now()
            val isCurrentSeason = season == Year.now().value.toString()
            val cacheData = data ?: NO_SPRINT
            val entry =
                CacheEntry(
                    data = cacheData as Any,
                    fetchedAt = now,
                    expiresAt = if (isCurrentSeason) now.plusSeconds(CURRENT_SEASON_TTL_SECONDS) else Instant.MAX,
                )
            cache.put(key, entry)
            lastFailedAttempt.remove(key)

            if (data == null) {
                throw NotFoundException("No sprint results found for season $season round $round")
            }

            SprintResultsResult(
                season = data.season,
                round = data.round,
                raceName = data.raceName,
                results = data.results,
                isStale = false,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: NotFoundException) {
            throw e
        } catch (_: Exception) {
            lastFailedAttempt[key] = Instant.now()
            staleOrThrow(cached)
        }

    @Suppress("UNCHECKED_CAST")
    private fun toResult(
        cached: CacheEntry<Any>,
        isStale: Boolean,
    ): SprintResultsResult {
        val data = cached.data as SprintResultsData
        return SprintResultsResult(
            season = data.season,
            round = data.round,
            raceName = data.raceName,
            results = data.results,
            isStale = isStale,
        )
    }

    private fun throwIfNoSprint(
        cached: CacheEntry<Any>,
        season: String,
        round: Int,
    ) {
        if (cached.data === NO_SPRINT) {
            throw NotFoundException("No sprint results found for season $season round $round")
        }
    }

    private fun staleOrThrow(cached: CacheEntry<Any>?): SprintResultsResult {
        if (cached != null) {
            if (cached.data === NO_SPRINT) {
                throw NotFoundException("No sprint results available")
            }
            return toResult(cached, true)
        }
        throw ExternalServiceException("Unable to fetch sprint results. Please try again later.")
    }
}
