package com.blaizmiko.f1backend.infrastructure.persistence.table

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.javatime.*

object RefreshTokensTable : Table("refresh_tokens") {
    val id = uuid("id")
    val userId = uuid("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE).index()
    val tokenHash = varchar("token_hash", 64).index()
    val expiresAt = timestamp("expires_at")
    val revoked = bool("revoked").default(false)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
