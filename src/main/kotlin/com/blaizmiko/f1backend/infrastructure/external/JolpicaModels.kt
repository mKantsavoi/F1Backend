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

// Constructor (team) models
@Serializable
data class JolpicaConstructorResponse(
    @SerialName("MRData") val mrData: ConstructorMRData,
)

@Serializable
data class ConstructorMRData(
    @SerialName("ConstructorTable") val constructorTable: ConstructorTable,
)

@Serializable
data class ConstructorTable(
    val season: String,
    @SerialName("Constructors") val constructors: List<JolpicaConstructor>,
)

@Serializable
data class JolpicaConstructor(
    val constructorId: String,
    val name: String,
    val nationality: String = "",
    val url: String = "",
)

// Circuit models
@Serializable
data class JolpicaCircuitResponse(
    @SerialName("MRData") val mrData: CircuitMRData,
)

@Serializable
data class CircuitMRData(
    @SerialName("CircuitTable") val circuitTable: CircuitTable,
)

@Serializable
data class CircuitTable(
    @SerialName("Circuits") val circuits: List<JolpicaCircuit>,
)

@Serializable
data class JolpicaCircuit(
    val circuitId: String,
    val circuitName: String,
    val url: String = "",
    @SerialName("Location") val location: JolpicaLocation,
)

@Serializable
data class JolpicaLocation(
    val lat: String,
    val long: String,
    val locality: String = "",
    val country: String = "",
)
