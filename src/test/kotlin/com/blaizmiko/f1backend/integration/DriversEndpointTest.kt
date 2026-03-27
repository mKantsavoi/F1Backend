package com.blaizmiko.f1backend.integration

import com.blaizmiko.f1backend.adapter.dto.DriversResponse
import com.blaizmiko.f1backend.adapter.dto.ErrorResponse
import com.blaizmiko.f1backend.adapter.route.driverRoutes
import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.Driver
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.SeasonCache
import com.blaizmiko.f1backend.domain.port.DriverCache
import com.blaizmiko.f1backend.domain.port.DriverDataSource
import com.blaizmiko.f1backend.infrastructure.cache.InMemoryDriverCache
import com.blaizmiko.f1backend.usecase.GetDrivers
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
import java.time.LocalDate
import com.blaizmiko.f1backend.adapter.dto.ErrorResponse as Err
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class DriversEndpointTest :
    StringSpec({

        val sampleDrivers =
            listOf(
                Driver("max_verstappen", 1, "VER", "Max", "Verstappen", "Dutch", LocalDate.of(1997, 9, 30)),
                Driver("hamilton", 44, "HAM", "Lewis", "Hamilton", "British", LocalDate.of(1985, 1, 7)),
            )

        class FakeDataSource(
            private var drivers: List<Driver> = emptyList(),
            private var season: String = "2026",
            var shouldFail: Boolean = false,
            var callCount: Int = 0,
        ) : DriverDataSource {
            override suspend fun fetchDrivers(season: String): Pair<String, List<Driver>> {
                callCount++
                if (shouldFail) throw ExternalServiceException("Jolpica unavailable")
                return this.season to drivers
            }
        }

        fun driversTestApp(
            dataSource: DriverDataSource,
            cache: DriverCache = InMemoryDriverCache(),
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

            val getDrivers = GetDrivers(cache, dataSource, cacheTtlHours)

            install(Koin) {
                modules(
                    module {
                        single { getDrivers }
                    },
                )
            }

            routing {
                authenticate {
                    route("/api/v1") {
                        driverRoutes()
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

        "happy path: returns current season drivers" {
            val dataSource = FakeDataSource(drivers = sampleDrivers, season = "2026")
            driversTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response =
                    client.get("/api/v1/drivers") {
                        bearerAuth(token)
                    }

                response.status shouldBe HttpStatusCode.OK
                val body = response.body<DriversResponse>()
                body.season shouldBe "2026"
                body.drivers shouldHaveSize 2
                body.drivers[0].id shouldBe "max_verstappen"
                body.drivers[0].number shouldBe 1
                body.drivers[0].code shouldBe "VER"
                body.drivers[0].firstName shouldBe "Max"
                body.drivers[0].lastName shouldBe "Verstappen"
                body.drivers[0].nationality shouldBe "Dutch"
                body.drivers[0].dateOfBirth shouldBe "1997-09-30"
                response.headers[HttpHeaders.Warning] shouldBe null
            }
        }

        "401 without token" {
            val dataSource = FakeDataSource(drivers = sampleDrivers)
            driversTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/api/v1/drivers")
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "cache hit: second request does not call data source" {
            val dataSource = FakeDataSource(drivers = sampleDrivers, season = "2026")
            driversTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                // First request — fetches from source
                client.get("/api/v1/drivers") { bearerAuth(token) }
                dataSource.callCount shouldBe 1

                // Second request — served from cache
                val response = client.get("/api/v1/drivers") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                dataSource.callCount shouldBe 1

                // Cached response should return resolved season, not "current"
                val body = response.body<DriversResponse>()
                body.season shouldBe "2026"
            }
        }

        "stale cache fallback: returns data with Warning header when source fails" {
            val dataSource = FakeDataSource(drivers = sampleDrivers, season = "2026")
            val cache = InMemoryDriverCache()

            // Pre-populate cache with stale data
            cache.put(
                "current",
                CacheEntry(
                    data = SeasonCache("2026", sampleDrivers),
                    fetchedAt = Instant.now().minusSeconds(90_000),
                    expiresAt = Instant.now().minusSeconds(3_600),
                ),
            )
            dataSource.shouldFail = true

            driversTestApp(dataSource, cache) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/drivers") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                response.headers[HttpHeaders.Warning] shouldBe """110 - "Response is stale""""

                val body = response.body<DriversResponse>()
                body.drivers shouldHaveSize 2
            }
        }

        "502 when source fails and no cache exists" {
            val dataSource = FakeDataSource(shouldFail = true)
            driversTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/drivers") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.BadGateway
                val body = response.body<ErrorResponse>()
                body.error shouldBe "external_service_unavailable"
            }
        }

        "season parameter: valid year returns drivers for that season" {
            val dataSource = FakeDataSource(drivers = sampleDrivers, season = "2024")
            driversTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/drivers?season=2024") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                val body = response.body<DriversResponse>()
                body.season shouldBe "2024"
            }
        }

        "season parameter: invalid value returns 400" {
            val dataSource = FakeDataSource()
            driversTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/drivers?season=abc") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.BadRequest

                val response2 = client.get("/api/v1/drivers?season=1800") { bearerAuth(token) }
                response2.status shouldBe HttpStatusCode.BadRequest
            }
        }
    })
