package com.blaizmiko.f1backend.domain.port

import com.blaizmiko.f1backend.domain.model.QualifyingResult
import com.blaizmiko.f1backend.domain.model.RaceResult

data class RaceResultsData(
    val season: String,
    val round: Int,
    val raceName: String,
    val results: List<RaceResult>,
)

data class QualifyingResultsData(
    val season: String,
    val round: Int,
    val raceName: String,
    val qualifying: List<QualifyingResult>,
)

data class SprintResultsData(
    val season: String,
    val round: Int,
    val raceName: String,
    val results: List<RaceResult>,
)

interface RaceDataSource {
    suspend fun fetchRaceResults(
        season: String,
        round: Int,
    ): RaceResultsData

    suspend fun fetchQualifyingResults(
        season: String,
        round: Int,
    ): QualifyingResultsData

    suspend fun fetchSprintResults(
        season: String,
        round: Int,
    ): SprintResultsData?
}
