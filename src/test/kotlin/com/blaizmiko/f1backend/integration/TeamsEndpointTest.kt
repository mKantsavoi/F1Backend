package com.blaizmiko.f1backend.integration

import com.blaizmiko.f1backend.adapter.dto.TeamsResponse
import com.blaizmiko.f1backend.adapter.route.teamRoutes
import com.blaizmiko.f1backend.domain.model.Team
import com.blaizmiko.f1backend.domain.repository.TeamRepository
import com.blaizmiko.f1backend.infrastructure.persistence.DatabaseFactory
import com.blaizmiko.f1backend.infrastructure.persistence.repository.ExposedTeamRepository
import com.blaizmiko.f1backend.usecase.GetTeams
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
import com.blaizmiko.f1backend.adapter.dto.ErrorResponse as Err
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class TeamsEndpointTest :
    StringSpec({

        val postgres =
            PostgreSQLContainer("postgres:16-alpine").apply {
                withDatabaseName("f1backend_test")
                withUsername("test")
                withPassword("test")
            }

        val sampleTeams =
            listOf(
                Team("red_bull", "Red Bull Racing", "Austrian"),
                Team("mercedes", "Mercedes-AMG Petronas", "German"),
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
        }

        afterSpec {
            postgres.stop()
        }

        fun teamsTestApp(
            repository: TeamRepository = ExposedTeamRepository(),
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

            val getTeams = GetTeams(repository)

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

        "happy path: returns teams from database" {
            val repo = ExposedTeamRepository()
            repo.insertAll(sampleTeams)

            teamsTestApp(repo) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/teams") { bearerAuth(token) }

                response.status shouldBe HttpStatusCode.OK
                val body = response.body<TeamsResponse>()
                body.season shouldBe "current"
                body.teams shouldHaveSize 2
                body.teams[0].teamId shouldBe "red_bull"
                body.teams[0].name shouldBe "Red Bull Racing"
                body.teams[0].nationality shouldBe "Austrian"
            }
        }

        "401 without token" {
            teamsTestApp {
                val client = createClient { install(ContentNegotiation) { json() } }
                val response = client.get("/api/v1/teams")
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }
    })
