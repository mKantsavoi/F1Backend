package com.blaizmiko.f1backend.infrastructure.external.client

import com.blaizmiko.f1backend.domain.model.ConstructorStanding
import com.blaizmiko.f1backend.domain.model.DriverStanding
import com.blaizmiko.f1backend.domain.model.StandingsData
import com.blaizmiko.f1backend.domain.port.StandingsDataSource
import com.blaizmiko.f1backend.infrastructure.external.JolpicaConstructorStandingsResponse
import com.blaizmiko.f1backend.infrastructure.external.JolpicaDriverStandingsResponse
import com.blaizmiko.f1backend.infrastructure.external.toDomainConstructorStanding
import com.blaizmiko.f1backend.infrastructure.external.toDomainDriverStanding
import io.ktor.client.call.body
import io.ktor.client.request.get

class JolpicaStandingsClient(
    private val client: JolpicaHttpClient,
) : StandingsDataSource {
    private val baseUrl get() = client.config.baseUrl

    override suspend fun fetchDriverStandings(season: String): StandingsData<DriverStanding> {
        val response: JolpicaDriverStandingsResponse =
            client.http.get("$baseUrl/$season/driverstandings.json?limit=100").body()
        val table = response.mrData.standingsTable
        val list = table.standingsLists.firstOrNull()
        return StandingsData(
            season = list?.season ?: table.season,
            round = list?.round?.toIntOrNull() ?: 0,
            standings = list?.driverStandings?.map { it.toDomainDriverStanding() } ?: emptyList(),
        )
    }

    override suspend fun fetchConstructorStandings(season: String): StandingsData<ConstructorStanding> {
        val response: JolpicaConstructorStandingsResponse =
            client.http.get("$baseUrl/$season/constructorstandings.json?limit=100").body()
        val table = response.mrData.standingsTable
        val list = table.standingsLists.firstOrNull()
        return StandingsData(
            season = list?.season ?: table.season,
            round = list?.round?.toIntOrNull() ?: 0,
            standings = list?.constructorStandings?.map { it.toDomainConstructorStanding() } ?: emptyList(),
        )
    }
}
