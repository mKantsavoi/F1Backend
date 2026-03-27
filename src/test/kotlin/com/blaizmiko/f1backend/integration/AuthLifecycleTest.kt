package com.blaizmiko.f1backend.integration

import com.blaizmiko.f1backend.adapter.dto.ChangePasswordRequest
import com.blaizmiko.f1backend.adapter.dto.LoginRequest
import com.blaizmiko.f1backend.adapter.dto.LogoutRequest
import com.blaizmiko.f1backend.adapter.dto.ProfileResponse
import com.blaizmiko.f1backend.adapter.dto.RefreshRequest
import com.blaizmiko.f1backend.adapter.dto.RegisterRequest
import com.blaizmiko.f1backend.adapter.dto.TokenResponse
import com.blaizmiko.f1backend.adapter.dto.UpdateProfileRequest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.testcontainers.containers.PostgreSQLContainer

class AuthLifecycleTest :
    StringSpec({

        val postgres =
            PostgreSQLContainer("postgres:16-alpine").apply {
                withDatabaseName("f1backend_test")
                withUsername("test")
                withPassword("test")
            }

        beforeSpec {
            postgres.start()
        }

        afterSpec {
            postgres.stop()
        }

        fun ApplicationTestBuilder.configuredClient() =
            createClient {
                install(ContentNegotiation) { json() }
            }

        fun testApp(block: suspend ApplicationTestBuilder.() -> Unit) =
            testApplication {
                environment {
                    config =
                        io.ktor.server.config.MapApplicationConfig(
                            "ktor.application.modules.size" to "1",
                            "ktor.application.modules.0" to "com.blaizmiko.f1backend.ApplicationKt.module",
                            "database.url" to postgres.jdbcUrl,
                            "database.user" to postgres.username,
                            "database.password" to postgres.password,
                            "jwt.secret" to "test-secret-that-is-at-least-256-bits-long-for-hmac",
                            "jwt.issuer" to "f1backend",
                            "jwt.audience" to "f1backend-api",
                            "jwt.accessTokenExpiry" to "900",
                            "jwt.refreshTokenExpiry" to "2592000",
                            "jolpica.baseUrl" to "https://api.jolpi.ca/ergast/f1",
                            "jolpica.requestTimeoutMs" to "10000",
                            "jolpica.connectTimeoutMs" to "5000",
                            "jolpica.cacheTtlHours" to "24",
                        )
                }
                block()
            }

        "full auth lifecycle: register -> access -> refresh -> access -> logout -> refresh fails" {
            testApp {
                val client = configuredClient()

                // 1. Register
                val registerResponse =
                    client.post("/api/v1/auth/register") {
                        contentType(ContentType.Application.Json)
                        setBody(RegisterRequest("lifecycle@test.com", "lifecycle_user", "Password1"))
                    }
                registerResponse.status shouldBe HttpStatusCode.Created
                val tokens = registerResponse.body<TokenResponse>()
                tokens.accessToken.shouldNotBeBlank()
                tokens.refreshToken.shouldNotBeBlank()
                tokens.expiresIn shouldBe 900

                // 2. Access protected endpoint with access token
                val profileResponse =
                    client.get("/api/v1/auth/me") {
                        bearerAuth(tokens.accessToken)
                    }
                profileResponse.status shouldBe HttpStatusCode.OK
                val profile = profileResponse.body<ProfileResponse>()
                profile.email shouldBe "lifecycle@test.com"
                profile.username shouldBe "lifecycle_user"
                profile.role shouldBe "user"

                // 3. Refresh tokens
                val refreshResponse =
                    client.post("/api/v1/auth/refresh") {
                        contentType(ContentType.Application.Json)
                        setBody(RefreshRequest(tokens.refreshToken))
                    }
                refreshResponse.status shouldBe HttpStatusCode.OK
                val newTokens = refreshResponse.body<TokenResponse>()
                newTokens.accessToken.shouldNotBeBlank()
                newTokens.refreshToken shouldNotBe tokens.refreshToken

                // 4. Access with new access token
                val profileResponse2 =
                    client.get("/api/v1/auth/me") {
                        bearerAuth(newTokens.accessToken)
                    }
                profileResponse2.status shouldBe HttpStatusCode.OK

                // 5. Old refresh token should fail (rotation)
                val oldRefreshResponse =
                    client.post("/api/v1/auth/refresh") {
                        contentType(ContentType.Application.Json)
                        setBody(RefreshRequest(tokens.refreshToken))
                    }
                oldRefreshResponse.status shouldBe HttpStatusCode.Unauthorized

                // 6. After reuse detection, new refresh token should also fail
                val reuseDetectedRefresh =
                    client.post("/api/v1/auth/refresh") {
                        contentType(ContentType.Application.Json)
                        setBody(RefreshRequest(newTokens.refreshToken))
                    }
                reuseDetectedRefresh.status shouldBe HttpStatusCode.Unauthorized

                // 7. Re-login to get fresh tokens
                val loginResponse =
                    client.post("/api/v1/auth/login") {
                        contentType(ContentType.Application.Json)
                        setBody(LoginRequest("lifecycle@test.com", "Password1"))
                    }
                loginResponse.status shouldBe HttpStatusCode.OK
                val freshTokens = loginResponse.body<TokenResponse>()

                // 8. Logout
                val logoutResponse =
                    client.post("/api/v1/auth/logout") {
                        bearerAuth(freshTokens.accessToken)
                        contentType(ContentType.Application.Json)
                        setBody(LogoutRequest(freshTokens.refreshToken))
                    }
                logoutResponse.status shouldBe HttpStatusCode.OK

                // 9. Refresh after logout should fail
                val postLogoutRefresh =
                    client.post("/api/v1/auth/refresh") {
                        contentType(ContentType.Application.Json)
                        setBody(RefreshRequest(freshTokens.refreshToken))
                    }
                postLogoutRefresh.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "registration with invalid data returns 400" {
            testApp {
                val client = configuredClient()

                // Bad email
                val badEmail =
                    client.post("/api/v1/auth/register") {
                        contentType(ContentType.Application.Json)
                        setBody(RegisterRequest("not-an-email", "validuser", "Password1"))
                    }
                badEmail.status shouldBe HttpStatusCode.BadRequest

                // Weak password
                val weakPassword =
                    client.post("/api/v1/auth/register") {
                        contentType(ContentType.Application.Json)
                        setBody(RegisterRequest("weak@test.com", "validuser", "short"))
                    }
                weakPassword.status shouldBe HttpStatusCode.BadRequest

                // Invalid username
                val badUsername =
                    client.post("/api/v1/auth/register") {
                        contentType(ContentType.Application.Json)
                        setBody(RegisterRequest("bad@test.com", "ab", "Password1"))
                    }
                badUsername.status shouldBe HttpStatusCode.BadRequest
            }
        }

        "login with wrong credentials returns 401" {
            testApp {
                val client = configuredClient()

                // Register first
                client.post("/api/v1/auth/register") {
                    contentType(ContentType.Application.Json)
                    setBody(RegisterRequest("login@test.com", "loginuser", "Password1"))
                }

                // Wrong password
                val wrongPass =
                    client.post("/api/v1/auth/login") {
                        contentType(ContentType.Application.Json)
                        setBody(LoginRequest("login@test.com", "WrongPass1"))
                    }
                wrongPass.status shouldBe HttpStatusCode.Unauthorized

                // Non-existent email
                val noUser =
                    client.post("/api/v1/auth/login") {
                        contentType(ContentType.Application.Json)
                        setBody(LoginRequest("nobody@test.com", "Password1"))
                    }
                noUser.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "protected endpoint without token returns 401" {
            testApp {
                val client = configuredClient()

                val response = client.get("/api/v1/auth/me")
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "profile update and password change" {
            testApp {
                val client = configuredClient()

                // Register
                val registerResponse =
                    client.post("/api/v1/auth/register") {
                        contentType(ContentType.Application.Json)
                        setBody(RegisterRequest("profile@test.com", "originalname", "Password1"))
                    }
                val tokens = registerResponse.body<TokenResponse>()

                // Update username
                val updateResponse =
                    client.patch("/api/v1/auth/me") {
                        bearerAuth(tokens.accessToken)
                        contentType(ContentType.Application.Json)
                        setBody(UpdateProfileRequest("newname"))
                    }
                updateResponse.status shouldBe HttpStatusCode.OK
                val updated = updateResponse.body<ProfileResponse>()
                updated.username shouldBe "newname"

                // Change password
                val changePassResponse =
                    client.put("/api/v1/auth/me/password") {
                        bearerAuth(tokens.accessToken)
                        contentType(ContentType.Application.Json)
                        setBody(ChangePasswordRequest("Password1", "NewPassword2"))
                    }
                changePassResponse.status shouldBe HttpStatusCode.OK

                // Login with new password
                val loginResponse =
                    client.post("/api/v1/auth/login") {
                        contentType(ContentType.Application.Json)
                        setBody(LoginRequest("profile@test.com", "NewPassword2"))
                    }
                loginResponse.status shouldBe HttpStatusCode.OK

                // Old password should fail
                val oldPassLogin =
                    client.post("/api/v1/auth/login") {
                        contentType(ContentType.Application.Json)
                        setBody(LoginRequest("profile@test.com", "Password1"))
                    }
                oldPassLogin.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "duplicate email registration returns 409" {
            testApp {
                val client = configuredClient()

                client.post("/api/v1/auth/register") {
                    contentType(ContentType.Application.Json)
                    setBody(RegisterRequest("dupe@test.com", "firstuser", "Password1"))
                }

                val dupeResponse =
                    client.post("/api/v1/auth/register") {
                        contentType(ContentType.Application.Json)
                        setBody(RegisterRequest("dupe@test.com", "seconduser", "Password1"))
                    }
                dupeResponse.status shouldBe HttpStatusCode.Conflict
            }
        }
    })
