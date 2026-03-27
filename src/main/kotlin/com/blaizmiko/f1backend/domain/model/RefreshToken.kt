package com.blaizmiko.f1backend.domain.model

import java.time.Instant
import java.util.UUID

data class RefreshToken(
    val id: UUID,
    val userId: UUID,
    val tokenHash: String,
    val expiresAt: Instant,
    val revoked: Boolean,
    val createdAt: Instant,
)
