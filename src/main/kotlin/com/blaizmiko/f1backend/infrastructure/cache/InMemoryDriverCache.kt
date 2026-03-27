package com.blaizmiko.f1backend.infrastructure.cache

import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.Driver
import com.blaizmiko.f1backend.domain.port.DriverCache
import java.util.concurrent.ConcurrentHashMap

class InMemoryDriverCache : DriverCache {
    private val cache = ConcurrentHashMap<String, CacheEntry<List<Driver>>>()

    override fun get(season: String): CacheEntry<List<Driver>>? = cache[season]

    override fun put(
        season: String,
        entry: CacheEntry<List<Driver>>,
    ) {
        cache[season] = entry
    }
}
