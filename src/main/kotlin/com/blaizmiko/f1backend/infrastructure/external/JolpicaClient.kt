package com.blaizmiko.f1backend.infrastructure.external

import com.blaizmiko.f1backend.domain.model.Driver
import com.blaizmiko.f1backend.domain.port.DriverDataSource
import com.blaizmiko.f1backend.infrastructure.config.JolpicaConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.time.LocalDate

class JolpicaClient(private val config: JolpicaConfig) : DriverDataSource, java.io.Closeable {

    private val httpClient = HttpClient(CIO) {
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
            json(Json {
                ignoreUnknownKeys = true
                isLenient = false
            })
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

    private fun JolpicaDriver.toDomain(): Driver = Driver(
        id = driverId,
        number = permanentNumber.toIntOrNull() ?: 0,
        code = code,
        firstName = givenName,
        lastName = familyName,
        nationality = nationality,
        dateOfBirth = if (dateOfBirth.isNotBlank()) LocalDate.parse(dateOfBirth) else null,
    )
}
