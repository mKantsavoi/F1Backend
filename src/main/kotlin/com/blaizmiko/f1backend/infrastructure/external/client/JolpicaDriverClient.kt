package com.blaizmiko.f1backend.infrastructure.external.client

import com.blaizmiko.f1backend.domain.model.Circuit
import com.blaizmiko.f1backend.domain.model.Driver
import com.blaizmiko.f1backend.domain.model.Team
import com.blaizmiko.f1backend.domain.port.CircuitDataSource
import com.blaizmiko.f1backend.domain.port.DriverDataSource
import com.blaizmiko.f1backend.domain.port.TeamDataSource
import com.blaizmiko.f1backend.infrastructure.external.JolpicaCircuitResponse
import com.blaizmiko.f1backend.infrastructure.external.JolpicaConstructorResponse
import com.blaizmiko.f1backend.infrastructure.external.JolpicaResponse
import com.blaizmiko.f1backend.infrastructure.external.toDomain
import io.ktor.client.call.body
import io.ktor.client.request.get

class JolpicaDriverClient(
    private val client: JolpicaHttpClient,
) : DriverDataSource,
    TeamDataSource,
    CircuitDataSource {
    private val baseUrl get() = client.config.baseUrl

    override suspend fun fetchDrivers(season: String): Pair<String, List<Driver>> {
        val response: JolpicaResponse = client.http.get("$baseUrl/$season/drivers.json?limit=100").body()
        val driverTable = response.mrData.driverTable
        val drivers = driverTable.drivers.map { it.toDomain() }
        return driverTable.season to drivers
    }

    override suspend fun fetchTeams(season: String): Pair<String, List<Team>> {
        val response: JolpicaConstructorResponse =
            client.http.get("$baseUrl/$season/constructors.json?limit=100").body()
        val table = response.mrData.constructorTable
        val teams = table.constructors.map { it.toDomain() }
        return table.season to teams
    }

    override suspend fun fetchCircuits(): List<Circuit> {
        val response: JolpicaCircuitResponse =
            client.http.get("$baseUrl/circuits.json?limit=100").body()
        return response.mrData.circuitTable.circuits
            .map { it.toDomain() }
    }
}
