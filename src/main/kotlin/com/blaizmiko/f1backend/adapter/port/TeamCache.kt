package com.blaizmiko.f1backend.adapter.port

import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.SeasonCache
import com.blaizmiko.f1backend.domain.model.Team

interface TeamCache {
    fun get(season: String): CacheEntry<SeasonCache<Team>>?

    fun put(
        season: String,
        entry: CacheEntry<SeasonCache<Team>>,
    )
}
