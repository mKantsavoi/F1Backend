package com.blaizmiko.f1backend.domain.model

data class FastestLap(
    val rank: Int,
    val lap: Int,
    val time: String,
    val avgSpeed: String,
)

data class RaceResult(
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
    val fastestLap: FastestLap? = null,
)
