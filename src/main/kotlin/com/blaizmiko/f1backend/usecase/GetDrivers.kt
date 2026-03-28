package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.adapter.port.DriverCache
import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.Driver
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.SeasonCache
import com.blaizmiko.f1backend.domain.port.DriverDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

data class DriversResult(
    val season: String,
    val drivers: List<Driver>,
    val isStale: Boolean,
)

class GetDrivers(
    private val cache: DriverCache,
    private val dataSource: DriverDataSource,
    private val cacheTtlHours: Long,
) {
    companion object {
        private const val RETRY_THROTTLE_SECONDS = 60L
        private const val SECONDS_PER_HOUR = 3600L
    }

    private val fetchMutexes = ConcurrentHashMap<String, Mutex>()
    private val lastFailedAttempt = ConcurrentHashMap<String, Instant>()

    suspend fun execute(season: String = "current"): DriversResult {
        val quickCached = cache.get(season)
        if (quickCached != null && quickCached.isFresh()) {
            return freshResult(quickCached.data)
        }

        val mutex = fetchMutexes.getOrPut(season) { Mutex() }
        return mutex.withLock { fetchOrServeCached(season) }
    }

    private suspend fun fetchOrServeCached(season: String): DriversResult {
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
        cached: CacheEntry<SeasonCache<Driver>>?,
    ): DriversResult =
        try {
            val (resolvedSeason, drivers) = dataSource.fetchDrivers(season)
            val now = Instant.now()
            val entry =
                CacheEntry(
                    data = SeasonCache(resolvedSeason, drivers),
                    fetchedAt = now,
                    expiresAt = now.plusSeconds(cacheTtlHours * SECONDS_PER_HOUR),
                )
            cache.put(season, entry)
            lastFailedAttempt.remove(season)

            DriversResult(season = resolvedSeason, drivers = drivers, isStale = false)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            lastFailedAttempt[season] = Instant.now()
            staleOrThrow(cached)
        }

    private fun freshResult(data: SeasonCache<Driver>): DriversResult =
        DriversResult(
            season = data.resolvedSeason,
            drivers = data.items,
            isStale = false,
        )

    private fun staleOrThrow(cached: CacheEntry<SeasonCache<Driver>>?): DriversResult {
        if (cached != null) {
            return DriversResult(
                season = cached.data.resolvedSeason,
                drivers = cached.data.items,
                isStale = true,
            )
        }
        throw ExternalServiceException("Unable to fetch driver data. Please try again later.")
    }
}
