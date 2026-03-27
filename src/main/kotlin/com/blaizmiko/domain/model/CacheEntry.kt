package com.blaizmiko.domain.model

import java.time.Instant

data class CacheEntry<T>(
    val data: T,
    val fetchedAt: Instant,
    val expiresAt: Instant,
) {
    fun isFresh(): Boolean = Instant.now().isBefore(expiresAt)
}
