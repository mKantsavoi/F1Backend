package com.blaizmiko.f1backend.adapter.port

import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.Driver
import com.blaizmiko.f1backend.domain.model.SeasonCache

interface DriverCache {
    fun get(season: String): CacheEntry<SeasonCache<Driver>>?

    fun put(
        season: String,
        entry: CacheEntry<SeasonCache<Driver>>,
    )
}
