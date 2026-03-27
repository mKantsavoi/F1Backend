package com.blaizmiko.f1backend.adapter.port

import com.blaizmiko.f1backend.domain.model.CacheEntry

interface RaceResultCache {
    fun get(key: String): CacheEntry<Any>?

    fun put(
        key: String,
        entry: CacheEntry<Any>,
    )
}
