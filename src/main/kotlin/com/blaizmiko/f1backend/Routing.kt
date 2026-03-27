package com.blaizmiko.f1backend

import com.blaizmiko.f1backend.adapter.dto.ErrorResponse
import com.blaizmiko.f1backend.adapter.route.authRoutes
import com.blaizmiko.f1backend.adapter.route.driverRoutes
import com.blaizmiko.f1backend.domain.model.AuthenticationException
import com.blaizmiko.f1backend.domain.model.ConflictException
import com.blaizmiko.f1backend.domain.model.ExternalServiceException
import com.blaizmiko.f1backend.domain.model.ValidationException
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

fun Application.configureRouting() {
    routing {
        route("/api/v1/auth") {
            authRoutes()
        }
        authenticate {
            route("/api/v1") {
                driverRoutes()
            }
        }
    }
}
