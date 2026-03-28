package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.domain.model.AuthenticationException
import com.blaizmiko.f1backend.domain.repository.RefreshTokenRepository
import com.blaizmiko.f1backend.infrastructure.security.TokenHasher
import java.util.UUID

class LogoutUser(
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    suspend fun execute(
        userId: UUID,
        rawRefreshToken: String,
    ) {
        val tokenHash = TokenHasher.hashToken(rawRefreshToken)
        val storedToken =
            refreshTokenRepository.findByTokenHash(tokenHash)
                ?: throw AuthenticationException("Invalid refresh token")

        if (storedToken.userId != userId) {
            throw AuthenticationException("Invalid refresh token")
        }

        refreshTokenRepository.revokeByTokenHash(tokenHash)
    }
}
