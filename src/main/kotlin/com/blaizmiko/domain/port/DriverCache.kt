package com.blaizmiko.domain.port

import com.blaizmiko.domain.model.CacheEntry
import com.blaizmiko.domain.model.Driver

interface DriverCache {
    fun get(season: String): CacheEntry<List<Driver>>?
    fun put(season: String, entry: CacheEntry<List<Driver>>)
}
