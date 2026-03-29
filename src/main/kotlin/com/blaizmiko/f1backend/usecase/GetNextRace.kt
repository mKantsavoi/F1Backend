package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.adapter.port.CacheProvider
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.RaceWeekend
import com.blaizmiko.f1backend.infrastructure.cache.CacheSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

data class NextRaceResult(
    val season: String,
    val race: RaceWeekend?,
    val isStale: Boolean,
)

class GetNextRace(
    private val cacheProvider: CacheProvider,
    private val dataSource: com.blaizmiko.f1backend.domain.port.ScheduleDataSource,
) {
    companion object {
        private const val RETRY_THROTTLE_SECONDS = 60L
        private const val CACHE_KEY = "next"
    }

    private val cache = cacheProvider.getCache<String, NextRaceData>(CacheSpec.SCHEDULE_NEXT)
    private val fetchMutex = Mutex()

    @Volatile
    private var lastFailedAttempt: Instant? = null

    suspend fun execute(): NextRaceResult {
        val quickCached = cache.getIfPresent(CACHE_KEY)
        if (quickCached != null) {
            return NextRaceResult(season = quickCached.season, race = quickCached.race, isStale = false)
        }

        return fetchMutex.withLock { fetchOrServeCached() }
    }

    private suspend fun fetchOrServeCached(): NextRaceResult {
        val cached = cache.getIfPresent(CACHE_KEY)

        return when {
            cached != null -> NextRaceResult(season = cached.season, race = cached.race, isStale = false)
            isThrottled() -> staleOrThrow()
            else -> tryFetch()
        }
    }

    private fun isThrottled(): Boolean {
        val lastFailed = lastFailedAttempt ?: return false
        return Instant.now().isBefore(lastFailed.plusSeconds(RETRY_THROTTLE_SECONDS))
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun tryFetch(): NextRaceResult =
        try {
            val (season, race) = dataSource.fetchNextRace()
            val data = NextRaceData(season, race)
            cache.put(CACHE_KEY, data)
            cacheProvider.putFallback(CacheSpec.SCHEDULE_NEXT, CACHE_KEY, data)
            lastFailedAttempt = null

            NextRaceResult(season = season, race = race, isStale = false)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            lastFailedAttempt = Instant.now()
            staleOrThrow()
        }

    private fun staleOrThrow(): NextRaceResult {
        val fallback = cacheProvider.getFallback<String>(CacheSpec.SCHEDULE_NEXT, CACHE_KEY) as? NextRaceData
        if (fallback != null) {
            return NextRaceResult(
                season = fallback.season,
                race = fallback.race,
                isStale = true,
            )
        }
        throw ExternalServiceException("Unable to fetch next race data. Please try again later.")
    }
}

internal data class NextRaceData(
    val season: String,
    val race: RaceWeekend?,
)
