package com.blaizmiko.usecase

import com.blaizmiko.domain.model.AuthenticationException
import com.blaizmiko.domain.model.ValidationException
import com.blaizmiko.domain.repository.UserRepository
import com.blaizmiko.infrastructure.security.PasswordHasher
import java.util.UUID

class ChangePassword(private val userRepository: UserRepository) {
    suspend fun execute(userId: UUID, currentPassword: String, newPassword: String) {
        val user = userRepository.findById(userId)
            ?: throw AuthenticationException("User not found")

        if (!PasswordHasher.verify(currentPassword, user.passwordHash)) {
            throw AuthenticationException("Current password is incorrect")
        }

        validatePassword(newPassword)

        val newHash = PasswordHasher.hash(newPassword)
        userRepository.updatePasswordHash(userId, newHash)
    }

    private fun validatePassword(password: String) {
        if (password.length < 8) {
            throw ValidationException("Password must be at least 8 characters")
        }
        if (!password.any { it.isLetter() }) {
            throw ValidationException("Password must contain at least one letter")
        }
        if (!password.any { it.isDigit() }) {
            throw ValidationException("Password must contain at least one digit")
        }
    }
}
