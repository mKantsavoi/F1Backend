package com.blaizmiko.f1backend.infrastructure.external.client

import com.blaizmiko.f1backend.domain.port.QualifyingResultsData
import com.blaizmiko.f1backend.domain.port.RaceDataSource
import com.blaizmiko.f1backend.domain.port.RaceResultsData
import com.blaizmiko.f1backend.domain.port.SprintResultsData
import com.blaizmiko.f1backend.infrastructure.external.JolpicaScheduleResponse
import com.blaizmiko.f1backend.infrastructure.external.toDomainQualifyingResult
import com.blaizmiko.f1backend.infrastructure.external.toDomainRaceResult
import io.ktor.client.call.body
import io.ktor.client.request.get

class JolpicaRaceClient(
    private val client: JolpicaHttpClient,
) : RaceDataSource {
    private val baseUrl get() = client.config.baseUrl

    override suspend fun fetchRaceResults(
        season: String,
        round: Int,
    ): RaceResultsData {
        val response: JolpicaScheduleResponse =
            client.http.get("$baseUrl/$season/$round/results.json?limit=100").body()
        val table = response.mrData.raceTable
        val race = table.races.firstOrNull()
        return RaceResultsData(
            season = table.season,
            round = round,
            raceName = race?.raceName ?: "",
            results = race?.results?.map { it.toDomainRaceResult() } ?: emptyList(),
        )
    }

    override suspend fun fetchQualifyingResults(
        season: String,
        round: Int,
    ): QualifyingResultsData {
        val response: JolpicaScheduleResponse =
            client.http.get("$baseUrl/$season/$round/qualifying.json?limit=100").body()
        val table = response.mrData.raceTable
        val race = table.races.firstOrNull()
        return QualifyingResultsData(
            season = table.season,
            round = round,
            raceName = race?.raceName ?: "",
            qualifying = race?.qualifyingResults?.map { it.toDomainQualifyingResult() } ?: emptyList(),
        )
    }

    override suspend fun fetchSprintResults(
        season: String,
        round: Int,
    ): SprintResultsData? {
        val response: JolpicaScheduleResponse =
            client.http.get("$baseUrl/$season/$round/sprint.json?limit=100").body()
        val table = response.mrData.raceTable
        val race = table.races.firstOrNull()
        val results = race?.sprintResults ?: emptyList()
        if (results.isEmpty()) return null
        return SprintResultsData(
            season = table.season,
            round = round,
            raceName = race?.raceName ?: "",
            results = results.map { it.toDomainRaceResult() },
        )
    }
}
