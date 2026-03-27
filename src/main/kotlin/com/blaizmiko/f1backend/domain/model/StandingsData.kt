package com.blaizmiko.f1backend.domain.model

data class StandingsData<T>(
    val season: String,
    val round: Int,
    val standings: List<T>,
)
