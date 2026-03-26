package com.blaizmiko.usecase

import com.blaizmiko.domain.model.AuthenticationException
import com.blaizmiko.domain.model.User
import com.blaizmiko.domain.repository.UserRepository
import java.util.UUID

class GetProfile(private val userRepository: UserRepository) {
    suspend fun execute(userId: UUID): User {
        return userRepository.findById(userId)
            ?: throw AuthenticationException("User not found")
    }
}
