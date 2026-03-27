package com.blaizmiko.usecase

import com.blaizmiko.domain.model.CacheEntry
import com.blaizmiko.domain.model.Driver
import com.blaizmiko.domain.model.ExternalServiceException
import com.blaizmiko.domain.port.DriverCache
import com.blaizmiko.domain.port.DriverDataSource
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
    private val fetchMutexes = ConcurrentHashMap<String, Mutex>()
    private val resolvedSeasons = ConcurrentHashMap<String, String>()
    private val lastFailedAttempt = ConcurrentHashMap<String, Instant>()

    suspend fun execute(season: String = "current"): DriversResult {
        // Fast path: serve from fresh cache without acquiring mutex
        val quickCached = cache.get(season)
        if (quickCached != null && quickCached.isFresh()) {
            return DriversResult(
                season = resolvedSeasons[season] ?: season,
                drivers = quickCached.data,
                isStale = false,
            )
        }

        val mutex = fetchMutexes.getOrPut(season) { Mutex() }

        return mutex.withLock {
            // Re-check after acquiring lock (another coroutine may have refreshed)
            val cached = cache.get(season)

            if (cached != null && cached.isFresh()) {
                return@withLock DriversResult(
                    season = resolvedSeasons[season] ?: season,
                    drivers = cached.data,
                    isStale = false,
                )
            }

            // Skip fetch if we failed recently (throttle for 60s)
            val lastFailed = lastFailedAttempt[season]
            if (lastFailed != null && Instant.now().isBefore(lastFailed.plusSeconds(60))) {
                if (cached != null) {
                    return@withLock DriversResult(
                        season = resolvedSeasons[season] ?: season,
                        drivers = cached.data,
                        isStale = true,
                    )
                } else {
                    throw ExternalServiceException("Unable to fetch driver data. Please try again later.")
                }
            }

            try {
                val (resolvedSeason, drivers) = dataSource.fetchDrivers(season)
                val now = Instant.now()
                val entry = CacheEntry(
                    data = drivers,
                    fetchedAt = now,
                    expiresAt = now.plusSeconds(cacheTtlHours * 3600),
                )
                cache.put(season, entry)
                resolvedSeasons[season] = resolvedSeason
                lastFailedAttempt.remove(season)

                DriversResult(
                    season = resolvedSeason,
                    drivers = drivers,
                    isStale = false,
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                lastFailedAttempt[season] = Instant.now()
                if (cached != null) {
                    DriversResult(
                        season = resolvedSeasons[season] ?: season,
                        drivers = cached.data,
                        isStale = true,
                    )
                } else {
                    throw ExternalServiceException("Unable to fetch driver data. Please try again later.")
                }
            }
        }
    }
}
