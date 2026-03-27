package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.domain.model.AuthenticationException
import com.blaizmiko.f1backend.domain.model.User
import com.blaizmiko.f1backend.domain.repository.UserRepository
import java.util.UUID

class GetProfile(private val userRepository: UserRepository) {
    suspend fun execute(userId: UUID): User {
        return userRepository.findById(userId)
            ?: throw AuthenticationException("User not found")
    }
}
