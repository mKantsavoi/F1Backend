package com.blaizmiko.f1backend.integration

import com.blaizmiko.f1backend.adapter.dto.TeamsResponse
import com.blaizmiko.f1backend.adapter.route.teamRoutes
import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.SeasonCache
import com.blaizmiko.f1backend.domain.model.Team
import com.blaizmiko.f1backend.domain.port.TeamCache
import com.blaizmiko.f1backend.domain.port.TeamDataSource
import com.blaizmiko.f1backend.infrastructure.cache.InMemoryTeamCache
import com.blaizmiko.f1backend.usecase.GetTeams
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

class TeamsEndpointTest :
    StringSpec({

        val sampleTeams =
            listOf(
                Team("red_bull", "Red Bull Racing", "Austrian"),
                Team("mercedes", "Mercedes-AMG Petronas", "German"),
            )

        class FakeTeamDataSource(
            private var teams: List<Team> = emptyList(),
            private var season: String = "2026",
            var shouldFail: Boolean = false,
            var callCount: Int = 0,
        ) : TeamDataSource {
            override suspend fun fetchTeams(season: String): Pair<String, List<Team>> {
                callCount++
                if (shouldFail) throw ExternalServiceException("Jolpica unavailable")
                return this.season to teams
            }
        }

        fun teamsTestApp(
            dataSource: TeamDataSource,
            cache: TeamCache = InMemoryTeamCache(),
            cacheTtlHours: Long = 24,
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

            val getTeams = GetTeams(cache, dataSource, cacheTtlHours)

            install(Koin) {
                modules(
                    module {
                        single { getTeams }
                    },
                )
            }

            routing {
                authenticate {
                    route("/api/v1") {
                        teamRoutes()
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

        "happy path: returns current season teams" {
            val dataSource = FakeTeamDataSource(teams = sampleTeams, season = "2026")
            teamsTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response =
                    client.get("/api/v1/teams") {
                        bearerAuth(token)
                    }

                response.status shouldBe HttpStatusCode.OK
                val body = response.body<TeamsResponse>()
                body.season shouldBe "2026"
                body.teams shouldHaveSize 2
                body.teams[0].teamId shouldBe "red_bull"
                body.teams[0].name shouldBe "Red Bull Racing"
                body.teams[0].nationality shouldBe "Austrian"
                response.headers[HttpHeaders.Warning] shouldBe null
            }
        }

        "401 without token" {
            val dataSource = FakeTeamDataSource(teams = sampleTeams)
            teamsTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/api/v1/teams")
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "season parameter: valid year returns teams for that season" {
            val dataSource = FakeTeamDataSource(teams = sampleTeams, season = "2024")
            teamsTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/teams?season=2024") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                val body = response.body<TeamsResponse>()
                body.season shouldBe "2024"
            }
        }

        "season parameter: invalid value returns 400" {
            val dataSource = FakeTeamDataSource()
            teamsTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/teams?season=abc") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.BadRequest

                val response2 = client.get("/api/v1/teams?season=1800") { bearerAuth(token) }
                response2.status shouldBe HttpStatusCode.BadRequest
            }
        }

        "cache hit: second request does not call data source" {
            val dataSource = FakeTeamDataSource(teams = sampleTeams, season = "2026")
            teamsTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                client.get("/api/v1/teams") { bearerAuth(token) }
                dataSource.callCount shouldBe 1

                val response = client.get("/api/v1/teams") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                dataSource.callCount shouldBe 1

                val body = response.body<TeamsResponse>()
                body.season shouldBe "2026"
            }
        }

        "stale cache fallback: returns data with Warning header when source fails" {
            val dataSource = FakeTeamDataSource(teams = sampleTeams, season = "2026")
            val cache = InMemoryTeamCache()

            cache.put(
                "current",
                CacheEntry(
                    data = SeasonCache("2026", sampleTeams),
                    fetchedAt = Instant.now().minusSeconds(90_000),
                    expiresAt = Instant.now().minusSeconds(3_600),
                ),
            )
            dataSource.shouldFail = true

            teamsTestApp(dataSource, cache) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/teams") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                response.headers[HttpHeaders.Warning] shouldBe """110 - "Response is stale""""

                val body = response.body<TeamsResponse>()
                body.teams shouldHaveSize 2
            }
        }

        "502 when source fails and no cache exists" {
            val dataSource = FakeTeamDataSource(shouldFail = true)
            teamsTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/teams") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.BadGateway
                val body = response.body<Err>()
                body.error shouldBe "external_service_unavailable"
            }
        }
    })
