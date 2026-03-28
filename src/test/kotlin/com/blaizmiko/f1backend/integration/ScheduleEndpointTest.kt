package com.blaizmiko.f1backend.integration

import com.blaizmiko.f1backend.adapter.dto.ErrorResponse
import com.blaizmiko.f1backend.adapter.dto.ScheduleResponse
import com.blaizmiko.f1backend.adapter.port.ScheduleCache
import com.blaizmiko.f1backend.adapter.route.scheduleRoutes
import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.RaceWeekend
import com.blaizmiko.f1backend.domain.model.SeasonCache
import com.blaizmiko.f1backend.domain.model.Sessions
import com.blaizmiko.f1backend.domain.port.ScheduleDataSource
import com.blaizmiko.f1backend.infrastructure.cache.InMemoryNextRaceCache
import com.blaizmiko.f1backend.infrastructure.cache.InMemoryScheduleCache
import com.blaizmiko.f1backend.usecase.GetNextRace
import com.blaizmiko.f1backend.usecase.GetSchedule
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

class ScheduleEndpointTest :
    StringSpec({

        val sampleRaces =
            listOf(
                RaceWeekend(
                    round = 1,
                    raceName = "Bahrain Grand Prix",
                    circuitId = "bahrain",
                    circuitName = "Bahrain International Circuit",
                    country = "Bahrain",
                    date = "2026-03-15",
                    time = "15:00:00Z",
                    sessions =
                        Sessions(
                            fp1 = "2026-03-13T11:30:00Z",
                            fp2 = "2026-03-13T15:00:00Z",
                            fp3 = "2026-03-14T12:30:00Z",
                            qualifying = "2026-03-14T16:00:00Z",
                            race = "2026-03-15T15:00:00Z",
                        ),
                ),
            )

        class FakeScheduleDataSource(
            private var races: List<RaceWeekend> = emptyList(),
            private var season: String = "2026",
            var shouldFail: Boolean = false,
            var callCount: Int = 0,
        ) : ScheduleDataSource {
            override suspend fun fetchSchedule(season: String): Pair<String, List<RaceWeekend>> {
                callCount++
                if (shouldFail) throw ExternalServiceException("Jolpica unavailable")
                return this.season to races
            }

            override suspend fun fetchNextRace(): Pair<String, RaceWeekend?> {
                callCount++
                if (shouldFail) throw ExternalServiceException("Jolpica unavailable")
                return season to races.firstOrNull()
            }
        }

        fun scheduleTestApp(
            dataSource: ScheduleDataSource,
            cache: ScheduleCache = InMemoryScheduleCache(),
            cacheTtlHours: Long = 24,
            block: suspend ApplicationTestBuilder.() -> Unit,
        ) = testApplication {
            install(ServerContentNegotiation) { json() }
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

            val nextRaceCache = InMemoryNextRaceCache()
            val getSchedule = GetSchedule(cache, dataSource, cacheTtlHours)
            val getNextRace = GetNextRace(nextRaceCache, dataSource)

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

        "happy path: returns current season schedule" {
            val dataSource = FakeScheduleDataSource(races = sampleRaces, season = "2026")
            scheduleTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/schedule") { bearerAuth(token) }

                response.status shouldBe HttpStatusCode.OK
                val body = response.body<ScheduleResponse>()
                body.season shouldBe "2026"
                body.races shouldHaveSize 1
                body.races[0].round shouldBe 1
                body.races[0].raceName shouldBe "Bahrain Grand Prix"
                body.races[0].circuitId shouldBe "bahrain"
                body.races[0].sessions.fp1 shouldBe "2026-03-13T11:30:00Z"
                response.headers[HttpHeaders.Warning] shouldBe null
            }
        }

        "season parameter: valid year returns schedule for that season" {
            val dataSource = FakeScheduleDataSource(races = sampleRaces, season = "2024")
            scheduleTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/schedule?season=2024") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                val body = response.body<ScheduleResponse>()
                body.season shouldBe "2024"
            }
        }

        "401 without token" {
            val dataSource = FakeScheduleDataSource(races = sampleRaces)
            scheduleTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/api/v1/schedule")
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "cache hit: second request does not call data source" {
            val dataSource = FakeScheduleDataSource(races = sampleRaces, season = "2026")
            scheduleTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                client.get("/api/v1/schedule") { bearerAuth(token) }
                dataSource.callCount shouldBe 1

                val response = client.get("/api/v1/schedule") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                dataSource.callCount shouldBe 1
            }
        }

        "stale cache fallback: returns data with Warning header when source fails" {
            val dataSource = FakeScheduleDataSource(races = sampleRaces, season = "2026")
            val cache = InMemoryScheduleCache()

            cache.put(
                "current",
                CacheEntry(
                    data = SeasonCache("2026", sampleRaces),
                    fetchedAt = Instant.now().minusSeconds(90_000),
                    expiresAt = Instant.now().minusSeconds(3_600),
                ),
            )
            dataSource.shouldFail = true

            scheduleTestApp(dataSource, cache) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/schedule") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                response.headers[HttpHeaders.Warning] shouldBe """110 - "Response is stale""""
                val body = response.body<ScheduleResponse>()
                body.races shouldHaveSize 1
            }
        }

        "502 when source fails and no cache exists" {
            val dataSource = FakeScheduleDataSource(shouldFail = true)
            scheduleTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/schedule") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.BadGateway
                val body = response.body<ErrorResponse>()
                body.error shouldBe "external_service_unavailable"
            }
        }

        "400 for invalid season parameter" {
            val dataSource = FakeScheduleDataSource()
            scheduleTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/schedule?season=abc") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.BadRequest

                val response2 = client.get("/api/v1/schedule?season=1800") { bearerAuth(token) }
                response2.status shouldBe HttpStatusCode.BadRequest
            }
        }
    })
