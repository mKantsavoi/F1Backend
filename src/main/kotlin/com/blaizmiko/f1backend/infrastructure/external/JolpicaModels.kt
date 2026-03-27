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

// Schedule models
@Serializable
data class JolpicaScheduleResponse(
    @SerialName("MRData") val mrData: ScheduleMRData,
)

@Serializable
data class ScheduleMRData(
    @SerialName("RaceTable") val raceTable: RaceTable,
)

@Serializable
data class RaceTable(
    val season: String,
    val round: String = "",
    @SerialName("Races") val races: List<JolpicaRace> = emptyList(),
)

@Serializable
data class JolpicaRace(
    val season: String = "",
    val round: String,
    val raceName: String,
    @SerialName("Circuit") val circuit: JolpicaCircuit,
    val date: String = "",
    val time: String = "",
    @SerialName("FirstPractice") val firstPractice: JolpicaSessionTime? = null,
    @SerialName("SecondPractice") val secondPractice: JolpicaSessionTime? = null,
    @SerialName("ThirdPractice") val thirdPractice: JolpicaSessionTime? = null,
    @SerialName("Qualifying") val qualifying: JolpicaSessionTime? = null,
    @SerialName("Sprint") val sprint: JolpicaSessionTime? = null,
    @SerialName("Results") val results: List<JolpicaRaceResult> = emptyList(),
    @SerialName("QualifyingResults") val qualifyingResults: List<JolpicaQualifyingResult> = emptyList(),
    @SerialName("SprintResults") val sprintResults: List<JolpicaRaceResult> = emptyList(),
)

@Serializable
data class JolpicaSessionTime(
    val date: String = "",
    val time: String = "",
)

// Race results models
@Serializable
data class JolpicaRaceResult(
    val number: String = "",
    val position: String,
    @SerialName("Driver") val driver: JolpicaDriver,
    @SerialName("Constructor") val constructor: JolpicaConstructor,
    val grid: String = "0",
    val laps: String = "0",
    @SerialName("Time") val time: JolpicaResultTime? = null,
    val points: String = "0",
    val status: String = "",
    @SerialName("FastestLap") val fastestLap: JolpicaFastestLap? = null,
)

@Serializable
data class JolpicaResultTime(
    val millis: String = "",
    val time: String = "",
)

@Serializable
data class JolpicaFastestLap(
    val rank: String = "0",
    val lap: String = "0",
    @SerialName("Time") val time: JolpicaLapTime? = null,
    @SerialName("AverageSpeed") val averageSpeed: JolpicaAverageSpeed? = null,
)

@Serializable
data class JolpicaLapTime(
    val time: String = "",
)

@Serializable
data class JolpicaAverageSpeed(
    val units: String = "",
    val speed: String = "",
)

// Qualifying results models
@Serializable
data class JolpicaQualifyingResult(
    val number: String = "",
    val position: String,
    @SerialName("Driver") val driver: JolpicaDriver,
    @SerialName("Constructor") val constructor: JolpicaConstructor,
    @SerialName("Q1") val q1: String = "",
    @SerialName("Q2") val q2: String = "",
    @SerialName("Q3") val q3: String = "",
)
