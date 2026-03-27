package com.blaizmiko.f1backend.domain.model

data class DriverStanding(
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
