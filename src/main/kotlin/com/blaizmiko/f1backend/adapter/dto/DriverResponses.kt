package com.blaizmiko.f1backend.adapter.dto

import kotlinx.serialization.Serializable

@Serializable
data class DriversResponse(
    val season: String,
    val drivers: List<DriverDto>,
)

@Serializable
data class DriverDto(
    val id: String,
    val number: Int,
    val code: String,
    val firstName: String,
    val lastName: String,
    val nationality: String,
    val dateOfBirth: String? = null,
)
