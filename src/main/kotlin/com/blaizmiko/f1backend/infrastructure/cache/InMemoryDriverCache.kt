package com.blaizmiko.f1backend.infrastructure.cache

import com.blaizmiko.f1backend.adapter.port.DriverCache
import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.Driver
import com.blaizmiko.f1backend.domain.model.SeasonCache
import java.util.concurrent.ConcurrentHashMap

class InMemoryDriverCache : DriverCache {
    private val cache = ConcurrentHashMap<String, CacheEntry<SeasonCache<Driver>>>()

    override fun get(season: String): CacheEntry<SeasonCache<Driver>>? = cache[season]

    override fun put(
        season: String,
        entry: CacheEntry<SeasonCache<Driver>>,
    ) {
        cache[season] = entry
    }
}
