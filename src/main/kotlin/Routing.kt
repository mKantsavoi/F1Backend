package com.blaizmiko

import com.blaizmiko.adapter.dto.ErrorResponse
import com.blaizmiko.adapter.route.authRoutes
import com.blaizmiko.adapter.route.driverRoutes
import com.blaizmiko.domain.model.AuthenticationException
import com.blaizmiko.domain.model.ConflictException
import com.blaizmiko.domain.model.ExternalServiceException
import com.blaizmiko.domain.model.ValidationException
import com.blaizmiko.domain.repository.RefreshTokenRepository
import com.blaizmiko.domain.repository.UserRepository
import com.blaizmiko.infrastructure.cache.InMemoryDriverCache
import com.blaizmiko.infrastructure.config.AppConfig
import com.blaizmiko.infrastructure.external.JolpicaClient
import com.blaizmiko.infrastructure.persistence.repository.ExposedRefreshTokenRepository
import com.blaizmiko.infrastructure.persistence.repository.ExposedUserRepository
import com.blaizmiko.infrastructure.security.JwtProvider
import com.blaizmiko.usecase.GetDrivers
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = false
            isLenient = false
            ignoreUnknownKeys = true
        })
    }
}

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<ValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("validation_error", cause.message))
        }
        exception<AuthenticationException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("invalid_credentials", cause.message))
        }
        exception<ConflictException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse("email_taken", cause.message))
        }
        exception<ExternalServiceException> { call, cause ->
            call.respond(HttpStatusCode.BadGateway, ErrorResponse("external_service_unavailable", cause.message))
        }
    }
}

fun Application.configureRouting(appConfig: AppConfig, jwtProvider: JwtProvider) {
    val userRepository: UserRepository = ExposedUserRepository()
    val refreshTokenRepository: RefreshTokenRepository = ExposedRefreshTokenRepository()

    val jolpicaClient = JolpicaClient(appConfig.jolpica)
    val driverCache = InMemoryDriverCache()
    val getDrivers = GetDrivers(driverCache, jolpicaClient, appConfig.jolpica.cacheTtlHours)

    routing {
        route("/api/v1/auth") {
            authRoutes(userRepository, refreshTokenRepository, jwtProvider, appConfig)
        }
        authenticate {
            route("/api/v1") {
                driverRoutes(getDrivers)
            }
        }
    }
}
