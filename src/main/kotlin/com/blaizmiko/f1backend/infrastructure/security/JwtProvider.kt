package com.blaizmiko.f1backend.infrastructure.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.blaizmiko.f1backend.adapter.dto.ErrorResponse
import com.blaizmiko.f1backend.infrastructure.config.JwtConfig
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import java.util.Date
import java.util.UUID

class JwtProvider(private val config: JwtConfig) {
    private val algorithm = Algorithm.HMAC256(config.secret)

    fun generateAccessToken(userId: UUID, role: String): String =
        JWT.create()
            .withSubject(userId.toString())
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withClaim("role", role)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + config.accessTokenExpirySeconds * 1000))
            .sign(algorithm)

    fun configureAuth(authConfig: AuthenticationConfig) {
        authConfig.jwt {
            verifier(
                JWT.require(algorithm)
                    .withIssuer(this@JwtProvider.config.issuer)
                    .withAudience(this@JwtProvider.config.audience)
                    .build()
            )
            validate { credential ->
                if (credential.payload.subject != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("unauthorized", "Token is invalid or has expired")
                )
            }
        }
    }
}
