package com.blaizmiko.f1backend.infrastructure.cache

import com.blaizmiko.f1backend.adapter.port.RaceResultCache
import com.blaizmiko.f1backend.domain.model.CacheEntry
import java.util.concurrent.ConcurrentHashMap

class InMemoryRaceResultCache : RaceResultCache {
    private val cache = ConcurrentHashMap<String, CacheEntry<Any>>()

    override fun get(key: String): CacheEntry<Any>? = cache[key]

    override fun put(
        key: String,
        entry: CacheEntry<Any>,
    ) {
        cache[key] = entry
    }
}
