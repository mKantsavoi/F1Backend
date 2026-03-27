package com.blaizmiko.f1backend.adapter.route

import com.blaizmiko.f1backend.adapter.dto.ChangePasswordRequest
import com.blaizmiko.f1backend.adapter.dto.LoginRequest
import com.blaizmiko.f1backend.adapter.dto.LogoutRequest
import com.blaizmiko.f1backend.adapter.dto.MessageResponse
import com.blaizmiko.f1backend.adapter.dto.ProfileResponse
import com.blaizmiko.f1backend.adapter.dto.RefreshRequest
import com.blaizmiko.f1backend.adapter.dto.RegisterRequest
import com.blaizmiko.f1backend.adapter.dto.TokenResponse
import com.blaizmiko.f1backend.adapter.dto.UpdateProfileRequest
import com.blaizmiko.f1backend.usecase.ChangePassword
import com.blaizmiko.f1backend.usecase.GetProfile
import com.blaizmiko.f1backend.usecase.LoginUser
import com.blaizmiko.f1backend.usecase.LogoutUser
import com.blaizmiko.f1backend.usecase.RefreshTokens
import com.blaizmiko.f1backend.usecase.RegisterUser
import com.blaizmiko.f1backend.usecase.UpdateProfile
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.authRoutes() {
    val registerUser by inject<RegisterUser>()
    val loginUser by inject<LoginUser>()
    val refreshTokens by inject<RefreshTokens>()
    val getProfile by inject<GetProfile>()
    val updateProfile by inject<UpdateProfile>()
    val changePassword by inject<ChangePassword>()
    val logoutUser by inject<LogoutUser>()

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
            call.respond(
                ProfileResponse(
                    id = user.id.toString(),
                    email = user.email,
                    username = user.username,
                    role = user.role.name.lowercase(),
                    createdAt = user.createdAt.toString(),
                ),
            )
        }

        patch("/me") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = UUID.fromString(principal.subject)
            val request = call.receive<UpdateProfileRequest>()
            val user = updateProfile.execute(userId, request.username)
            call.respond(
                ProfileResponse(
                    id = user.id.toString(),
                    email = user.email,
                    username = user.username,
                    role = user.role.name.lowercase(),
                    createdAt = user.createdAt.toString(),
                ),
            )
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
