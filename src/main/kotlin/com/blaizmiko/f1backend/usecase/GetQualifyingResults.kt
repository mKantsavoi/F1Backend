package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.adapter.port.RaceResultCache
import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.QualifyingResult
import com.blaizmiko.f1backend.domain.port.QualifyingResultsData
import com.blaizmiko.f1backend.domain.port.RaceDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

data class QualifyingResultsResult(
    val season: String,
    val round: Int,
    val raceName: String,
    val qualifying: List<QualifyingResult>,
    val isStale: Boolean,
)

class GetQualifyingResults(
    private val cache: RaceResultCache,
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
        val quickCached = cache.get(key)
        if (quickCached != null && quickCached.isFresh()) {
            return toResult(quickCached, false)
        }

        val mutex = fetchMutexes.getOrPut(key) { Mutex() }
        return mutex.withLock { fetchOrServeCached(season, round, key) }
    }

    private suspend fun fetchOrServeCached(
        season: String,
        round: Int,
        key: String,
    ): QualifyingResultsResult {
        val cached = cache.get(key)

        return when {
            cached != null && cached.isFresh() -> toResult(cached, false)
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
    ): QualifyingResultsResult =
        try {
            val data = dataSource.fetchQualifyingResults(season, round)
            val now = Instant.now()
            val entry =
                CacheEntry(
                    data = data as Any,
                    fetchedAt = now,
                    expiresAt = Instant.MAX,
                )
            cache.put(key, entry)
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
            staleOrThrow(cached)
        }

    @Suppress("UNCHECKED_CAST")
    private fun toResult(
        cached: CacheEntry<Any>,
        isStale: Boolean,
    ): QualifyingResultsResult {
        val data = cached.data as QualifyingResultsData
        return QualifyingResultsResult(
            season = data.season,
            round = data.round,
            raceName = data.raceName,
            qualifying = data.qualifying,
            isStale = isStale,
        )
    }

    private fun staleOrThrow(cached: CacheEntry<Any>?): QualifyingResultsResult {
        if (cached != null) {
            return toResult(cached, true)
        }
        throw ExternalServiceException("Unable to fetch qualifying results. Please try again later.")
    }
}
