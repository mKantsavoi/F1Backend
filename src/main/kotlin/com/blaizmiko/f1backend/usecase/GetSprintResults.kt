package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.adapter.port.CacheProvider
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.NotFoundException
import com.blaizmiko.f1backend.domain.model.RaceResult
import com.blaizmiko.f1backend.domain.port.RaceDataSource
import com.blaizmiko.f1backend.domain.port.SprintResultsData
import com.blaizmiko.f1backend.infrastructure.cache.CacheSpec
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
    private val cacheProvider: CacheProvider,
    private val dataSource: RaceDataSource,
) {
    companion object {
        private const val RETRY_THROTTLE_SECONDS = 60L
        private val NO_SPRINT = SprintResultsData("", 0, "", emptyList())
    }

    private val fetchMutexes = ConcurrentHashMap<String, Mutex>()
    private val lastFailedAttempt = ConcurrentHashMap<String, Instant>()

    suspend fun execute(
        season: String,
        round: Int,
    ): SprintResultsResult {
        val key = "sprint:$season:$round"
        val spec = specForSeason(season)
        val cache = cacheProvider.getCache<String, SprintResultsData>(spec)
        val quickCached = cache.getIfPresent(key)
        if (quickCached != null) {
            throwIfNoSprint(quickCached, season, round)
            return toResult(quickCached, false)
        }

        val mutex = fetchMutexes.getOrPut(key) { Mutex() }
        return mutex.withLock { fetchOrServeCached(season, round, key, spec) }
    }

    private suspend fun fetchOrServeCached(
        season: String,
        round: Int,
        key: String,
        spec: CacheSpec,
    ): SprintResultsResult {
        val cache = cacheProvider.getCache<String, SprintResultsData>(spec)
        val cached = cache.getIfPresent(key)

        if (cached != null) {
            throwIfNoSprint(cached, season, round)
            return toResult(cached, false)
        }

        return when {
            isThrottled(key) -> staleOrThrow(cached, key, spec)
            else -> tryFetch(season, round, key, spec)
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
        spec: CacheSpec,
    ): SprintResultsResult =
        try {
            val data = dataSource.fetchSprintResults(season, round)
            val cacheData = data ?: NO_SPRINT
            val cache = cacheProvider.getCache<String, SprintResultsData>(spec)
            cache.put(key, cacheData)
            cacheProvider.putFallback(spec, key, cacheData)
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
            staleOrThrow(null, key, spec)
        }

    private fun toResult(
        data: SprintResultsData,
        isStale: Boolean,
    ): SprintResultsResult =
        SprintResultsResult(
            season = data.season,
            round = data.round,
            raceName = data.raceName,
            results = data.results,
            isStale = isStale,
        )

    private fun throwIfNoSprint(
        data: SprintResultsData,
        season: String,
        round: Int,
    ) {
        if (data === NO_SPRINT) {
            throw NotFoundException("No sprint results found for season $season round $round")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun staleOrThrow(
        cached: SprintResultsData?,
        key: String,
        spec: CacheSpec,
    ): SprintResultsResult {
        val fallback = cached ?: cacheProvider.getFallback<String>(spec, key) as? SprintResultsData
        if (fallback != null) {
            if (fallback === NO_SPRINT) {
                throw NotFoundException("No sprint results available")
            }
            return toResult(fallback, true)
        }
        throw ExternalServiceException("Unable to fetch sprint results. Please try again later.")
    }

    private fun specForSeason(season: String): CacheSpec =
        if (season == Year.now().value.toString()) CacheSpec.SPRINT else CacheSpec.SPRINT_HISTORICAL
}
