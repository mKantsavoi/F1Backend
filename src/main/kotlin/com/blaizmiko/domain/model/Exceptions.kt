package com.blaizmiko.domain.model

class ValidationException(override val message: String) : RuntimeException(message)
class AuthenticationException(override val message: String = "Invalid credentials") : RuntimeException(message)
class ConflictException(override val message: String) : RuntimeException(message)
