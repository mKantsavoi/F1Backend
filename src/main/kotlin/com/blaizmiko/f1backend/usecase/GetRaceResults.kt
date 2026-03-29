package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.adapter.port.CacheProvider
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.RaceResult
import com.blaizmiko.f1backend.domain.port.RaceDataSource
import com.blaizmiko.f1backend.domain.port.RaceResultsData
import com.blaizmiko.f1backend.infrastructure.cache.CacheSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.Year
import java.util.concurrent.ConcurrentHashMap

data class RaceResultsResult(
    val season: String,
    val round: Int,
    val raceName: String,
    val results: List<RaceResult>,
    val isStale: Boolean,
)

class GetRaceResults(
    private val cacheProvider: CacheProvider,
    private val dataSource: RaceDataSource,
) {
    companion object {
        private const val RETRY_THROTTLE_SECONDS = 60L
    }

    private val fetchMutexes = ConcurrentHashMap<String, Mutex>()
    private val lastFailedAttempt = ConcurrentHashMap<String, Instant>()

    suspend fun execute(
        season: String,
        round: Int,
    ): RaceResultsResult {
        val key = "results:$season:$round"
        val spec = specForSeason(season)
        val cache = cacheProvider.getCache<String, RaceResultsData>(spec)
        val quickCached = cache.getIfPresent(key)
        if (quickCached != null) {
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
    ): RaceResultsResult {
        val cache = cacheProvider.getCache<String, RaceResultsData>(spec)
        val cached = cache.getIfPresent(key)

        return when {
            cached != null -> toResult(cached, false)
            isThrottled(key) -> staleOrThrow(key, spec)
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
    ): RaceResultsResult =
        try {
            val data = dataSource.fetchRaceResults(season, round)
            val cache = cacheProvider.getCache<String, RaceResultsData>(spec)
            cache.put(key, data)
            cacheProvider.putFallback(spec, key, data)
            lastFailedAttempt.remove(key)

            toResult(data, false)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            lastFailedAttempt[key] = Instant.now()
            staleOrThrow(key, spec)
        }

    private fun toResult(
        data: RaceResultsData,
        isStale: Boolean,
    ): RaceResultsResult =
        RaceResultsResult(
            season = data.season,
            round = data.round,
            raceName = data.raceName,
            results = data.results,
            isStale = isStale,
        )

    private fun staleOrThrow(
        key: String,
        spec: CacheSpec,
    ): RaceResultsResult {
        val fallback = cacheProvider.getFallback<String>(spec, key) as? RaceResultsData
        if (fallback != null) {
            return toResult(fallback, true)
        }
        throw ExternalServiceException("Unable to fetch race results. Please try again later.")
    }

    private fun specForSeason(season: String): CacheSpec =
        if (season == Year.now().value.toString()) CacheSpec.RACE_RESULTS else CacheSpec.RACE_RESULTS_HISTORICAL
}
