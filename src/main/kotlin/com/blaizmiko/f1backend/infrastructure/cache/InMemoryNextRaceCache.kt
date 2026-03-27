package com.blaizmiko.f1backend.infrastructure.cache

import com.blaizmiko.f1backend.adapter.port.NextRaceCache
import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.RaceWeekend
import java.util.concurrent.atomic.AtomicReference

class InMemoryNextRaceCache : NextRaceCache {
    private val cache = AtomicReference<CacheEntry<RaceWeekend?>>()

    override fun get(): CacheEntry<RaceWeekend?>? = cache.get()

    override fun put(entry: CacheEntry<RaceWeekend?>) {
        cache.set(entry)
    }
}
