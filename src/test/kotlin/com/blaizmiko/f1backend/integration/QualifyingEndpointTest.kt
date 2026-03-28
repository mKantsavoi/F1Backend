package com.blaizmiko.f1backend.integration

import com.blaizmiko.f1backend.adapter.dto.QualifyingResultsResponse
import com.blaizmiko.f1backend.adapter.port.RaceResultCache
import com.blaizmiko.f1backend.adapter.route.raceRoutes
import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.QualifyingResult
import com.blaizmiko.f1backend.domain.port.QualifyingResultsData
import com.blaizmiko.f1backend.domain.port.RaceDataSource
import com.blaizmiko.f1backend.domain.port.RaceResultsData
import com.blaizmiko.f1backend.domain.port.SprintResultsData
import com.blaizmiko.f1backend.infrastructure.cache.InMemoryRaceResultCache
import com.blaizmiko.f1backend.usecase.GetQualifyingResults
import com.blaizmiko.f1backend.usecase.GetRaceResults
import com.blaizmiko.f1backend.usecase.GetSprintResults
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.time.Instant
import com.blaizmiko.f1backend.adapter.dto.ErrorResponse as Err
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class QualifyingEndpointTest :
    StringSpec({

        val sampleQualifying =
            listOf(
                QualifyingResult(
                    position = 1,
                    driverId = "max_verstappen",
                    driverCode = "VER",
                    driverName = "Max Verstappen",
                    teamId = "red_bull",
                    teamName = "Red Bull Racing",
                    q1 = "1:30.558",
                    q2 = "1:29.998",
                    q3 = "1:29.179",
                ),
                QualifyingResult(
                    position = 16,
                    driverId = "albon",
                    driverCode = "ALB",
                    driverName = "Alex Albon",
                    teamId = "williams",
                    teamName = "Williams",
                    q1 = "1:31.800",
                    q2 = null,
                    q3 = null,
                ),
            )

        class FakeRaceDataSource(
            private var qualifying: List<QualifyingResult> = emptyList(),
            var shouldFail: Boolean = false,
            var callCount: Int = 0,
        ) : RaceDataSource {
            override suspend fun fetchRaceResults(
                season: String,
                round: Int,
            ): RaceResultsData = throw UnsupportedOperationException("Not used in this test")

            override suspend fun fetchQualifyingResults(
                season: String,
                round: Int,
            ): QualifyingResultsData {
                callCount++
                if (shouldFail) throw ExternalServiceException("Jolpica unavailable")
                return QualifyingResultsData(season, round, "Test Grand Prix", qualifying)
            }

            override suspend fun fetchSprintResults(
                season: String,
                round: Int,
            ): SprintResultsData? = throw UnsupportedOperationException("Not used in this test")
        }

        fun qualifyingTestApp(
            dataSource: RaceDataSource,
            cache: RaceResultCache = InMemoryRaceResultCache(),
            block: suspend ApplicationTestBuilder.() -> Unit,
        ) = testApplication {
            install(ServerContentNegotiation) { json() }
            install(StatusPages) {
                exception<com.blaizmiko.f1backend.domain.model.ValidationException> { call, cause ->
                    call.respond(HttpStatusCode.BadRequest, Err("validation_error", cause.message))
                }
                exception<com.blaizmiko.f1backend.domain.model.NotFoundException> { call, cause ->
                    call.respond(HttpStatusCode.NotFound, Err("not_found", cause.message))
                }
                exception<ExternalServiceException> { call, cause ->
                    call.respond(HttpStatusCode.BadGateway, Err("external_service_unavailable", cause.message))
                }
            }
            install(Authentication) {
                jwt {
                    realm = "test"
                    verifier(
                        com.auth0.jwt.JWT
                            .require(
                                com.auth0.jwt.algorithms.Algorithm
                                    .HMAC256("test-secret-that-is-at-least-256-bits-long-for-hmac"),
                            ).withIssuer("f1backend")
                            .withAudience("f1backend-api")
                            .build(),
                    )
                    validate { credential ->
                        if (credential.payload.subject != null) JWTPrincipal(credential.payload) else null
                    }
                    challenge { _, _ ->
                        call.respond(HttpStatusCode.Unauthorized, Err("unauthorized", "Token is missing or invalid"))
                    }
                }
            }

            val getRaceResults = GetRaceResults(cache, dataSource)
            val getQualifyingResults = GetQualifyingResults(cache, dataSource)
            val getSprintResults = GetSprintResults(cache, dataSource)

            install(Koin) {
                modules(
                    module {
                        single { getRaceResults }
                        single { getQualifyingResults }
                        single { getSprintResults }
                    },
                )
            }

            routing {
                authenticate {
                    route("/api/v1") {
                        raceRoutes()
                    }
                }
            }

            block()
        }

        fun generateToken(): String =
            com.auth0.jwt.JWT
                .create()
                .withSubject("00000000-0000-0000-0000-000000000001")
                .withIssuer("f1backend")
                .withAudience("f1backend-api")
                .withClaim("role", "user")
                .withExpiresAt(java.util.Date(System.currentTimeMillis() + 900_000))
                .sign(
                    com.auth0.jwt.algorithms.Algorithm
                        .HMAC256("test-secret-that-is-at-least-256-bits-long-for-hmac"),
                )

        "happy path: returns qualifying results with Q1/Q2/Q3" {
            val dataSource = FakeRaceDataSource(qualifying = sampleQualifying)
            qualifyingTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/races/2025/1/qualifying") { bearerAuth(token) }

                response.status shouldBe HttpStatusCode.OK
                val body = response.body<QualifyingResultsResponse>()
                body.season shouldBe "2025"
                body.round shouldBe 1
                body.qualifying shouldHaveSize 2
                body.qualifying[0].q3 shouldBe "1:29.179"
                response.headers[HttpHeaders.Warning] shouldBe null
            }
        }

        "null Q2/Q3 for eliminated drivers" {
            val dataSource = FakeRaceDataSource(qualifying = sampleQualifying)
            qualifyingTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/races/2025/1/qualifying") { bearerAuth(token) }
                val body = response.body<QualifyingResultsResponse>()
                body.qualifying[1].q1 shouldBe "1:31.800"
                body.qualifying[1].q2 shouldBe null
                body.qualifying[1].q3 shouldBe null
            }
        }

        "401 without token" {
            val dataSource = FakeRaceDataSource(qualifying = sampleQualifying)
            qualifyingTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/api/v1/races/2025/1/qualifying")
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "cache hit: second request does not call data source" {
            val dataSource = FakeRaceDataSource(qualifying = sampleQualifying)
            qualifyingTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                client.get("/api/v1/races/2025/1/qualifying") { bearerAuth(token) }
                dataSource.callCount shouldBe 1

                client.get("/api/v1/races/2025/1/qualifying") { bearerAuth(token) }
                dataSource.callCount shouldBe 1
            }
        }

        "stale cache fallback: returns data with Warning header when source fails" {
            val dataSource = FakeRaceDataSource(qualifying = sampleQualifying)
            val cache = InMemoryRaceResultCache()

            cache.put(
                "qualifying:2025:1",
                CacheEntry(
                    data = QualifyingResultsData("2025", 1, "Test GP", sampleQualifying) as Any,
                    fetchedAt = Instant.now().minusSeconds(90_000),
                    expiresAt = Instant.now().minusSeconds(3_600),
                ),
            )
            dataSource.shouldFail = true

            qualifyingTestApp(dataSource, cache) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/races/2025/1/qualifying") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                response.headers[HttpHeaders.Warning] shouldBe """110 - "Response is stale""""
            }
        }

        "502 when source fails and no cache exists" {
            val dataSource = FakeRaceDataSource(shouldFail = true)
            qualifyingTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/races/2025/1/qualifying") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.BadGateway
            }
        }

        "400 for invalid params" {
            val dataSource = FakeRaceDataSource()
            qualifyingTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/races/abc/1/qualifying") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.BadRequest

                val response2 = client.get("/api/v1/races/2025/0/qualifying") { bearerAuth(token) }
                response2.status shouldBe HttpStatusCode.BadRequest
            }
        }
    })
