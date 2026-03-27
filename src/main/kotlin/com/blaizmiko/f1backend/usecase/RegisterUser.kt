package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.domain.model.ConflictException
import com.blaizmiko.f1backend.domain.model.MAX_USERNAME_LENGTH
import com.blaizmiko.f1backend.domain.model.MIN_PASSWORD_LENGTH
import com.blaizmiko.f1backend.domain.model.MIN_USERNAME_LENGTH
import com.blaizmiko.f1backend.domain.model.ValidationException
import com.blaizmiko.f1backend.domain.repository.RefreshTokenRepository
import com.blaizmiko.f1backend.domain.repository.UserRepository
import com.blaizmiko.f1backend.infrastructure.config.JwtConfig
import com.blaizmiko.f1backend.infrastructure.security.JwtProvider
import com.blaizmiko.f1backend.infrastructure.security.PasswordHasher
import com.blaizmiko.f1backend.infrastructure.security.TokenHasher
import java.time.Instant

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)

class RegisterUser(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProvider: JwtProvider,
    private val jwtConfig: JwtConfig,
) {
    suspend fun execute(
        email: String,
        username: String,
        password: String,
    ): TokenPair {
        validateEmail(email)
        validateUsername(username)
        validatePassword(password)

        if (userRepository.findByEmail(email) != null) {
            throw ConflictException("An account with this email already exists")
        }

        val passwordHash = PasswordHasher.hash(password)
        val user = userRepository.create(email, username, passwordHash)

        val rawRefreshToken = TokenHasher.generateToken()
        val tokenHash = TokenHasher.hashToken(rawRefreshToken)
        val expiresAt = Instant.now().plusSeconds(jwtConfig.refreshTokenExpirySeconds)
        refreshTokenRepository.create(user.id, tokenHash, expiresAt)

        val accessToken = jwtProvider.generateAccessToken(user.id, user.role.name.lowercase())

        return TokenPair(accessToken, rawRefreshToken, jwtConfig.accessTokenExpirySeconds)
    }

    private fun validateEmail(email: String) {
        if (!email.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) {
            throw ValidationException("Invalid email format")
        }
    }

    private fun validateUsername(username: String) {
        if (username.length < MIN_USERNAME_LENGTH || username.length > MAX_USERNAME_LENGTH) {
            throw ValidationException(
                "Username must be between $MIN_USERNAME_LENGTH and $MAX_USERNAME_LENGTH characters",
            )
        }
        if (!username.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            throw ValidationException("Username can only contain Latin letters, digits, hyphens, and underscores")
        }
        if (username.matches(Regex("^[0-9]+$"))) {
            throw ValidationException("Username cannot consist solely of digits")
        }
    }

    private fun validatePassword(password: String) {
        if (password.length < MIN_PASSWORD_LENGTH) {
            throw ValidationException("Password must be at least $MIN_PASSWORD_LENGTH characters")
        }
        if (!password.any { it.isLetter() }) {
            throw ValidationException("Password must contain at least one letter")
        }
        if (!password.any { it.isDigit() }) {
            throw ValidationException("Password must contain at least one digit")
        }
    }
}
