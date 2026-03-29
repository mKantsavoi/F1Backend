package com.blaizmiko.f1backend.infrastructure.cache

import com.blaizmiko.f1backend.adapter.port.CacheProvider
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.CacheStats
import com.sksamuel.aedile.core.Cache
import com.sksamuel.aedile.core.asCache
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class CacheRegistry : CacheProvider {
    private data class CacheKey(
        val spec: CacheSpec,
        val ttlOverride: Duration?,
    )

    private val caches = ConcurrentHashMap<CacheKey, Cache<Any, Any>>()
    private val fallbackMaps = ConcurrentHashMap<CacheSpec, ConcurrentHashMap<Any, Any>>()

    @Suppress("UNCHECKED_CAST")
    override fun <K : Any, V : Any> getCache(spec: CacheSpec): Cache<K, V> =
        getOrCreateCache(CacheKey(spec, null), spec.ttl, spec.maxSize) as Cache<K, V>

    @Suppress("UNCHECKED_CAST")
    override fun <K : Any, V : Any> getCache(
        spec: CacheSpec,
        ttlOverride: Duration,
    ): Cache<K, V> = getOrCreateCache(CacheKey(spec, ttlOverride), ttlOverride, spec.maxSize) as Cache<K, V>

    override fun <K : Any> getFallback(
        spec: CacheSpec,
        key: K,
    ): Any? = fallbackMaps[spec]?.get(key)

    override fun <K : Any> putFallback(
        spec: CacheSpec,
        key: K,
        value: Any,
    ) {
        fallbackMaps.getOrPut(spec) { ConcurrentHashMap() }[key] = value
    }

    fun stats(spec: CacheSpec): CacheStats? {
        val entry = caches.entries.find { it.key.spec == spec } ?: return null
        return entry.value
            .underlying()
            .synchronous()
            .stats()
    }

    private fun getOrCreateCache(
        key: CacheKey,
        ttl: Duration,
        maxSize: Long,
    ): Cache<Any, Any> =
        caches.getOrPut(key) {
            Caffeine
                .newBuilder()
                .expireAfterWrite(ttl.toJavaDuration())
                .maximumSize(maxSize)
                .recordStats()
                .asCache<Any, Any>()
        }
}
