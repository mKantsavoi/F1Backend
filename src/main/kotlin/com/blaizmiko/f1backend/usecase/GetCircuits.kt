package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.adapter.port.CacheProvider
import com.blaizmiko.f1backend.domain.model.Circuit
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.port.CircuitDataSource
import com.blaizmiko.f1backend.infrastructure.cache.CacheSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

data class CircuitsResult(
    val circuits: List<Circuit>,
    val isStale: Boolean,
)

class GetCircuits(
    private val cacheProvider: CacheProvider,
    private val dataSource: CircuitDataSource,
) {
    companion object {
        private const val RETRY_THROTTLE_SECONDS = 60L
        private const val CACHE_KEY = "all"
    }

    private val cache = cacheProvider.getCache<String, List<Circuit>>(CacheSpec.CIRCUITS)
    private val fetchMutex = Mutex()

    @Volatile
    private var lastFailedAttempt: Instant? = null

    suspend fun execute(): CircuitsResult {
        val quickCached = cache.getIfPresent(CACHE_KEY)
        if (quickCached != null) {
            return CircuitsResult(circuits = quickCached, isStale = false)
        }

        return fetchMutex.withLock { fetchOrServeCached() }
    }

    private suspend fun fetchOrServeCached(): CircuitsResult {
        val cached = cache.getIfPresent(CACHE_KEY)

        return when {
            cached != null -> CircuitsResult(circuits = cached, isStale = false)
            isThrottled() -> staleOrThrow()
            else -> tryFetch()
        }
    }

    private fun isThrottled(): Boolean {
        val lastFailed = lastFailedAttempt ?: return false
        return Instant.now().isBefore(lastFailed.plusSeconds(RETRY_THROTTLE_SECONDS))
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun tryFetch(): CircuitsResult =
        try {
            val circuits = dataSource.fetchCircuits()
            cache.put(CACHE_KEY, circuits)
            cacheProvider.putFallback(CacheSpec.CIRCUITS, CACHE_KEY, circuits)
            lastFailedAttempt = null

            CircuitsResult(circuits = circuits, isStale = false)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            lastFailedAttempt = Instant.now()
            staleOrThrow()
        }

    @Suppress("UNCHECKED_CAST")
    private fun staleOrThrow(): CircuitsResult {
        val fallback = cacheProvider.getFallback<String>(CacheSpec.CIRCUITS, CACHE_KEY) as? List<Circuit>
        if (fallback != null) {
            return CircuitsResult(circuits = fallback, isStale = true)
        }
        throw ExternalServiceException("Unable to fetch circuit data. Please try again later.")
    }
}
