package com.blaizmiko.f1backend.infrastructure.cache

import com.blaizmiko.f1backend.adapter.port.DriverStandingsCache
import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.DriverStanding
import com.blaizmiko.f1backend.domain.model.StandingsData
import java.util.concurrent.ConcurrentHashMap

class InMemoryDriverStandingsCache : DriverStandingsCache {
    private val cache = ConcurrentHashMap<String, CacheEntry<StandingsData<DriverStanding>>>()

    override fun get(season: String): CacheEntry<StandingsData<DriverStanding>>? = cache[season]

    override fun put(
        season: String,
        entry: CacheEntry<StandingsData<DriverStanding>>,
    ) {
        cache[season] = entry
    }
}
