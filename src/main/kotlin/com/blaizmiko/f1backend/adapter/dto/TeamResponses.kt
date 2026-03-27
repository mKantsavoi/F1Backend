package com.blaizmiko.f1backend.adapter.dto

import kotlinx.serialization.Serializable

@Serializable
data class TeamsResponse(
    val season: String,
    val teams: List<TeamDto>,
)

@Serializable
data class TeamDto(
    val teamId: String,
    val name: String,
    val nationality: String,
)
