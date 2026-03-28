package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.adapter.port.CircuitCache
import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.Circuit
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.port.CircuitDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

data class CircuitsResult(
    val circuits: List<Circuit>,
    val isStale: Boolean,
)

class GetCircuits(
    private val cache: CircuitCache,
    private val dataSource: CircuitDataSource,
) {
    companion object {
        private const val RETRY_THROTTLE_SECONDS = 60L
    }

    private val fetchMutex = Mutex()

    @Volatile
    private var lastFailedAttempt: Instant? = null

    suspend fun execute(): CircuitsResult {
        val quickCached = cache.get()
        if (quickCached != null && quickCached.isFresh()) {
            return CircuitsResult(circuits = quickCached.data, isStale = false)
        }

        return fetchMutex.withLock { fetchOrServeCached() }
    }

    private suspend fun fetchOrServeCached(): CircuitsResult {
        val cached = cache.get()

        return when {
            cached != null && cached.isFresh() -> CircuitsResult(circuits = cached.data, isStale = false)
            isThrottled() -> staleOrThrow(cached)
            else -> tryFetch(cached)
        }
    }

    private fun isThrottled(): Boolean {
        val lastFailed = lastFailedAttempt ?: return false
        return Instant.now().isBefore(lastFailed.plusSeconds(RETRY_THROTTLE_SECONDS))
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun tryFetch(cached: CacheEntry<List<Circuit>>?): CircuitsResult =
        try {
            val circuits = dataSource.fetchCircuits()
            val now = Instant.now()
            val entry =
                CacheEntry(
                    data = circuits,
                    fetchedAt = now,
                    expiresAt = Instant.MAX,
                )
            cache.put(entry)
            lastFailedAttempt = null

            CircuitsResult(circuits = circuits, isStale = false)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            lastFailedAttempt = Instant.now()
            staleOrThrow(cached)
        }

    private fun staleOrThrow(cached: CacheEntry<List<Circuit>>?): CircuitsResult {
        if (cached != null) {
            return CircuitsResult(circuits = cached.data, isStale = true)
        }
        throw ExternalServiceException("Unable to fetch circuit data. Please try again later.")
    }
}
