package com.blaizmiko.f1backend.integration

import com.blaizmiko.f1backend.adapter.dto.FavoriteDriverActionResponse
import com.blaizmiko.f1backend.adapter.dto.FavoriteDriversResponse
import com.blaizmiko.f1backend.adapter.dto.FavoriteStatusResponse
import com.blaizmiko.f1backend.adapter.dto.FavoriteTeamActionResponse
import com.blaizmiko.f1backend.adapter.dto.FavoriteTeamsResponse
import com.blaizmiko.f1backend.adapter.dto.PersonalizedFeedResponse
import com.blaizmiko.f1backend.adapter.route.favoritesRoutes
import com.blaizmiko.f1backend.domain.model.ConstructorStanding
import com.blaizmiko.f1backend.domain.model.Driver
import com.blaizmiko.f1backend.domain.model.DriverStanding
import com.blaizmiko.f1backend.domain.model.RaceResult
import com.blaizmiko.f1backend.domain.model.StandingsData
import com.blaizmiko.f1backend.domain.model.Team
import com.blaizmiko.f1backend.domain.port.QualifyingResultsData
import com.blaizmiko.f1backend.domain.port.RaceDataSource
import com.blaizmiko.f1backend.domain.port.RaceResultsData
import com.blaizmiko.f1backend.domain.port.SprintResultsData
import com.blaizmiko.f1backend.domain.port.StandingsDataSource
import com.blaizmiko.f1backend.infrastructure.persistence.DatabaseFactory
import com.blaizmiko.f1backend.infrastructure.persistence.repository.ExposedDriverRepository
import com.blaizmiko.f1backend.infrastructure.persistence.repository.ExposedFavoriteRepository
import com.blaizmiko.f1backend.infrastructure.persistence.repository.ExposedTeamRepository
import com.blaizmiko.f1backend.infrastructure.persistence.table.FavoriteDriversTable
import com.blaizmiko.f1backend.infrastructure.persistence.table.FavoriteTeamsTable
import com.blaizmiko.f1backend.infrastructure.persistence.table.UsersTable
import com.blaizmiko.f1backend.usecase.AddFavoriteDriver
import com.blaizmiko.f1backend.usecase.AddFavoriteTeam
import com.blaizmiko.f1backend.usecase.CheckDriverFavorite
import com.blaizmiko.f1backend.usecase.CheckTeamFavorite
import com.blaizmiko.f1backend.usecase.GetConstructorStandings
import com.blaizmiko.f1backend.usecase.GetDriverStandings
import com.blaizmiko.f1backend.usecase.GetFavoriteDrivers
import com.blaizmiko.f1backend.usecase.GetFavoriteTeams
import com.blaizmiko.f1backend.usecase.GetPersonalizedFeed
import com.blaizmiko.f1backend.usecase.GetRaceResults
import com.blaizmiko.f1backend.usecase.RemoveFavoriteDriver
import com.blaizmiko.f1backend.usecase.RemoveFavoriteTeam
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
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
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Instant
import java.time.LocalDate
import com.blaizmiko.f1backend.adapter.dto.ErrorResponse as Err
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages as ServerStatusPages

@Suppress("LargeClass")
class FavoritesEndpointTest :
    StringSpec({

        val postgres =
            PostgreSQLContainer("postgres:16-alpine").apply {
                withDatabaseName("f1backend_test")
                withUsername("test")
                withPassword("test")
            }

        val userAId = "00000000-0000-0000-0000-000000000001"
        val userBId = "00000000-0000-0000-0000-000000000002"

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
                    "https://example.com/ver.webp",
                    "red_bull",
                ),
                Driver(
                    "hamilton",
                    44,
                    "HAM",
                    "Lewis",
                    "Hamilton",
                    "British",
                    LocalDate.of(1985, 1, 7),
                    null,
                    "ferrari",
                ),
            )

        val sampleTeams =
            listOf(
                Team("red_bull", "Red Bull Racing", "Austrian"),
                Team("ferrari", "Scuderia Ferrari", "Italian"),
            )

        val fakeDriverStandings =
            StandingsData(
                season = "2026",
                round = 3,
                standings =
                    listOf(
                        DriverStanding(
                            1,
                            "max_verstappen",
                            "VER",
                            "Verstappen",
                            "Dutch",
                            "red_bull",
                            "Red Bull",
                            75.0,
                            3,
                        ),
                        DriverStanding(
                            2,
                            "hamilton",
                            "HAM",
                            "Hamilton",
                            "British",
                            "ferrari",
                            "Ferrari",
                            60.0,
                            1,
                        ),
                    ),
            )

        val fakeConstructorStandings =
            StandingsData(
                season = "2026",
                round = 3,
                standings =
                    listOf(
                        ConstructorStanding(1, "red_bull", "Red Bull Racing", "Austrian", 120.0, 3),
                        ConstructorStanding(2, "ferrari", "Scuderia Ferrari", "Italian", 95.0, 1),
                    ),
            )

        val fakeRaceResults =
            RaceResultsData(
                season = "2026",
                round = 3,
                raceName = "Australian Grand Prix",
                results =
                    listOf(
                        RaceResult(
                            1,
                            "max_verstappen",
                            "VER",
                            "Verstappen",
                            "red_bull",
                            "Red Bull",
                            2,
                            57,
                            "1:32:00",
                            25.0,
                            "Finished",
                        ),
                        RaceResult(
                            3,
                            "hamilton",
                            "HAM",
                            "Hamilton",
                            "ferrari",
                            "Ferrari",
                            5,
                            57,
                            "1:33:00",
                            15.0,
                            "Finished",
                        ),
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
            transaction {
                val now = Instant.now()
                UsersTable.insert {
                    it[id] = kotlin.uuid.Uuid.parse(userAId)
                    it[email] = "usera@test.com"
                    it[username] = "usera"
                    it[passwordHash] = "fakehash"
                    it[role] = "user"
                    it[createdAt] = now
                    it[updatedAt] = now
                }
                UsersTable.insert {
                    it[id] = kotlin.uuid.Uuid.parse(userBId)
                    it[email] = "userb@test.com"
                    it[username] = "userb"
                    it[passwordHash] = "fakehash"
                    it[role] = "user"
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
            ExposedTeamRepository().insertAll(sampleTeams)
            ExposedDriverRepository().insertAll(sampleDrivers)
        }

        beforeEach {
            transaction {
                FavoriteDriversTable.deleteAll()
                FavoriteTeamsTable.deleteAll()
            }
        }

        afterSpec {
            postgres.stop()
        }

        @Suppress("LongMethod")
        fun favoritesTestApp(block: suspend ApplicationTestBuilder.() -> Unit) =
            testApplication {
                install(ServerContentNegotiation) { json() }
                install(ServerStatusPages) {
                    exception<com.blaizmiko.f1backend.domain.model.NotFoundException> { call, cause ->
                        call.respond(HttpStatusCode.NotFound, Err("not_found", cause.message))
                    }
                    exception<com.blaizmiko.f1backend.domain.model.ValidationException> { call, cause ->
                        call.respond(HttpStatusCode.BadRequest, Err("validation_error", cause.message))
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

                val favoriteRepo = ExposedFavoriteRepository()
                val driverRepo = ExposedDriverRepository()
                val teamRepo = ExposedTeamRepository()

                val fakeStandingsDataSource =
                    object : StandingsDataSource {
                        override suspend fun fetchDriverStandings(season: String) = fakeDriverStandings

                        override suspend fun fetchConstructorStandings(season: String) = fakeConstructorStandings
                    }

                val fakeRaceDataSource =
                    object : RaceDataSource {
                        override suspend fun fetchRaceResults(
                            season: String,
                            round: Int,
                        ) = fakeRaceResults

                        override suspend fun fetchQualifyingResults(
                            season: String,
                            round: Int,
                        ): QualifyingResultsData = throw UnsupportedOperationException()

                        override suspend fun fetchSprintResults(
                            season: String,
                            round: Int,
                        ): SprintResultsData? = null
                    }

                val driverStandingsCache =
                    com.blaizmiko.f1backend.infrastructure.cache
                        .InMemoryDriverStandingsCache()
                val constructorStandingsCache =
                    com.blaizmiko.f1backend.infrastructure.cache
                        .InMemoryConstructorStandingsCache()
                val raceResultCache =
                    com.blaizmiko.f1backend.infrastructure.cache
                        .InMemoryRaceResultCache()

                val getDriverStandings = GetDriverStandings(driverStandingsCache, fakeStandingsDataSource)
                val getConstructorStandings =
                    GetConstructorStandings(constructorStandingsCache, fakeStandingsDataSource)
                val getRaceResults = GetRaceResults(raceResultCache, fakeRaceDataSource)

                install(Koin) {
                    modules(
                        module {
                            single { AddFavoriteDriver(favoriteRepo, driverRepo) }
                            single { RemoveFavoriteDriver(favoriteRepo) }
                            single { AddFavoriteTeam(favoriteRepo, teamRepo) }
                            single { RemoveFavoriteTeam(favoriteRepo) }
                            single { GetFavoriteDrivers(favoriteRepo, driverRepo) }
                            single { GetFavoriteTeams(favoriteRepo, teamRepo, driverRepo) }
                            single { CheckDriverFavorite(favoriteRepo) }
                            single { CheckTeamFavorite(favoriteRepo) }
                            single {
                                GetPersonalizedFeed(
                                    favoriteRepo,
                                    driverRepo,
                                    teamRepo,
                                    getDriverStandings,
                                    getConstructorStandings,
                                    getRaceResults,
                                )
                            }
                        },
                    )
                }

                routing {
                    authenticate {
                        route("/api/v1") {
                            favoritesRoutes()
                        }
                    }
                }

                block()
            }

        fun generateToken(userId: String = userAId): String =
            com.auth0.jwt.JWT
                .create()
                .withSubject(userId)
                .withIssuer("f1backend")
                .withAudience("f1backend-api")
                .withClaim("role", "user")
                .withExpiresAt(java.util.Date(System.currentTimeMillis() + 900_000))
                .sign(
                    com.auth0.jwt.algorithms.Algorithm
                        .HMAC256("test-secret-that-is-at-least-256-bits-long-for-hmac"),
                )

        fun ApplicationTestBuilder.jsonClient() = createClient { install(ContentNegotiation) { json() } }

        // --- Driver Favorites ---

        "POST /favorites/drivers/{driverId} returns 201 on first add" {
            favoritesTestApp {
                val client = jsonClient()
                val response =
                    client.post("/api/v1/favorites/drivers/max_verstappen") {
                        bearerAuth(generateToken())
                    }
                response.status shouldBe HttpStatusCode.Created
                val body = response.body<FavoriteDriverActionResponse>()
                body.driverId shouldBe "max_verstappen"
                body.addedAt.shouldNotBeBlank()
            }
        }

        "POST /favorites/drivers/{driverId} returns 200 on duplicate add (idempotent)" {
            favoritesTestApp {
                val client = jsonClient()
                val token = generateToken()
                client.post("/api/v1/favorites/drivers/hamilton") { bearerAuth(token) }
                val response = client.post("/api/v1/favorites/drivers/hamilton") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                val body = response.body<FavoriteDriverActionResponse>()
                body.driverId shouldBe "hamilton"
            }
        }

        "DELETE /favorites/drivers/{driverId} returns 204" {
            favoritesTestApp {
                val client = jsonClient()
                val token = generateToken()
                client.post("/api/v1/favorites/drivers/max_verstappen") { bearerAuth(token) }
                val response = client.delete("/api/v1/favorites/drivers/max_verstappen") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.NoContent
            }
        }

        "DELETE /favorites/drivers/{nonFavorited} returns 204 (idempotent)" {
            favoritesTestApp {
                val client = jsonClient()
                val response =
                    client.delete("/api/v1/favorites/drivers/hamilton") {
                        bearerAuth(generateToken())
                    }
                response.status shouldBe HttpStatusCode.NoContent
            }
        }

        "POST /favorites/drivers/{nonExistent} returns 404" {
            favoritesTestApp {
                val client = jsonClient()
                val response =
                    client.post("/api/v1/favorites/drivers/nonexistent_driver") {
                        bearerAuth(generateToken())
                    }
                response.status shouldBe HttpStatusCode.NotFound
            }
        }

        "GET /favorites/drivers returns full driver cards with all fields" {
            favoritesTestApp {
                val client = jsonClient()
                val token = generateToken()
                client.post("/api/v1/favorites/drivers/max_verstappen") { bearerAuth(token) }

                val response = client.get("/api/v1/favorites/drivers") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                val body = response.body<FavoriteDriversResponse>()
                body.drivers shouldHaveSize 1
                val driver = body.drivers[0]
                driver.driverId shouldBe "max_verstappen"
                driver.code shouldBe "VER"
                driver.firstName shouldBe "Max"
                driver.lastName shouldBe "Verstappen"
                driver.number shouldBe 1
                driver.teamName shouldBe "Red Bull Racing"
                driver.addedAt.shouldNotBeBlank()
            }
        }

        "GET /favorites/drivers returns empty array for user with no favorites" {
            favoritesTestApp {
                val client = jsonClient()
                val response =
                    client.get("/api/v1/favorites/drivers") {
                        bearerAuth(generateToken(userBId))
                    }
                response.status shouldBe HttpStatusCode.OK
                val body = response.body<FavoriteDriversResponse>()
                body.drivers.shouldBeEmpty()
            }
        }

        "GET /favorites/drivers/check/{driverId} returns true/false correctly" {
            favoritesTestApp {
                val client = jsonClient()
                val token = generateToken()

                val before = client.get("/api/v1/favorites/drivers/check/max_verstappen") { bearerAuth(token) }
                before.body<FavoriteStatusResponse>().isFavorite shouldBe false

                client.post("/api/v1/favorites/drivers/max_verstappen") { bearerAuth(token) }

                val after = client.get("/api/v1/favorites/drivers/check/max_verstappen") { bearerAuth(token) }
                after.body<FavoriteStatusResponse>().isFavorite shouldBe true
            }
        }

        // --- Team Favorites ---

        "POST /favorites/teams/{teamId} returns 201 on first add" {
            favoritesTestApp {
                val client = jsonClient()
                val response =
                    client.post("/api/v1/favorites/teams/red_bull") {
                        bearerAuth(generateToken())
                    }
                response.status shouldBe HttpStatusCode.Created
                val body = response.body<FavoriteTeamActionResponse>()
                body.teamId shouldBe "red_bull"
                body.addedAt.shouldNotBeBlank()
            }
        }

        "POST /favorites/teams/{teamId} returns 200 on duplicate add (idempotent)" {
            favoritesTestApp {
                val client = jsonClient()
                val token = generateToken()
                client.post("/api/v1/favorites/teams/ferrari") { bearerAuth(token) }
                val response = client.post("/api/v1/favorites/teams/ferrari") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
            }
        }

        "POST /favorites/teams/{nonExistent} returns 404" {
            favoritesTestApp {
                val client = jsonClient()
                val response =
                    client.post("/api/v1/favorites/teams/nonexistent_team") {
                        bearerAuth(generateToken())
                    }
                response.status shouldBe HttpStatusCode.NotFound
            }
        }

        "DELETE /favorites/teams/{teamId} returns 204" {
            favoritesTestApp {
                val client = jsonClient()
                val token = generateToken()
                client.post("/api/v1/favorites/teams/red_bull") { bearerAuth(token) }
                val response = client.delete("/api/v1/favorites/teams/red_bull") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.NoContent
            }
        }

        "GET /favorites/teams returns full team cards with driver rosters" {
            favoritesTestApp {
                val client = jsonClient()
                val token = generateToken()
                client.post("/api/v1/favorites/teams/red_bull") { bearerAuth(token) }

                val response = client.get("/api/v1/favorites/teams") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                val body = response.body<FavoriteTeamsResponse>()
                body.teams shouldHaveSize 1
                val team = body.teams[0]
                team.teamId shouldBe "red_bull"
                team.name shouldBe "Red Bull Racing"
                team.nationality shouldBe "Austrian"
                team.drivers shouldHaveSize 1
                team.drivers[0].driverId shouldBe "max_verstappen"
                team.addedAt.shouldNotBeBlank()
            }
        }

        "GET /favorites/teams/check/{teamId} returns true/false correctly" {
            favoritesTestApp {
                val client = jsonClient()
                val token = generateToken()

                val before = client.get("/api/v1/favorites/teams/check/red_bull") { bearerAuth(token) }
                before.body<FavoriteStatusResponse>().isFavorite shouldBe false

                client.post("/api/v1/favorites/teams/red_bull") { bearerAuth(token) }

                val after = client.get("/api/v1/favorites/teams/check/red_bull") { bearerAuth(token) }
                after.body<FavoriteStatusResponse>().isFavorite shouldBe true
            }
        }

        // --- Feed ---

        "GET /favorites/feed returns standings and last race data for favorites" {
            favoritesTestApp {
                val client = jsonClient()
                val token = generateToken()
                client.post("/api/v1/favorites/drivers/max_verstappen") { bearerAuth(token) }
                client.post("/api/v1/favorites/teams/red_bull") { bearerAuth(token) }

                val response = client.get("/api/v1/favorites/feed") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                val body = response.body<PersonalizedFeedResponse>()
                body.favoriteDrivers shouldHaveSize 1
                body.favoriteDrivers[0].driverId shouldBe "max_verstappen"
                body.favoriteDrivers[0].championshipPosition shouldBe 1
                body.favoriteDrivers[0].championshipPoints shouldBe 75.0
                body.favoriteDrivers[0].lastRace shouldNotBe null
                body.favoriteDrivers[0].lastRace!!.name shouldBe "Australian Grand Prix"
                body.favoriteDrivers[0].lastRace!!.position shouldBe 1
                body.favoriteDrivers[0].lastRace!!.points shouldBe 25.0

                body.favoriteTeams shouldHaveSize 1
                body.favoriteTeams[0].teamId shouldBe "red_bull"
                body.favoriteTeams[0].championshipPosition shouldBe 1
                body.favoriteTeams[0].championshipPoints shouldBe 120.0
            }
        }

        "GET /favorites/feed response includes relevantNews as empty array" {
            favoritesTestApp {
                val client = jsonClient()
                val response = client.get("/api/v1/favorites/feed") { bearerAuth(generateToken()) }
                response.status shouldBe HttpStatusCode.OK
                val body = response.body<PersonalizedFeedResponse>()
                body.relevantNews.shouldBeEmpty()
            }
        }

        "GET /favorites/feed returns empty arrays for user with no favorites" {
            favoritesTestApp {
                val client = jsonClient()
                val response =
                    client.get("/api/v1/favorites/feed") {
                        bearerAuth(generateToken(userBId))
                    }
                response.status shouldBe HttpStatusCode.OK
                val body = response.body<PersonalizedFeedResponse>()
                body.favoriteDrivers.shouldBeEmpty()
                body.favoriteTeams.shouldBeEmpty()
                body.relevantNews.shouldBeEmpty()
            }
        }

        // --- Multi-User Isolation (US6) ---

        "multi-user isolation: User A favorites not visible to User B" {
            favoritesTestApp {
                val client = jsonClient()
                val tokenA = generateToken(userAId)
                val tokenB = generateToken(userBId)

                client.post("/api/v1/favorites/drivers/max_verstappen") { bearerAuth(tokenA) }
                client.post("/api/v1/favorites/teams/red_bull") { bearerAuth(tokenA) }

                val driversB = client.get("/api/v1/favorites/drivers") { bearerAuth(tokenB) }
                driversB.body<FavoriteDriversResponse>().drivers.shouldBeEmpty()

                val teamsB = client.get("/api/v1/favorites/teams") { bearerAuth(tokenB) }
                teamsB.body<FavoriteTeamsResponse>().teams.shouldBeEmpty()

                val checkB = client.get("/api/v1/favorites/drivers/check/max_verstappen") { bearerAuth(tokenB) }
                checkB.body<FavoriteStatusResponse>().isFavorite shouldBe false
            }
        }

        // --- Auth ---

        "401 without JWT token on all endpoints" {
            favoritesTestApp {
                val client = jsonClient()
                client.get("/api/v1/favorites/drivers").status shouldBe HttpStatusCode.Unauthorized
                client.post("/api/v1/favorites/drivers/max_verstappen").status shouldBe HttpStatusCode.Unauthorized
                client.delete("/api/v1/favorites/drivers/max_verstappen").status shouldBe HttpStatusCode.Unauthorized
                client.get("/api/v1/favorites/drivers/check/max_verstappen").status shouldBe HttpStatusCode.Unauthorized
                client.get("/api/v1/favorites/teams").status shouldBe HttpStatusCode.Unauthorized
                client.post("/api/v1/favorites/teams/red_bull").status shouldBe HttpStatusCode.Unauthorized
                client.delete("/api/v1/favorites/teams/red_bull").status shouldBe HttpStatusCode.Unauthorized
                client.get("/api/v1/favorites/teams/check/red_bull").status shouldBe HttpStatusCode.Unauthorized
                client.get("/api/v1/favorites/feed").status shouldBe HttpStatusCode.Unauthorized
            }
        }
    })
