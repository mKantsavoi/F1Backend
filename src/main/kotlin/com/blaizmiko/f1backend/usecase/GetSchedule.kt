package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.adapter.port.ScheduleCache
import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.RaceWeekend
import com.blaizmiko.f1backend.domain.model.SeasonCache
import com.blaizmiko.f1backend.domain.port.ScheduleDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

data class ScheduleResult(
    val season: String,
    val races: List<RaceWeekend>,
    val isStale: Boolean,
)

class GetSchedule(
    private val cache: ScheduleCache,
    private val dataSource: ScheduleDataSource,
    private val cacheTtlHours: Long,
) {
    companion object {
        private const val RETRY_THROTTLE_SECONDS = 60L
        private const val SECONDS_PER_HOUR = 3600L
    }

    private val fetchMutexes = ConcurrentHashMap<String, Mutex>()
    private val lastFailedAttempt = ConcurrentHashMap<String, Instant>()

    suspend fun execute(season: String = "current"): ScheduleResult {
        val quickCached = cache.get(season)
        if (quickCached != null && quickCached.isFresh()) {
            return freshResult(quickCached.data)
        }

        val mutex = fetchMutexes.getOrPut(season) { Mutex() }
        return mutex.withLock { fetchOrServeCached(season) }
    }

    private suspend fun fetchOrServeCached(season: String): ScheduleResult {
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
        cached: CacheEntry<SeasonCache<RaceWeekend>>?,
    ): ScheduleResult =
        try {
            val (resolvedSeason, races) = dataSource.fetchSchedule(season)
            val now = Instant.now()
            val entry =
                CacheEntry(
                    data = SeasonCache(resolvedSeason, races),
                    fetchedAt = now,
                    expiresAt = now.plusSeconds(cacheTtlHours * SECONDS_PER_HOUR),
                )
            cache.put(season, entry)
            lastFailedAttempt.remove(season)

            ScheduleResult(season = resolvedSeason, races = races, isStale = false)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            lastFailedAttempt[season] = Instant.now()
            staleOrThrow(cached)
        }

    private fun freshResult(data: SeasonCache<RaceWeekend>): ScheduleResult =
        ScheduleResult(
            season = data.resolvedSeason,
            races = data.items,
            isStale = false,
        )

    private fun staleOrThrow(cached: CacheEntry<SeasonCache<RaceWeekend>>?): ScheduleResult {
        if (cached != null) {
            return ScheduleResult(
                season = cached.data.resolvedSeason,
                races = cached.data.items,
                isStale = true,
            )
        }
        throw ExternalServiceException("Unable to fetch schedule data. Please try again later.")
    }
}
