package com.blaizmiko.f1backend.integration

import com.blaizmiko.f1backend.adapter.dto.RaceResultsResponse
import com.blaizmiko.f1backend.adapter.port.CacheProvider
import com.blaizmiko.f1backend.adapter.route.raceRoutes
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.RaceResult
import com.blaizmiko.f1backend.domain.port.QualifyingResultsData
import com.blaizmiko.f1backend.domain.port.RaceDataSource
import com.blaizmiko.f1backend.domain.port.RaceResultsData
import com.blaizmiko.f1backend.domain.port.SprintResultsData
import com.blaizmiko.f1backend.infrastructure.cache.CacheRegistry
import com.blaizmiko.f1backend.infrastructure.cache.CacheSpec
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
import com.blaizmiko.f1backend.adapter.dto.ErrorResponse as Err
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class SprintEndpointTest :
    StringSpec({

        val sampleSprintResults =
            listOf(
                RaceResult(
                    position = 1,
                    driverId = "max_verstappen",
                    driverCode = "VER",
                    driverName = "Max Verstappen",
                    teamId = "red_bull",
                    teamName = "Red Bull Racing",
                    grid = 1,
                    laps = 17,
                    time = "25:44.123",
                    points = 8.0,
                    status = "Finished",
                ),
            )

        class FakeRaceDataSource(
            private var sprintResults: SprintResultsData? = null,
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
            ): QualifyingResultsData = throw UnsupportedOperationException("Not used in this test")

            override suspend fun fetchSprintResults(
                season: String,
                round: Int,
            ): SprintResultsData? {
                callCount++
                if (shouldFail) throw ExternalServiceException("Jolpica unavailable")
                return sprintResults
            }
        }

        fun sprintTestApp(
            dataSource: RaceDataSource,
            cacheProvider: CacheProvider = CacheRegistry(),
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

            val getRaceResults = GetRaceResults(cacheProvider, dataSource)
            val getQualifyingResults = GetQualifyingResults(cacheProvider, dataSource)
            val getSprintResults = GetSprintResults(cacheProvider, dataSource)

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

        "happy path: returns sprint results for sprint weekend" {
            val sprintData = SprintResultsData("2025", 4, "Sprint GP", sampleSprintResults)
            val dataSource = FakeRaceDataSource(sprintResults = sprintData)
            sprintTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/races/2025/4/sprint") { bearerAuth(token) }

                response.status shouldBe HttpStatusCode.OK
                val body = response.body<RaceResultsResponse>()
                body.season shouldBe "2025"
                body.round shouldBe 4
                body.results shouldHaveSize 1
                body.results[0].points shouldBe 8.0
                response.headers[HttpHeaders.Warning] shouldBe null
            }
        }

        "404 for non-sprint round" {
            val dataSource = FakeRaceDataSource(sprintResults = null)
            sprintTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/races/2025/1/sprint") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.NotFound
                val body = response.body<Err>()
                body.error shouldBe "not_found"
            }
        }

        "401 without token" {
            val dataSource = FakeRaceDataSource()
            sprintTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/api/v1/races/2025/4/sprint")
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "cache hit: second request does not call data source" {
            val sprintData = SprintResultsData("2025", 4, "Sprint GP", sampleSprintResults)
            val dataSource = FakeRaceDataSource(sprintResults = sprintData)
            sprintTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                client.get("/api/v1/races/2025/4/sprint") { bearerAuth(token) }
                dataSource.callCount shouldBe 1

                client.get("/api/v1/races/2025/4/sprint") { bearerAuth(token) }
                dataSource.callCount shouldBe 1
            }
        }

        "stale cache fallback: returns data with Warning header when source fails" {
            val sprintData = SprintResultsData("2025", 4, "Sprint GP", sampleSprintResults)
            val dataSource = FakeRaceDataSource(sprintResults = sprintData)
            val cacheProvider = CacheRegistry()

            cacheProvider.putFallback(CacheSpec.SPRINT_HISTORICAL, "sprint:2025:4", sprintData)
            dataSource.shouldFail = true

            sprintTestApp(dataSource, cacheProvider) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/races/2025/4/sprint") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                response.headers[HttpHeaders.Warning] shouldBe """110 - "Response is stale""""
            }
        }

        "502 when source fails and no cache exists" {
            val dataSource = FakeRaceDataSource(shouldFail = true)
            sprintTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/races/2025/4/sprint") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.BadGateway
            }
        }

        "400 for invalid params" {
            val dataSource = FakeRaceDataSource()
            sprintTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/races/abc/4/sprint") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }
    })
