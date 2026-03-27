package com.blaizmiko.f1backend.adapter.port

import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.ConstructorStanding
import com.blaizmiko.f1backend.domain.model.StandingsData

interface ConstructorStandingsCache {
    fun get(season: String): CacheEntry<StandingsData<ConstructorStanding>>?

    fun put(
        season: String,
        entry: CacheEntry<StandingsData<ConstructorStanding>>,
    )
}
