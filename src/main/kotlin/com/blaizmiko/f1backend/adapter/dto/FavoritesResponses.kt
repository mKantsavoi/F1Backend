package com.blaizmiko.f1backend.adapter.dto

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteDriversResponse(
    val drivers: List<FavoriteDriverDto>,
)

@Serializable
data class FavoriteDriverDto(
    val driverId: String,
    val number: Int?,
    val code: String,
    val firstName: String,
    val lastName: String,
    val photoUrl: String?,
    val teamName: String?,
    val teamColor: String?,
    val addedAt: String,
)

@Serializable
data class FavoriteTeamsResponse(
    val teams: List<FavoriteTeamDto>,
)

@Serializable
data class FavoriteTeamDto(
    val teamId: String,
    val name: String,
    val nationality: String,
    val drivers: List<TeamDriverDto>,
    val addedAt: String,
)

@Serializable
data class TeamDriverDto(
    val driverId: String,
    val code: String,
    val number: Int?,
)

@Serializable
data class FavoriteStatusResponse(
    val isFavorite: Boolean,
)

@Serializable
data class FavoriteDriverActionResponse(
    val driverId: String,
    val addedAt: String,
)

@Serializable
data class FavoriteTeamActionResponse(
    val teamId: String,
    val addedAt: String,
)

@Serializable
data class PersonalizedFeedResponse(
    val favoriteDrivers: List<FeedDriverDto>,
    val favoriteTeams: List<FeedTeamDto>,
    val relevantNews: List<String>,
)

@Serializable
data class FeedDriverDto(
    val driverId: String,
    val code: String,
    val photoUrl: String?,
    val championshipPosition: Int?,
    val championshipPoints: Double?,
    val lastRace: LastRaceDto?,
)

@Serializable
data class LastRaceDto(
    val name: String,
    val position: Int,
    val points: Double,
)

@Serializable
data class FeedTeamDto(
    val teamId: String,
    val name: String,
    val championshipPosition: Int?,
    val championshipPoints: Double?,
)
