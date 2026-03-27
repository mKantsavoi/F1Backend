package com.blaizmiko.f1backend.infrastructure.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JolpicaResponse(
    @SerialName("MRData") val mrData: MRData,
)

@Serializable
data class MRData(
    @SerialName("DriverTable") val driverTable: DriverTable,
)

@Serializable
data class DriverTable(
    val season: String,
    @SerialName("Drivers") val drivers: List<JolpicaDriver>,
)

@Serializable
data class JolpicaDriver(
    val driverId: String,
    val permanentNumber: String = "0",
    val code: String = "",
    val givenName: String,
    val familyName: String,
    val dateOfBirth: String = "",
    val nationality: String = "",
    val url: String = "",
)
