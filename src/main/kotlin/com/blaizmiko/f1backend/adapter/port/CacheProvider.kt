package com.blaizmiko.f1backend.adapter.port

import com.blaizmiko.f1backend.infrastructure.cache.CacheSpec
import com.sksamuel.aedile.core.Cache
import kotlin.time.Duration

interface CacheProvider {
    fun <K : Any, V : Any> getCache(spec: CacheSpec): Cache<K, V>

    fun <K : Any, V : Any> getCache(
        spec: CacheSpec,
        ttlOverride: Duration,
    ): Cache<K, V>

    fun <K : Any> getFallback(
        spec: CacheSpec,
        key: K,
    ): Any?

    fun <K : Any> putFallback(
        spec: CacheSpec,
        key: K,
        value: Any,
    )
}
