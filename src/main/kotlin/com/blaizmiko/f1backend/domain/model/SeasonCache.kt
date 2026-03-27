package com.blaizmiko.f1backend.domain.model

data class SeasonCache<T>(
    val resolvedSeason: String,
    val items: List<T>,
)
