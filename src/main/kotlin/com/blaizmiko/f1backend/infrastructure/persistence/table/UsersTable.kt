package com.blaizmiko.f1backend.infrastructure.persistence.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object UsersTable : Table("users") {
    val id = uuid("id")
    val email = varchar("email", ColumnLengths.EMAIL).uniqueIndex()
    val username = varchar("username", ColumnLengths.USERNAME)
    val passwordHash = varchar("password_hash", ColumnLengths.PASSWORD_HASH)
    val role = varchar("role", ColumnLengths.ROLE).default("user")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
