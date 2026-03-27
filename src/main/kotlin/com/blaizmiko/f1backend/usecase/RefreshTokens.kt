package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.domain.model.AuthenticationException
import com.blaizmiko.f1backend.domain.repository.RefreshTokenRepository
import com.blaizmiko.f1backend.domain.repository.UserRepository
import com.blaizmiko.f1backend.infrastructure.config.JwtConfig
import com.blaizmiko.f1backend.infrastructure.security.JwtProvider
import com.blaizmiko.f1backend.infrastructure.security.TokenHasher
import java.time.Instant

class RefreshTokens(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProvider: JwtProvider,
    private val jwtConfig: JwtConfig,
) {
    suspend fun execute(rawRefreshToken: String): TokenPair {
        val tokenHash = TokenHasher.hashToken(rawRefreshToken)
        val storedToken =
            refreshTokenRepository.findByTokenHash(tokenHash)
                ?: throw AuthenticationException("Refresh token is invalid or has been revoked")

        // Reuse detection: if the token was already revoked (rotated), revoke ALL tokens for the user
        if (storedToken.revoked) {
            refreshTokenRepository.revokeAllForUser(storedToken.userId)
            throw AuthenticationException("Refresh token is invalid or has been revoked")
        }

        if (storedToken.expiresAt.isBefore(Instant.now())) {
            throw AuthenticationException("Refresh token is invalid or has been revoked")
        }

        // Revoke the old token (rotation)
        refreshTokenRepository.revokeByTokenHash(tokenHash)

        val user =
            userRepository.findById(storedToken.userId)
                ?: throw AuthenticationException("Refresh token is invalid or has been revoked")

        // Issue new token pair
        val newRawRefreshToken = TokenHasher.generateToken()
        val newTokenHash = TokenHasher.hashToken(newRawRefreshToken)
        val expiresAt = Instant.now().plusSeconds(jwtConfig.refreshTokenExpirySeconds)
        refreshTokenRepository.create(user.id, newTokenHash, expiresAt)

        val accessToken = jwtProvider.generateAccessToken(user.id, user.role.name.lowercase())

        return TokenPair(accessToken, newRawRefreshToken, jwtConfig.accessTokenExpirySeconds)
    }
}
