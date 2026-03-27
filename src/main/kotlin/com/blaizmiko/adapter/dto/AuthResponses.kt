package com.blaizmiko.adapter.dto

import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)

@Serializable
data class ProfileResponse(
    val id: String,
    val email: String,
    val username: String,
    val role: String,
    val createdAt: String,
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
)

@Serializable
data class MessageResponse(
    val message: String,
)
