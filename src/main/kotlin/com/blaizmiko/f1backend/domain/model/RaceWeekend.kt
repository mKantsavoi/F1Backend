package com.blaizmiko.f1backend.domain.model

data class Sessions(
    val fp1: String? = null,
    val fp2: String? = null,
    val fp3: String? = null,
    val qualifying: String? = null,
    val sprint: String? = null,
    val race: String? = null,
)

data class RaceWeekend(
    val round: Int,
    val raceName: String,
    val circuitId: String,
    val circuitName: String,
    val country: String,
    val date: String,
    val time: String? = null,
    val sessions: Sessions,
)
