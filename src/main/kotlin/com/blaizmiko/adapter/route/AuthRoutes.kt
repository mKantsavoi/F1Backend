package com.blaizmiko.adapter.route

import com.blaizmiko.adapter.dto.*
import com.blaizmiko.domain.repository.RefreshTokenRepository
import com.blaizmiko.domain.repository.UserRepository
import com.blaizmiko.infrastructure.config.AppConfig
import com.blaizmiko.infrastructure.security.JwtProvider
import com.blaizmiko.usecase.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.authRoutes(
    userRepository: UserRepository,
    refreshTokenRepository: RefreshTokenRepository,
    jwtProvider: JwtProvider,
    appConfig: AppConfig,
) {
    val registerUser = RegisterUser(userRepository, refreshTokenRepository, jwtProvider, appConfig.jwt)
    val loginUser = LoginUser(userRepository, refreshTokenRepository, jwtProvider, appConfig.jwt)
    val refreshTokens = RefreshTokens(userRepository, refreshTokenRepository, jwtProvider, appConfig.jwt)
    val getProfile = GetProfile(userRepository)
    val updateProfile = UpdateProfile(userRepository)
    val changePassword = ChangePassword(userRepository)
    val logoutUser = LogoutUser(refreshTokenRepository)

    post("/register") {
        val request = call.receive<RegisterRequest>()
        val result = registerUser.execute(request.email, request.username, request.password)
        call.respond(HttpStatusCode.Created, TokenResponse(result.accessToken, result.refreshToken, result.expiresIn))
    }

    post("/login") {
        val request = call.receive<LoginRequest>()
        val result = loginUser.execute(request.email, request.password)
        call.respond(TokenResponse(result.accessToken, result.refreshToken, result.expiresIn))
    }

    post("/refresh") {
        val request = call.receive<RefreshRequest>()
        val result = refreshTokens.execute(request.refreshToken)
        call.respond(TokenResponse(result.accessToken, result.refreshToken, result.expiresIn))
    }

    authenticate {
        post("/logout") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = UUID.fromString(principal.subject)
            val request = call.receive<LogoutRequest>()
            logoutUser.execute(userId, request.refreshToken)
            call.respond(MessageResponse("Successfully logged out"))
        }

        get("/me") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = UUID.fromString(principal.subject)
            val user = getProfile.execute(userId)
            call.respond(ProfileResponse(
                id = user.id.toString(),
                email = user.email,
                username = user.username,
                role = user.role.name.lowercase(),
                createdAt = user.createdAt.toString(),
            ))
        }

        patch("/me") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = UUID.fromString(principal.subject)
            val request = call.receive<UpdateProfileRequest>()
            val user = updateProfile.execute(userId, request.username)
            call.respond(ProfileResponse(
                id = user.id.toString(),
                email = user.email,
                username = user.username,
                role = user.role.name.lowercase(),
                createdAt = user.createdAt.toString(),
            ))
        }

        put("/me/password") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = UUID.fromString(principal.subject)
            val request = call.receive<ChangePasswordRequest>()
            changePassword.execute(userId, request.currentPassword, request.newPassword)
            call.respond(MessageResponse("Password updated successfully"))
        }
    }
}
