package com.blaizmiko.f1backend.infrastructure.cache

import com.blaizmiko.f1backend.adapter.port.CircuitCache
import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.Circuit
import java.util.concurrent.atomic.AtomicReference

class InMemoryCircuitCache : CircuitCache {
    private val cache = AtomicReference<CacheEntry<List<Circuit>>>()

    override fun get(): CacheEntry<List<Circuit>>? = cache.get()

    override fun put(entry: CacheEntry<List<Circuit>>) {
        cache.set(entry)
    }
}
