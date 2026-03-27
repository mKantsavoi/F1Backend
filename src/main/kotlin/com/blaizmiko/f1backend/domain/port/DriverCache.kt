package com.blaizmiko.f1backend.domain.port

import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.Driver

interface DriverCache {
    fun get(season: String): CacheEntry<List<Driver>>?

    fun put(
        season: String,
        entry: CacheEntry<List<Driver>>,
    )
}
