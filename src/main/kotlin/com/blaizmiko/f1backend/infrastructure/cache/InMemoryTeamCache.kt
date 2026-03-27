package com.blaizmiko.f1backend.infrastructure.cache

import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.SeasonCache
import com.blaizmiko.f1backend.domain.model.Team
import com.blaizmiko.f1backend.domain.port.TeamCache
import java.util.concurrent.ConcurrentHashMap

class InMemoryTeamCache : TeamCache {
    private val cache = ConcurrentHashMap<String, CacheEntry<SeasonCache<Team>>>()

    override fun get(season: String): CacheEntry<SeasonCache<Team>>? = cache[season]

    override fun put(
        season: String,
        entry: CacheEntry<SeasonCache<Team>>,
    ) {
        cache[season] = entry
    }
}
