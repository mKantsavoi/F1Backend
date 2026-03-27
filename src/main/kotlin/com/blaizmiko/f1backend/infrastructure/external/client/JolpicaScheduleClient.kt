package com.blaizmiko.f1backend.infrastructure.external.client

import com.blaizmiko.f1backend.domain.model.RaceWeekend
import com.blaizmiko.f1backend.domain.port.ScheduleDataSource
import com.blaizmiko.f1backend.infrastructure.external.JolpicaScheduleResponse
import com.blaizmiko.f1backend.infrastructure.external.toDomainRaceWeekend
import io.ktor.client.call.body
import io.ktor.client.request.get

class JolpicaScheduleClient(
    private val client: JolpicaHttpClient,
) : ScheduleDataSource {
    private val baseUrl get() = client.config.baseUrl

    override suspend fun fetchSchedule(season: String): Pair<String, List<RaceWeekend>> {
        val response: JolpicaScheduleResponse =
            client.http.get("$baseUrl/$season.json?limit=100").body()
        val table = response.mrData.raceTable
        val races = table.races.map { it.toDomainRaceWeekend() }
        return table.season to races
    }

    override suspend fun fetchNextRace(): Pair<String, RaceWeekend?> {
        val response: JolpicaScheduleResponse =
            client.http.get("$baseUrl/current/next.json").body()
        val table = response.mrData.raceTable
        val race = table.races.firstOrNull()?.toDomainRaceWeekend()
        return table.season to race
    }
}
