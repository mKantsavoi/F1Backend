package com.blaizmiko.f1backend.integration

import com.blaizmiko.f1backend.adapter.dto.DriverStandingsResponse
import com.blaizmiko.f1backend.adapter.dto.ErrorResponse
import com.blaizmiko.f1backend.adapter.port.DriverStandingsCache
import com.blaizmiko.f1backend.adapter.route.standingsRoutes
import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.ConstructorStanding
import com.blaizmiko.f1backend.domain.model.DriverStanding
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.StandingsData
import com.blaizmiko.f1backend.domain.port.StandingsDataSource
import com.blaizmiko.f1backend.infrastructure.cache.InMemoryConstructorStandingsCache
import com.blaizmiko.f1backend.infrastructure.cache.InMemoryDriverStandingsCache
import com.blaizmiko.f1backend.usecase.GetConstructorStandings
import com.blaizmiko.f1backend.usecase.GetDriverStandings
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

class DriverStandingsEndpointTest :
    StringSpec({

        val sampleDriverStandings =
            listOf(
                DriverStanding(
                    position = 1,
                    driverId = "max_verstappen",
                    driverCode = "VER",
                    driverName = "Max Verstappen",
                    nationality = "Dutch",
                    teamId = "red_bull",
                    teamName = "Red Bull Racing",
                    points = 77.0,
                    wins = 2,
                ),
                DriverStanding(
                    position = 2,
                    driverId = "hamilton",
                    driverCode = "HAM",
                    driverName = "Lewis Hamilton",
                    nationality = "British",
                    teamId = "ferrari",
                    teamName = "Scuderia Ferrari",
                    points = 64.0,
                    wins = 1,
                ),
            )

        class FakeStandingsDataSource(
            private val driverStandings: List<DriverStanding> = emptyList(),
            private val season: String = "2026",
            private val round: Int = 3,
            var shouldFail: Boolean = false,
            var callCount: Int = 0,
        ) : StandingsDataSource {
            override suspend fun fetchDriverStandings(season: String): StandingsData<DriverStanding> {
                callCount++
                if (shouldFail) throw ExternalServiceException("Jolpica unavailable")
                return StandingsData(this.season, round, driverStandings)
            }

            override suspend fun fetchConstructorStandings(season: String): StandingsData<ConstructorStanding> =
                throw UnsupportedOperationException("Not used in driver standings tests")
        }

        fun standingsTestApp(
            dataSource: StandingsDataSource,
            driverCache: DriverStandingsCache = InMemoryDriverStandingsCache(),
            block: suspend ApplicationTestBuilder.() -> Unit,
        ) = testApplication {
            install(ServerContentNegotiation) {
                json()
            }
            install(StatusPages) {
                exception<com.blaizmiko.f1backend.domain.model.ValidationException> { call, cause ->
                    call.respond(HttpStatusCode.BadRequest, Err("validation_error", cause.message))
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

            val getDriverStandings = GetDriverStandings(driverCache, dataSource)
            val getConstructorStandings = GetConstructorStandings(InMemoryConstructorStandingsCache(), dataSource)

            install(Koin) {
                modules(
                    module {
                        single { getDriverStandings }
                        single { getConstructorStandings }
                    },
                )
            }

            routing {
                authenticate {
                    route("/api/v1") {
                        standingsRoutes()
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

        "happy path: returns current season driver standings" {
            val dataSource = FakeStandingsDataSource(driverStandings = sampleDriverStandings)
            standingsTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response =
                    client.get("/api/v1/standings/drivers") {
                        bearerAuth(token)
                    }

                response.status shouldBe HttpStatusCode.OK
                val body = response.body<DriverStandingsResponse>()
                body.season shouldBe "2026"
                body.round shouldBe 3
                body.standings shouldHaveSize 2
                body.standings[0].position shouldBe 1
                body.standings[0].driverId shouldBe "max_verstappen"
                body.standings[0].driverCode shouldBe "VER"
                body.standings[0].driverName shouldBe "Max Verstappen"
                body.standings[0].nationality shouldBe "Dutch"
                body.standings[0].teamId shouldBe "red_bull"
                body.standings[0].teamName shouldBe "Red Bull Racing"
                body.standings[0].points shouldBe 77.0
                body.standings[0].wins shouldBe 2
                response.headers[HttpHeaders.Warning] shouldBe null
            }
        }

        "401 without token" {
            val dataSource = FakeStandingsDataSource(driverStandings = sampleDriverStandings)
            standingsTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/api/v1/standings/drivers")
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "cache hit: second request does not call data source" {
            val dataSource = FakeStandingsDataSource(driverStandings = sampleDriverStandings)
            standingsTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                client.get("/api/v1/standings/drivers") { bearerAuth(token) }
                dataSource.callCount shouldBe 1

                val response = client.get("/api/v1/standings/drivers") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                dataSource.callCount shouldBe 1

                val body = response.body<DriverStandingsResponse>()
                body.season shouldBe "2026"
            }
        }

        "stale cache fallback: returns data with Warning header when source fails" {
            val dataSource = FakeStandingsDataSource(driverStandings = sampleDriverStandings)
            val cache = InMemoryDriverStandingsCache()

            cache.put(
                "current",
                CacheEntry(
                    data = StandingsData("2026", 3, sampleDriverStandings),
                    fetchedAt = Instant.now().minusSeconds(90_000),
                    expiresAt = Instant.now().minusSeconds(3_600),
                ),
            )
            dataSource.shouldFail = true

            standingsTestApp(dataSource, cache) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/standings/drivers") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                response.headers[HttpHeaders.Warning] shouldBe """110 - "Response is stale""""

                val body = response.body<DriverStandingsResponse>()
                body.standings shouldHaveSize 2
            }
        }

        "502 when source fails and no cache exists" {
            val dataSource = FakeStandingsDataSource(shouldFail = true)
            standingsTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/standings/drivers") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.BadGateway
                val body = response.body<ErrorResponse>()
                body.error shouldBe "external_service_unavailable"
            }
        }

        "season parameter: valid year returns standings for that season" {
            val dataSource = FakeStandingsDataSource(driverStandings = sampleDriverStandings, season = "2024")
            standingsTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/standings/drivers?season=2024") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                val body = response.body<DriverStandingsResponse>()
                body.season shouldBe "2024"
            }
        }

        "season parameter: invalid value returns 400" {
            val dataSource = FakeStandingsDataSource()
            standingsTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/standings/drivers?season=abc") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.BadRequest

                val response2 = client.get("/api/v1/standings/drivers?season=1800") { bearerAuth(token) }
                response2.status shouldBe HttpStatusCode.BadRequest
            }
        }

        "historical season: past season data is cached permanently" {
            val dataSource = FakeStandingsDataSource(driverStandings = sampleDriverStandings, season = "2023")
            standingsTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/standings/drivers?season=2023") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                dataSource.callCount shouldBe 1

                val response2 = client.get("/api/v1/standings/drivers?season=2023") { bearerAuth(token) }
                response2.status shouldBe HttpStatusCode.OK
                dataSource.callCount shouldBe 1

                val body = response2.body<DriverStandingsResponse>()
                body.season shouldBe "2023"
            }
        }
    })
