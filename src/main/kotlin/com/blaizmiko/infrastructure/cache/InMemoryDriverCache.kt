package com.blaizmiko.infrastructure.cache

import com.blaizmiko.domain.model.CacheEntry
import com.blaizmiko.domain.model.Driver
import com.blaizmiko.domain.port.DriverCache
import java.util.concurrent.ConcurrentHashMap

class InMemoryDriverCache : DriverCache {

    private val cache = ConcurrentHashMap<String, CacheEntry<List<Driver>>>()

    override fun get(season: String): CacheEntry<List<Driver>>? = cache[season]

    override fun put(season: String, entry: CacheEntry<List<Driver>>) {
        cache[season] = entry
    }
}
