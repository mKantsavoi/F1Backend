package com.blaizmiko.f1backend.domain.port

import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.Team

interface TeamCache {
    fun get(season: String): CacheEntry<List<Team>>?

    fun put(
        season: String,
        entry: CacheEntry<List<Team>>,
    )
}
