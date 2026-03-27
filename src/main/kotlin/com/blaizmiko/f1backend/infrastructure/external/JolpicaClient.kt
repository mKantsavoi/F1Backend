package com.blaizmiko.f1backend.infrastructure.external

import com.blaizmiko.f1backend.domain.model.Circuit
import com.blaizmiko.f1backend.domain.model.Driver
import com.blaizmiko.f1backend.domain.model.Team
import com.blaizmiko.f1backend.domain.port.CircuitDataSource
import com.blaizmiko.f1backend.domain.port.DriverDataSource
import com.blaizmiko.f1backend.domain.port.TeamDataSource
import com.blaizmiko.f1backend.infrastructure.config.JolpicaConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.time.LocalDate

class JolpicaClient(
    private val config: JolpicaConfig,
) : DriverDataSource,
    TeamDataSource,
    CircuitDataSource,
    java.io.Closeable {
    private val httpClient =
        HttpClient(CIO) {
            install(HttpRequestRetry) {
                maxRetries = 2
                retryOnServerErrors(maxRetries = 2)
                exponentialDelay()
            }
            install(HttpTimeout) {
                requestTimeoutMillis = config.requestTimeoutMs
                connectTimeoutMillis = config.connectTimeoutMs
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = false
                    },
                )
            }
        }

    override fun close() {
        httpClient.close()
    }

    override suspend fun fetchDrivers(season: String): Pair<String, List<Driver>> {
        val response: JolpicaResponse = httpClient.get("${config.baseUrl}/$season/drivers.json?limit=100").body()
        val driverTable = response.mrData.driverTable
        val drivers = driverTable.drivers.map { it.toDomain() }
        return driverTable.season to drivers
    }

    override suspend fun fetchTeams(season: String): Pair<String, List<Team>> {
        val response: JolpicaConstructorResponse =
            httpClient.get("${config.baseUrl}/$season/constructors.json?limit=100").body()
        val table = response.mrData.constructorTable
        val teams = table.constructors.map { it.toDomain() }
        return table.season to teams
    }

    override suspend fun fetchCircuits(): List<Circuit> {
        val response: JolpicaCircuitResponse =
            httpClient.get("${config.baseUrl}/circuits.json?limit=100").body()
        return response.mrData.circuitTable.circuits
            .map { it.toDomain() }
    }

    private fun JolpicaDriver.toDomain(): Driver =
        Driver(
            id = driverId,
            number = permanentNumber.toIntOrNull() ?: 0,
            code = code,
            firstName = givenName,
            lastName = familyName,
            nationality = nationality,
            dateOfBirth = if (dateOfBirth.isNotBlank()) LocalDate.parse(dateOfBirth) else null,
        )

    private fun JolpicaConstructor.toDomain(): Team =
        Team(
            id = constructorId,
            name = name,
            nationality = nationality,
        )

    private fun JolpicaCircuit.toDomain(): Circuit =
        Circuit(
            id = circuitId,
            name = circuitName,
            locality = location.locality,
            country = location.country,
            lat = location.lat.toDoubleOrNull() ?: 0.0,
            lng = location.long.toDoubleOrNull() ?: 0.0,
            url = url,
        )
}
