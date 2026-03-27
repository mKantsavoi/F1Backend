package com.blaizmiko.f1backend.integration

import com.blaizmiko.f1backend.adapter.dto.CircuitsResponse
import com.blaizmiko.f1backend.adapter.route.circuitRoutes
import com.blaizmiko.f1backend.domain.model.CacheEntry
import com.blaizmiko.f1backend.domain.model.Circuit
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.port.CircuitCache
import com.blaizmiko.f1backend.domain.port.CircuitDataSource
import com.blaizmiko.f1backend.infrastructure.cache.InMemoryCircuitCache
import com.blaizmiko.f1backend.usecase.GetCircuits
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

class CircuitsEndpointTest :
    StringSpec({

        val sampleCircuits =
            listOf(
                Circuit(
                    "monza",
                    "Autodromo Nazionale di Monza",
                    "Monza",
                    "Italy",
                    45.6156,
                    9.2811,
                    "https://en.wikipedia.org/wiki/Monza_Circuit",
                ),
                Circuit(
                    "silverstone",
                    "Silverstone Circuit",
                    "Silverstone",
                    "UK",
                    52.0786,
                    -1.0169,
                    "https://en.wikipedia.org/wiki/Silverstone_Circuit",
                ),
            )

        class FakeCircuitDataSource(
            private var circuits: List<Circuit> = emptyList(),
            var shouldFail: Boolean = false,
            var callCount: Int = 0,
        ) : CircuitDataSource {
            override suspend fun fetchCircuits(): List<Circuit> {
                callCount++
                if (shouldFail) throw ExternalServiceException("Jolpica unavailable")
                return circuits
            }
        }

        fun circuitsTestApp(
            dataSource: CircuitDataSource,
            cache: CircuitCache = InMemoryCircuitCache(),
            block: suspend ApplicationTestBuilder.() -> Unit,
        ) = testApplication {
            install(ServerContentNegotiation) {
                json()
            }
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

            val getCircuits = GetCircuits(cache, dataSource)

            install(Koin) {
                modules(
                    module {
                        single { getCircuits }
                    },
                )
            }

            routing {
                authenticate {
                    route("/api/v1") {
                        circuitRoutes()
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

        "happy path: returns all circuits" {
            val dataSource = FakeCircuitDataSource(circuits = sampleCircuits)
            circuitsTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response =
                    client.get("/api/v1/circuits") {
                        bearerAuth(token)
                    }

                response.status shouldBe HttpStatusCode.OK
                val body = response.body<CircuitsResponse>()
                body.circuits shouldHaveSize 2
                body.circuits[0].circuitId shouldBe "monza"
                body.circuits[0].name shouldBe "Autodromo Nazionale di Monza"
                body.circuits[0].locality shouldBe "Monza"
                body.circuits[0].country shouldBe "Italy"
                body.circuits[0].lat shouldBe 45.6156
                body.circuits[0].lng shouldBe 9.2811
                body.circuits[0].url shouldBe "https://en.wikipedia.org/wiki/Monza_Circuit"
                response.headers[HttpHeaders.Warning] shouldBe null
            }
        }

        "401 without token" {
            val dataSource = FakeCircuitDataSource(circuits = sampleCircuits)
            circuitsTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/api/v1/circuits")
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "cache hit: second request does not call data source" {
            val dataSource = FakeCircuitDataSource(circuits = sampleCircuits)
            circuitsTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                client.get("/api/v1/circuits") { bearerAuth(token) }
                dataSource.callCount shouldBe 1

                val response = client.get("/api/v1/circuits") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                dataSource.callCount shouldBe 1

                val body = response.body<CircuitsResponse>()
                body.circuits shouldHaveSize 2
            }
        }

        "stale cache fallback: returns data with Warning header when source fails" {
            val dataSource = FakeCircuitDataSource(circuits = sampleCircuits)
            val cache = InMemoryCircuitCache()

            cache.put(
                CacheEntry(
                    data = sampleCircuits,
                    fetchedAt = Instant.now().minusSeconds(90_000),
                    expiresAt = Instant.now().minusSeconds(3_600),
                ),
            )
            dataSource.shouldFail = true

            circuitsTestApp(dataSource, cache) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/circuits") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.OK
                response.headers[HttpHeaders.Warning] shouldBe """110 - "Response is stale""""

                val body = response.body<CircuitsResponse>()
                body.circuits shouldHaveSize 2
            }
        }

        "502 when source fails and no cache exists" {
            val dataSource = FakeCircuitDataSource(shouldFail = true)
            circuitsTestApp(dataSource) {
                val client = createClient { install(ContentNegotiation) { json() } }
                val token = generateToken()

                val response = client.get("/api/v1/circuits") { bearerAuth(token) }
                response.status shouldBe HttpStatusCode.BadGateway
                val body = response.body<Err>()
                body.error shouldBe "external_service_unavailable"
            }
        }
    })
