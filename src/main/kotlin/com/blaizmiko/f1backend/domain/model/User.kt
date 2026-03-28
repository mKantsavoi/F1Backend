package com.blaizmiko.f1backend.domain.model

import java.time.Instant
import java.util.UUID

enum class Role { USER, ADMIN }

data class User(
    val id: UUID,
    val email: String,
    val username: String,
    val passwordHash: String,
    val role: Role,
    val createdAt: Instant,
    val updatedAt: Instant,
)
