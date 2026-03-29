package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.adapter.port.CacheProvider
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.QualifyingResult
import com.blaizmiko.f1backend.domain.port.QualifyingResultsData
import com.blaizmiko.f1backend.domain.port.RaceDataSource
import com.blaizmiko.f1backend.infrastructure.cache.CacheSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.Year
import java.util.concurrent.ConcurrentHashMap

data class QualifyingResultsResult(
    val season: String,
    val round: Int,
    val raceName: String,
    val qualifying: List<QualifyingResult>,
    val isStale: Boolean,
)

class GetQualifyingResults(
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
    ): QualifyingResultsResult {
        val key = "qualifying:$season:$round"
        val spec = specForSeason(season)
        val cache = cacheProvider.getCache<String, QualifyingResultsData>(spec)
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
    ): QualifyingResultsResult {
        val cache = cacheProvider.getCache<String, QualifyingResultsData>(spec)
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
    ): QualifyingResultsResult =
        try {
            val data = dataSource.fetchQualifyingResults(season, round)
            val cache = cacheProvider.getCache<String, QualifyingResultsData>(spec)
            cache.put(key, data)
            cacheProvider.putFallback(spec, key, data)
            lastFailedAttempt.remove(key)

            QualifyingResultsResult(
                season = data.season,
                round = data.round,
                raceName = data.raceName,
                qualifying = data.qualifying,
                isStale = false,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            lastFailedAttempt[key] = Instant.now()
            staleOrThrow(key, spec)
        }

    private fun toResult(
        data: QualifyingResultsData,
        isStale: Boolean,
    ): QualifyingResultsResult =
        QualifyingResultsResult(
            season = data.season,
            round = data.round,
            raceName = data.raceName,
            qualifying = data.qualifying,
            isStale = isStale,
        )

    private fun staleOrThrow(
        key: String,
        spec: CacheSpec,
    ): QualifyingResultsResult {
        val fallback = cacheProvider.getFallback<String>(spec, key) as? QualifyingResultsData
        if (fallback != null) {
            return toResult(fallback, true)
        }
        throw ExternalServiceException("Unable to fetch qualifying results. Please try again later.")
    }

    private fun specForSeason(season: String): CacheSpec =
        if (season == Year.now().value.toString()) CacheSpec.QUALIFYING else CacheSpec.QUALIFYING_HISTORICAL
}
