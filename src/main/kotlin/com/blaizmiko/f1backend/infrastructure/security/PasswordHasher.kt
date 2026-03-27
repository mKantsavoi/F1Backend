package com.blaizmiko.f1backend.infrastructure.security

import at.favre.lib.crypto.bcrypt.BCrypt

object PasswordHasher {
    private const val BCRYPT_COST = 12

    fun hash(password: String): String = BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray())

    fun verify(
        password: String,
        hash: String,
    ): Boolean = BCrypt.verifyer().verify(password.toCharArray(), hash).verified
}
