package com.blaizmiko.f1backend.domain.model

data class QualifyingResult(
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
