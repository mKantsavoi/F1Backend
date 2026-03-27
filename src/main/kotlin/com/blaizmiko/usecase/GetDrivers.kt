package com.blaizmiko.usecase

import com.blaizmiko.domain.model.CacheEntry
import com.blaizmiko.domain.model.Driver
import com.blaizmiko.domain.model.ExternalServiceException
import com.blaizmiko.domain.port.DriverCache
import com.blaizmiko.domain.port.DriverDataSource
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

    suspend fun execute(season: String = "current"): DriversResult {
        val mutex = fetchMutexes.getOrPut(season) { Mutex() }

        return mutex.withLock {
            val cached = cache.get(season)

            if (cached != null && cached.isFresh()) {
                return@withLock DriversResult(
                    season = season,
                    drivers = cached.data,
                    isStale = false,
                )
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

                DriversResult(
                    season = resolvedSeason,
                    drivers = drivers,
                    isStale = false,
                )
            } catch (e: Exception) {
                if (cached != null) {
                    DriversResult(
                        season = season,
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
