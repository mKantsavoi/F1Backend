package com.blaizmiko.f1backend.integration

import com.blaizmiko.f1backend.adapter.dto.NextRaceResponse
import com.blaizmiko.f1backend.adapter.port.CacheProvider
import com.blaizmiko.f1backend.adapter.route.scheduleRoutes
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.RaceWeekend
import com.blaizmiko.f1backend.domain.model.Sessions
import com.blaizmiko.f1backend.domain.port.ScheduleDataSource
import com.blaizmiko.f1backend.infrastructure.cache.CacheRegistry
import com.blaizmiko.f1backend.infrastructure.cache.CacheSpec
import com.blaizmiko.f1backend.usecase.GetNextRace
import com.blaizmiko.f1backend.usecase.GetSchedule
import com.blaizmiko.f1backend.usecase.NextRaceData
import io.kotest.core.spec.style.StringSpec
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

class NextRaceEndpointTest :
    StringSpec({

        val sampleRace =
            RaceWeekend(
                round = 3,
                raceName = "Australian Grand Prix",
                circuitId = "albert_park",
                circuitName = "Albert Park Grand Prix Circuit",
                country = "Australia",
                date = "2026-03-29",
                time = "14:00:00Z",
                sessions =
                    Sessions(
                        fp1 = "2026-03-27T01:30:00Z",
                        qualifying = "2026-03-28T05:00:00Z",
                        race = "2026-03-29T14:00:00Z",
                    ),
            )

        class FakeScheduleDataSource(
            private var nextRace: RaceWeekend? = null,
            private var season: String = "2026",
            var shouldFail: Boolean = false,
            var callCount: Int = 0,
        ) : ScheduleDataSource {
            override suspend fun fetchSchedule(season: String): Pair<String, List<RaceWeekend>> =
                throw UnsupportedOperationException("Not used in this test")

            override suspend fun fetchNextRace(): Pair<String, RaceWeekend?> {
                callCount++
                if (shouldFail) throw ExternalServiceException("Jolpica unavailable")
                return season to nextRace
            }
        }

        fun nextRaceTestApp(
            dataSource: ScheduleDataSource,
            cacheProvider: CacheProvider = CacheRegistry(),
            block: suspend ApplicationTestBuilder.() -> Unit,
        ) = testApplication {
            install(ServerContentNegotiation) { json() }
            install(StatusPages) {
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

            val getSchedule = GetSchedule(cacheProvider, dataSource, 24)
            val getNextRace = GetNextRace(cacheProvider, dataSource)

            install(Koin) {
                modules(
                    module {
                        single { getSchedule }
                        single { getNextRace }
                    },
                )
            }

            routing {
                authenticate {
                    route("/api/v1") {
                        scheduleRoutes()
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

        "happy path: returns next race" {
            val dataSource = FakeScheduleDataSource(nextRace = sampleRace, season = "2026")
            nextRaceTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/schedule/next") { bearerAuth(token) }

                response.status shouldBe HttpStatusCode.OK
                val body = response.body<NextRaceResponse>()
                body.season shouldBe "2026"
                body.race?.raceName shouldBe "Australian Grand Prix"
                body.race?.round shouldBe 3
                response.headers[HttpHeaders.Warning] shouldBe null
            }
        }

        "401 without token" {
            val dataSource = FakeScheduleDataSource(nextRace = sampleRace)
            nextRaceTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/api/v1/schedule/next")
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "cache hit: second request does not call data source" {
            val dataSource = FakeScheduleDataSource(nextRace = sampleRace, season = "2026")
            nextRaceTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                client.get("/api/v1/schedule/next") { bearerAuth(token) }
                dataSource.callCount shouldBe 1

                client.get("/api/v1/schedule/next") { bearerAuth(token) }
                dataSource.callCount shouldBe 1
            }
        }

        "stale cache fallback: returns data with Warning header when source fails" {
            val dataSource = FakeScheduleDataSource(nextRace = sampleRace, season = "2026")
            val cacheProvider = CacheRegistry()

            cacheProvider.putFallback(CacheSpec.SCHEDULE_NEXT, "next", NextRaceData("2026", sampleRace))
            dataSource.shouldFail = true

            nextRaceTestApp(dataSource, cacheProvider) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/schedule/next") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                response.headers[HttpHeaders.Warning] shouldBe """110 - "Response is stale""""
            }
        }

        "502 when source fails and no cache exists" {
            val dataSource = FakeScheduleDataSource(shouldFail = true)
            nextRaceTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/schedule/next") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.BadGateway
            }
        }

        "null race when season ended" {
            val dataSource = FakeScheduleDataSource(nextRace = null, season = "2026")
            nextRaceTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/schedule/next") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                val body = response.body<NextRaceResponse>()
                body.race shouldBe null
            }
        }
    })
