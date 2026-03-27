package com.blaizmiko.f1backend.adapter.port

import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.RaceWeekend

interface NextRaceCache {
    fun get(): CacheEntry<RaceWeekend?>?

    fun put(entry: CacheEntry<RaceWeekend?>)
}
