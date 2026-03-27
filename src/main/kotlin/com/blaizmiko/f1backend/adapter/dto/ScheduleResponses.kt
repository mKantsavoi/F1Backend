package com.blaizmiko.f1backend.adapter.dto

import kotlinx.serialization.Serializable

@Serializable
data class SessionsDto(
    val fp1: String? = null,
    val fp2: String? = null,
    val fp3: String? = null,
    val qualifying: String? = null,
    val sprint: String? = null,
    val race: String? = null,
)

@Serializable
data class RaceWeekendDto(
    val round: Int,
    val raceName: String,
    val circuitId: String,
    val circuitName: String,
    val country: String,
    val date: String,
    val time: String? = null,
    val sessions: SessionsDto,
)

@Serializable
data class ScheduleResponse(
    val season: String,
    val races: List<RaceWeekendDto>,
)

@Serializable
data class NextRaceResponse(
    val season: String,
    val race: RaceWeekendDto? = null,
)
