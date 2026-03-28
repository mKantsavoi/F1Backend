package com.blaizmiko.f1backend.infrastructure.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object TokenHasher {
    private const val TOKEN_BYTE_LENGTH = 32
    private val secureRandom = SecureRandom()

    fun generateToken(): String {
        val bytes = ByteArray(TOKEN_BYTE_LENGTH)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
