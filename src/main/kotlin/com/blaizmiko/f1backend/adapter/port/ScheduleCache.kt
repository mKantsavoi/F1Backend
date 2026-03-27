package com.blaizmiko.f1backend.adapter.port

import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.RaceWeekend
import com.blaizmiko.f1backend.domain.model.SeasonCache

interface ScheduleCache {
    fun get(season: String): CacheEntry<SeasonCache<RaceWeekend>>?

    fun put(
        season: String,
        entry: CacheEntry<SeasonCache<RaceWeekend>>,
    )
}
