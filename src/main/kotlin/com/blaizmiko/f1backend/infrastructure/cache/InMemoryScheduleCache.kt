package com.blaizmiko.f1backend.infrastructure.cache

import com.blaizmiko.f1backend.adapter.port.ScheduleCache
import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.RaceWeekend
import com.blaizmiko.f1backend.domain.model.SeasonCache
import java.util.concurrent.ConcurrentHashMap

class InMemoryScheduleCache : ScheduleCache {
    private val cache = ConcurrentHashMap<String, CacheEntry<SeasonCache<RaceWeekend>>>()

    override fun get(season: String): CacheEntry<SeasonCache<RaceWeekend>>? = cache[season]

    override fun put(
        season: String,
        entry: CacheEntry<SeasonCache<RaceWeekend>>,
    ) {
        cache[season] = entry
    }
}
