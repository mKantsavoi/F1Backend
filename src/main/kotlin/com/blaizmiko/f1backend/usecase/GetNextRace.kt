package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.adapter.port.NextRaceCache
import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.RaceWeekend
import com.blaizmiko.f1backend.domain.port.ScheduleDataSource
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
    private val cache: NextRaceCache,
    private val dataSource: ScheduleDataSource,
) {
    companion object {
        private const val RETRY_THROTTLE_SECONDS = 60L
        private const val CACHE_TTL_SECONDS = 3600L
        private const val SEASON_YEAR_LENGTH = 4
    }

    private val fetchMutex = Mutex()

    @Volatile
    private var lastFailedAttempt: Instant? = null

    suspend fun execute(): NextRaceResult {
        val quickCached = cache.get()
        if (quickCached != null && quickCached.isFresh()) {
            val season = quickCached.data?.let { extractSeason(it) } ?: ""
            return NextRaceResult(season = season, race = quickCached.data, isStale = false)
        }

        return fetchMutex.withLock { fetchOrServeCached() }
    }

    private suspend fun fetchOrServeCached(): NextRaceResult {
        val cached = cache.get()

        return when {
            cached != null && cached.isFresh() ->
                NextRaceResult(season = extractSeason(cached.data), race = cached.data, isStale = false)
            isThrottled() -> staleOrThrow(cached)
            else -> tryFetch(cached)
        }
    }

    private fun isThrottled(): Boolean {
        val lastFailed = lastFailedAttempt ?: return false
        return Instant.now().isBefore(lastFailed.plusSeconds(RETRY_THROTTLE_SECONDS))
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun tryFetch(cached: CacheEntry<RaceWeekend?>?): NextRaceResult =
        try {
            val (season, race) = dataSource.fetchNextRace()
            val now = Instant.now()
            val entry =
                CacheEntry(
                    data = race,
                    fetchedAt = now,
                    expiresAt = now.plusSeconds(CACHE_TTL_SECONDS),
                )
            cache.put(entry)
            lastFailedAttempt = null

            NextRaceResult(season = season, race = race, isStale = false)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            lastFailedAttempt = Instant.now()
            staleOrThrow(cached)
        }

    private fun staleOrThrow(cached: CacheEntry<RaceWeekend?>?): NextRaceResult {
        if (cached != null) {
            return NextRaceResult(
                season = extractSeason(cached.data),
                race = cached.data,
                isStale = true,
            )
        }
        throw ExternalServiceException("Unable to fetch next race data. Please try again later.")
    }

    private fun extractSeason(race: RaceWeekend?): String = race?.date?.take(SEASON_YEAR_LENGTH) ?: ""
}
