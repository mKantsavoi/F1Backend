package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.domain.model.AuthenticationException
import com.blaizmiko.f1backend.domain.repository.RefreshTokenRepository
import com.blaizmiko.f1backend.domain.repository.UserRepository
import com.blaizmiko.f1backend.infrastructure.config.JwtConfig
import com.blaizmiko.f1backend.infrastructure.security.JwtProvider
import com.blaizmiko.f1backend.infrastructure.security.PasswordHasher
import com.blaizmiko.f1backend.infrastructure.security.TokenHasher
import java.time.Instant

class LoginUser(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProvider: JwtProvider,
    private val jwtConfig: JwtConfig,
) {
    suspend fun execute(
        email: String,
        password: String,
    ): TokenPair {
        val user =
            userRepository.findByEmail(email)
                ?: throw AuthenticationException("Invalid email or password")

        if (!PasswordHasher.verify(password, user.passwordHash)) {
            throw AuthenticationException("Invalid email or password")
        }

        val rawRefreshToken = TokenHasher.generateToken()
        val tokenHash = TokenHasher.hashToken(rawRefreshToken)
        val expiresAt = Instant.now().plusSeconds(jwtConfig.refreshTokenExpirySeconds)
        refreshTokenRepository.create(user.id, tokenHash, expiresAt)

        val accessToken = jwtProvider.generateAccessToken(user.id, user.role.name.lowercase())

        return TokenPair(accessToken, rawRefreshToken, jwtConfig.accessTokenExpirySeconds)
    }
}
