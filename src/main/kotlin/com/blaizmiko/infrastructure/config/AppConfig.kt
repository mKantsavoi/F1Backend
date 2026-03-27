package com.blaizmiko.infrastructure.config

import io.ktor.server.application.*

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
)

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val accessTokenExpirySeconds: Long,
    val refreshTokenExpirySeconds: Long,
)

data class AppConfig(
    val database: DatabaseConfig,
    val jwt: JwtConfig,
)

fun Application.loadAppConfig(): AppConfig {
    val dbConfig = environment.config.config("database")
    val jwtConfig = environment.config.config("jwt")

    return AppConfig(
        database = DatabaseConfig(
            url = dbConfig.property("url").getString(),
            user = dbConfig.property("user").getString(),
            password = dbConfig.property("password").getString(),
        ),
        jwt = JwtConfig(
            secret = jwtConfig.property("secret").getString(),
            issuer = jwtConfig.property("issuer").getString(),
            audience = jwtConfig.property("audience").getString(),
            accessTokenExpirySeconds = jwtConfig.property("accessTokenExpiry").getString().toLong(),
            refreshTokenExpirySeconds = jwtConfig.property("refreshTokenExpiry").getString().toLong(),
        ),
    )
}
