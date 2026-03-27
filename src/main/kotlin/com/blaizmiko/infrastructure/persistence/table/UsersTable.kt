package com.blaizmiko.infrastructure.persistence.table

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.javatime.*

object UsersTable : Table("users") {
    val id = uuid("id")
    val email = varchar("email", 255).uniqueIndex()
    val username = varchar("username", 30)
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 10).default("user")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
