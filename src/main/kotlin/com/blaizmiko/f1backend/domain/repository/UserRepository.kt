package com.blaizmiko.f1backend.domain.repository

import com.blaizmiko.f1backend.domain.model.User
import java.util.UUID

interface UserRepository {
    suspend fun findById(id: UUID): User?
    suspend fun findByEmail(email: String): User?
    suspend fun create(email: String, username: String, passwordHash: String): User
    suspend fun updateUsername(id: UUID, username: String): User?
    suspend fun updatePasswordHash(id: UUID, passwordHash: String): User?
}
