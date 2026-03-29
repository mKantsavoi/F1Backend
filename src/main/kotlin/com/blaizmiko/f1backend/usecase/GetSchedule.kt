package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.adapter.port.CacheProvider
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.RaceWeekend
import com.blaizmiko.f1backend.domain.model.SeasonCache
import com.blaizmiko.f1backend.domain.port.ScheduleDataSource
import com.blaizmiko.f1backend.infrastructure.cache.CacheSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.hours

data class ScheduleResult(
    val season: String,
    val races: List<RaceWeekend>,
    val isStale: Boolean,
)

class GetSchedule(
    private val cacheProvider: CacheProvider,
    private val dataSource: ScheduleDataSource,
    private val cacheTtlHours: Long,
) {
    companion object {
        private const val RETRY_THROTTLE_SECONDS = 60L
    }

    private val cache =
        cacheProvider.getCache<String, SeasonCache<RaceWeekend>>(
            CacheSpec.SCHEDULE,
            cacheTtlHours.hours,
        )
    private val fetchMutexes = ConcurrentHashMap<String, Mutex>()
    private val lastFailedAttempt = ConcurrentHashMap<String, Instant>()

    suspend fun execute(season: String = "current"): ScheduleResult {
        val quickCached = cache.getIfPresent(season)
        if (quickCached != null) {
            return freshResult(quickCached)
        }

        val mutex = fetchMutexes.getOrPut(season) { Mutex() }
        return mutex.withLock { fetchOrServeCached(season) }
    }

    private suspend fun fetchOrServeCached(season: String): ScheduleResult {
        val cached = cache.getIfPresent(season)

        return when {
            cached != null -> freshResult(cached)
            isThrottled(season) -> staleOrThrow(season)
            else -> tryFetch(season)
        }
    }

    private fun isThrottled(season: String): Boolean {
        val lastFailed = lastFailedAttempt[season] ?: return false
        return Instant.now().isBefore(lastFailed.plusSeconds(RETRY_THROTTLE_SECONDS))
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun tryFetch(season: String): ScheduleResult =
        try {
            val (resolvedSeason, races) = dataSource.fetchSchedule(season)
            val data = SeasonCache(resolvedSeason, races)
            cache.put(season, data)
            cacheProvider.putFallback(CacheSpec.SCHEDULE, season, data)
            lastFailedAttempt.remove(season)

            ScheduleResult(season = resolvedSeason, races = races, isStale = false)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            lastFailedAttempt[season] = Instant.now()
            staleOrThrow(season)
        }

    private fun freshResult(data: SeasonCache<RaceWeekend>): ScheduleResult =
        ScheduleResult(
            season = data.resolvedSeason,
            races = data.items,
            isStale = false,
        )

    @Suppress("UNCHECKED_CAST")
    private fun staleOrThrow(season: String): ScheduleResult {
        val fallback = cacheProvider.getFallback<String>(CacheSpec.SCHEDULE, season) as? SeasonCache<RaceWeekend>
        if (fallback != null) {
            return ScheduleResult(
                season = fallback.resolvedSeason,
                races = fallback.items,
                isStale = true,
            )
        }
        throw ExternalServiceException("Unable to fetch schedule data. Please try again later.")
    }
}
