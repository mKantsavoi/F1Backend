package com.blaizmiko.f1backend.adapter.dto

import kotlinx.serialization.Serializable

@Serializable
data class DriverStandingsResponse(
    val season: String,
    val round: Int,
    val standings: List<DriverStandingDto>,
)

@Serializable
data class DriverStandingDto(
    val position: Int,
    val driverId: String,
    val driverCode: String,
    val driverName: String,
    val nationality: String,
    val teamId: String,
    val teamName: String,
    val points: Double,
    val wins: Int,
)

@Serializable
data class ConstructorStandingsResponse(
    val season: String,
    val round: Int,
    val standings: List<ConstructorStandingDto>,
)

@Serializable
data class ConstructorStandingDto(
    val position: Int,
    val teamId: String,
    val teamName: String,
    val nationality: String,
    val points: Double,
    val wins: Int,
)
