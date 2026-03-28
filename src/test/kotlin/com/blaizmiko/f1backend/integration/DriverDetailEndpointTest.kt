package com.blaizmiko.f1backend.integration

import com.blaizmiko.f1backend.adapter.dto.DriverDetailResponse
import com.blaizmiko.f1backend.adapter.route.driverRoutes
import com.blaizmiko.f1backend.domain.model.Driver
import com.blaizmiko.f1backend.domain.model.NotFoundException
import com.blaizmiko.f1backend.domain.model.Team
import com.blaizmiko.f1backend.infrastructure.persistence.DatabaseFactory
import com.blaizmiko.f1backend.infrastructure.persistence.repository.ExposedDriverRepository
import com.blaizmiko.f1backend.infrastructure.persistence.repository.ExposedTeamRepository
import com.blaizmiko.f1backend.usecase.GetDriverDetail
import com.blaizmiko.f1backend.usecase.GetDrivers
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate
import com.blaizmiko.f1backend.adapter.dto.ErrorResponse as Err
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class DriverDetailEndpointTest :
    StringSpec({

        val postgres =
            PostgreSQLContainer("postgres:16-alpine").apply {
                withDatabaseName("f1backend_test")
                withUsername("test")
                withPassword("test")
            }

        beforeSpec {
            postgres.start()
            DatabaseFactory.init(
                com.blaizmiko.f1backend.infrastructure.config.DatabaseConfig(
                    url = postgres.jdbcUrl,
                    user = postgres.username,
                    password = postgres.password,
                ),
            )

            val teamRepo = ExposedTeamRepository()
            teamRepo.insertAll(
                listOf(
                    Team("red_bull", "Red Bull Racing", "Austrian"),
                    Team("ferrari", "Scuderia Ferrari", "Italian"),
                ),
            )

            val driverRepo = ExposedDriverRepository()
            driverRepo.insertAll(
                listOf(
                    Driver(
                        "max_verstappen",
                        1,
                        "VER",
                        "Max",
                        "Verstappen",
                        "Dutch",
                        LocalDate.of(1997, 9, 30),
                        photoUrl = "https://example.com/ver.webp",
                        teamId = "red_bull",
                        biography = "Max Verstappen is a Dutch racing driver.",
                    ),
                    Driver(
                        "hamilton",
                        44,
                        "HAM",
                        "Lewis",
                        "Hamilton",
                        "British",
                        LocalDate.of(1985, 1, 7),
                        photoUrl = null,
                        teamId = "ferrari",
                        biography = null,
                    ),
                ),
            )
        }

        afterSpec {
            postgres.stop()
        }

        fun detailTestApp(block: suspend ApplicationTestBuilder.() -> Unit) =
            testApplication {
                install(ServerContentNegotiation) { json() }
                install(StatusPages) {
                    exception<NotFoundException> { call, cause ->
                        call.respond(HttpStatusCode.NotFound, Err("not_found", cause.message))
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
                            call.respond(
                                HttpStatusCode.Unauthorized,
                                Err("unauthorized", "Token is missing or invalid"),
                            )
                        }
                    }
                }

                val driverRepo = ExposedDriverRepository()
                val getDrivers = GetDrivers(driverRepo)
                val getDriverDetail = GetDriverDetail(driverRepo)

                install(Koin) {
                    modules(
                        module {
                            single { getDrivers }
                            single { getDriverDetail }
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

        "happy path: returns full driver card with team and biography" {
            detailTestApp {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/drivers/max_verstappen") { bearerAuth(token) }

                response.status shouldBe HttpStatusCode.OK
                val body = response.body<DriverDetailResponse>()
                body.driverId shouldBe "max_verstappen"
                body.number shouldBe 1
                body.code shouldBe "VER"
                body.firstName shouldBe "Max"
                body.lastName shouldBe "Verstappen"
                body.nationality shouldBe "Dutch"
                body.dateOfBirth shouldBe "1997-09-30"
                body.photoUrl shouldBe "https://example.com/ver.webp"
                body.team?.teamId shouldBe "red_bull"
                body.team?.name shouldBe "Red Bull Racing"
                body.biography shouldBe "Max Verstappen is a Dutch racing driver."
            }
        }

        "returns driver with null biography and photoUrl" {
            detailTestApp {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/drivers/hamilton") { bearerAuth(token) }

                response.status shouldBe HttpStatusCode.OK
                val body = response.body<DriverDetailResponse>()
                body.driverId shouldBe "hamilton"
                body.photoUrl shouldBe null
                body.biography shouldBe null
                body.team?.teamId shouldBe "ferrari"
                body.team?.name shouldBe "Scuderia Ferrari"
            }
        }

        "404 for unknown driverId" {
            detailTestApp {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/drivers/unknown_driver") { bearerAuth(token) }

                response.status shouldBe HttpStatusCode.NotFound
                val body = response.body<Err>()
                body.error shouldBe "not_found"
            }
        }

        "401 without token" {
            detailTestApp {
                val client = createClient { install(ContentNegotiation) { json() } }
                val response = client.get("/api/v1/drivers/max_verstappen")
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }
    })
