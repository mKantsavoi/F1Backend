package com.blaizmiko.f1backend.domain.model

import java.time.LocalDate

data class DriverWithTeam(
    val driverId: String,
    val number: Int?,
    val code: String,
    val firstName: String,
    val lastName: String,
    val nationality: String,
    val dateOfBirth: LocalDate?,
    val photoUrl: String?,
    val biography: String?,
    val teamId: String?,
    val teamName: String?,
)
