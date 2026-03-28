package com.blaizmiko.f1backend.adapter.port

import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.Circuit

interface CircuitCache {
    fun get(): CacheEntry<List<Circuit>>?

    fun put(entry: CacheEntry<List<Circuit>>)
}
