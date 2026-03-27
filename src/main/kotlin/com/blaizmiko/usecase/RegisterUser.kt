package com.blaizmiko.usecase

import com.blaizmiko.domain.model.ConflictException
import com.blaizmiko.domain.model.ValidationException
import com.blaizmiko.domain.repository.RefreshTokenRepository
import com.blaizmiko.domain.repository.UserRepository
import com.blaizmiko.infrastructure.config.JwtConfig
import com.blaizmiko.infrastructure.security.JwtProvider
import com.blaizmiko.infrastructure.security.PasswordHasher
import com.blaizmiko.infrastructure.security.TokenHasher
import java.time.Instant

data class TokenPair(val accessToken: String, val refreshToken: String, val expiresIn: Long)

class RegisterUser(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProvider: JwtProvider,
    private val jwtConfig: JwtConfig,
) {
    suspend fun execute(email: String, username: String, password: String): TokenPair {
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
        if (username.length < 3 || username.length > 30) {
            throw ValidationException("Username must be between 3 and 30 characters")
        }
        if (!username.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            throw ValidationException("Username can only contain Latin letters, digits, hyphens, and underscores")
        }
        if (username.matches(Regex("^[0-9]+$"))) {
            throw ValidationException("Username cannot consist solely of digits")
        }
    }

    private fun validatePassword(password: String) {
        if (password.length < 8) {
            throw ValidationException("Password must be at least 8 characters")
        }
        if (!password.any { it.isLetter() }) {
            throw ValidationException("Password must contain at least one letter")
        }
        if (!password.any { it.isDigit() }) {
            throw ValidationException("Password must contain at least one digit")
        }
    }
}
