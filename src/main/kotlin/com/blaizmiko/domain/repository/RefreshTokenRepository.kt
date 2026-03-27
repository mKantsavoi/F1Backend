package com.blaizmiko.domain.repository

import com.blaizmiko.domain.model.RefreshToken
import java.time.Instant
import java.util.UUID

interface RefreshTokenRepository {
    suspend fun findByTokenHash(tokenHash: String): RefreshToken?
    suspend fun create(userId: UUID, tokenHash: String, expiresAt: Instant): RefreshToken
    suspend fun revokeByTokenHash(tokenHash: String): Boolean
    suspend fun revokeAllForUser(userId: UUID): Int
}
