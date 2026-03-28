package com.blaizmiko.f1backend.integration

import com.blaizmiko.f1backend.adapter.dto.DriversResponse
import com.blaizmiko.f1backend.adapter.route.driverRoutes
import com.blaizmiko.f1backend.domain.model.Driver
import com.blaizmiko.f1backend.domain.model.Team
import com.blaizmiko.f1backend.domain.repository.DriverRepository
import com.blaizmiko.f1backend.infrastructure.persistence.DatabaseFactory
import com.blaizmiko.f1backend.infrastructure.persistence.repository.ExposedDriverRepository
import com.blaizmiko.f1backend.infrastructure.persistence.repository.ExposedTeamRepository
import com.blaizmiko.f1backend.usecase.GetDriverDetail
import com.blaizmiko.f1backend.usecase.GetDrivers
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
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

class DriversEndpointTest :
    StringSpec({

        val postgres =
            PostgreSQLContainer("postgres:16-alpine").apply {
                withDatabaseName("f1backend_test")
                withUsername("test")
                withPassword("test")
            }

        val sampleDrivers =
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
                ),
            )

        beforeSpec {
            postgres.start()
            DatabaseFactory.init(
                com.blaizmiko.f1backend.infrastructure.config.DatabaseConfig(
                    url = postgres.jdbcUrl,
                    user = postgres.username,
                    password = postgres.password,
                ),
            )
            ExposedTeamRepository().insertAll(
                listOf(
                    Team("red_bull", "Red Bull Racing", "Austrian"),
                    Team("ferrari", "Scuderia Ferrari", "Italian"),
                ),
            )
        }

        afterSpec {
            postgres.stop()
        }

        fun driversTestApp(
            repository: DriverRepository = ExposedDriverRepository(),
            block: suspend ApplicationTestBuilder.() -> Unit,
        ) = testApplication {
            install(ServerContentNegotiation) { json() }
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

            val getDrivers = GetDrivers(repository)
            val getDriverDetail = GetDriverDetail(repository)

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

        "happy path: returns drivers from database with photoUrl" {
            val repo = ExposedDriverRepository()
            repo.insertAll(sampleDrivers)

            driversTestApp(repo) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/drivers") { bearerAuth(token) }

                response.status shouldBe HttpStatusCode.OK
                val body = response.body<DriversResponse>()
                body.season shouldBe "current"
                body.drivers shouldHaveSize 2
                body.drivers[0].id shouldBe "max_verstappen"
                body.drivers[0].number shouldBe 1
                body.drivers[0].code shouldBe "VER"
                body.drivers[0].firstName shouldBe "Max"
                body.drivers[0].lastName shouldBe "Verstappen"
                body.drivers[0].nationality shouldBe "Dutch"
                body.drivers[0].dateOfBirth shouldBe "1997-09-30"
                body.drivers[0].photoUrl shouldBe "https://example.com/ver.webp"
                body.drivers[1].photoUrl shouldBe null
            }
        }

        "401 without token" {
            driversTestApp {
                val client = createClient { install(ContentNegotiation) { json() } }
                val response = client.get("/api/v1/drivers")
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }
    })
