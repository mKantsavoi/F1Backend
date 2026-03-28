package com.blaizmiko.f1backend.infrastructure.cache

import com.blaizmiko.f1backend.adapter.port.ConstructorStandingsCache
import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.ConstructorStanding
import com.blaizmiko.f1backend.domain.model.StandingsData
import java.util.concurrent.ConcurrentHashMap

class InMemoryConstructorStandingsCache : ConstructorStandingsCache {
    private val cache = ConcurrentHashMap<String, CacheEntry<StandingsData<ConstructorStanding>>>()

    override fun get(season: String): CacheEntry<StandingsData<ConstructorStanding>>? = cache[season]

    override fun put(
        season: String,
        entry: CacheEntry<StandingsData<ConstructorStanding>>,
    ) {
        cache[season] = entry
    }
}
