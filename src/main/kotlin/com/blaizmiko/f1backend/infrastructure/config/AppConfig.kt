package com.blaizmiko.f1backend.infrastructure.config

import io.ktor.server.application.Application

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int = DEFAULT_MAX_POOL_SIZE,
) {
    companion object {
        const val DEFAULT_MAX_POOL_SIZE = 10
    }
}

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val accessTokenExpirySeconds: Long,
    val refreshTokenExpirySeconds: Long,
)

data class JolpicaConfig(
    val baseUrl: String,
    val requestTimeoutMs: Long,
    val connectTimeoutMs: Long,
    val cacheTtlHours: Long,
)

data class AppConfig(
    val database: DatabaseConfig,
    val jwt: JwtConfig,
    val jolpica: JolpicaConfig,
)

fun Application.loadAppConfig(): AppConfig {
    val dbConfig = environment.config.config("database")
    val jwtConfig = environment.config.config("jwt")
    val jolpicaConfig = environment.config.config("jolpica")

    return AppConfig(
        database =
            DatabaseConfig(
                url = dbConfig.property("url").getString(),
                user = dbConfig.property("user").getString(),
                password = dbConfig.property("password").getString(),
                maxPoolSize =
                    dbConfig.propertyOrNull("maxPoolSize")?.getString()?.toInt()
                        ?: DatabaseConfig.DEFAULT_MAX_POOL_SIZE,
            ),
        jwt =
            JwtConfig(
                secret = jwtConfig.property("secret").getString(),
                issuer = jwtConfig.property("issuer").getString(),
                audience = jwtConfig.property("audience").getString(),
                accessTokenExpirySeconds = jwtConfig.property("accessTokenExpiry").getString().toLong(),
                refreshTokenExpirySeconds = jwtConfig.property("refreshTokenExpiry").getString().toLong(),
            ),
        jolpica =
            JolpicaConfig(
                baseUrl = jolpicaConfig.property("baseUrl").getString(),
                requestTimeoutMs = jolpicaConfig.property("requestTimeoutMs").getString().toLong(),
                connectTimeoutMs = jolpicaConfig.property("connectTimeoutMs").getString().toLong(),
                cacheTtlHours = jolpicaConfig.property("cacheTtlHours").getString().toLong(),
            ),
    )
}
