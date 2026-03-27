package com.blaizmiko.f1backend.adapter.port

import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.DriverStanding
import com.blaizmiko.f1backend.domain.model.StandingsData

interface DriverStandingsCache {
    fun get(season: String): CacheEntry<StandingsData<DriverStanding>>?

    fun put(
        season: String,
        entry: CacheEntry<StandingsData<DriverStanding>>,
    )
}
