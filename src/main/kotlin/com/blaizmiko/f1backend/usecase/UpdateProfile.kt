package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.domain.model.AuthenticationException
import com.blaizmiko.f1backend.domain.model.MAX_USERNAME_LENGTH
import com.blaizmiko.f1backend.domain.model.MIN_USERNAME_LENGTH
import com.blaizmiko.f1backend.domain.model.User
import com.blaizmiko.f1backend.domain.model.ValidationException
import com.blaizmiko.f1backend.domain.repository.UserRepository
import java.util.UUID

class UpdateProfile(
    private val userRepository: UserRepository,
) {
    suspend fun execute(
        userId: UUID,
        username: String,
    ): User {
        validateUsername(username)

        return userRepository.updateUsername(userId, username)
            ?: throw AuthenticationException("User not found")
    }

    private fun validateUsername(username: String) {
        if (username.length < MIN_USERNAME_LENGTH || username.length > MAX_USERNAME_LENGTH) {
            throw ValidationException(
                "Username must be between $MIN_USERNAME_LENGTH and $MAX_USERNAME_LENGTH characters",
            )
        }
        if (!username.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            throw ValidationException("Username can only contain Latin letters, digits, hyphens, and underscores")
        }
        if (username.matches(Regex("^[0-9]+$"))) {
            throw ValidationException("Username cannot consist solely of digits")
        }
    }
}
