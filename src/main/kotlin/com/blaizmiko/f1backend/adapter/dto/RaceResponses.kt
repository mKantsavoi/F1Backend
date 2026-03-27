package com.blaizmiko.f1backend.adapter.dto

import kotlinx.serialization.Serializable

@Serializable
data class FastestLapDto(
    val rank: Int,
    val lap: Int,
    val time: String,
    val avgSpeed: String,
)

@Serializable
data class RaceResultDto(
    val position: Int,
    val driverId: String,
    val driverCode: String,
    val driverName: String,
    val teamId: String,
    val teamName: String,
    val grid: Int,
    val laps: Int,
    val time: String? = null,
    val points: Double,
    val status: String,
    val fastestLap: FastestLapDto? = null,
)

@Serializable
data class RaceResultsResponse(
    val season: String,
    val round: Int,
    val raceName: String,
    val results: List<RaceResultDto>,
)

@Serializable
data class QualifyingResultDto(
    val position: Int,
    val driverId: String,
    val driverCode: String,
    val driverName: String,
    val teamId: String,
    val teamName: String,
    val q1: String? = null,
    val q2: String? = null,
    val q3: String? = null,
)

@Serializable
data class QualifyingResultsResponse(
    val season: String,
    val round: Int,
    val raceName: String,
    val qualifying: List<QualifyingResultDto>,
)
